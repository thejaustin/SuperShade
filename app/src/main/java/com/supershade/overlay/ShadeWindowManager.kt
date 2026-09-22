package com.supershade.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.KeyEvent
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.supershade.ui.theme.BackdropTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.supershade.ui.shade.ShadeRoot
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creates and manages the full-screen [TYPE_APPLICATION_OVERLAY] Compose window
 * that renders [ShadeRoot].
 *
 * Because this object lives inside a [android.app.Service] — not an Activity —
 * it provides its own [LifecycleOwner], [ViewModelStoreOwner], [SavedStateRegistryOwner],
 * and [OnBackPressedDispatcherOwner] to the hosted [ComposeView] so that all Compose
 * APIs (including [BackHandler]) and hardware/gesture back keys work seamlessly.
 *
 * API:
 *   [show]  — present the shade (no-op if already visible)
 *   [hide]  — dismiss the shade (no-op if not visible)
 *   [isShowing] — query current visibility
 */
class ShadeWindowManager(
    private val context: Context,
    private val viewModel: ShadeViewModel,
    private val governor: com.supershade.shizuku.StatusBarGovernor? = null,
    private val haptics: com.supershade.haptics.SuperHaptics? = null,
) {

    private val windowManager: WindowManager = context.getSystemService()!!
    private var overlayView: ComposeView? = null

    // A fresh ShadeLifecycleOwner is created on each show() call because
    // LifecycleRegistry cannot transition out of DESTROYED back to RESUMED.
    private var lifecycleOwner: ShadeLifecycleOwner? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var hideJob: Job? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        // Android 12+ window compositor blur — rich frosted glass blur behind the overlay.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            val density = context.resources.displayMetrics.density
            blurBehindRadius = (28 * density).toInt().coerceIn(75, 115)
        }
    }

    init {
        scope.launch {
            viewModel.state
                .map { it.backdropTheme to it.backdropOpacity }
                .distinctUntilChanged()
                .collect { (backdrop, opacity) ->
                    applyBackdropTheme(backdrop, opacity)
                }
        }
    }

    private fun applyBackdropTheme(backdrop: BackdropTheme, opacity: Float = 0.78f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val density = context.resources.displayMetrics.density
            if (opacity >= 0.99f || backdrop == BackdropTheme.OPAQUE) {
                @Suppress("DEPRECATION")
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                params.blurBehindRadius = 0
            } else {
                @Suppress("DEPRECATION")
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                val targetRadius = when (backdrop) {
                    BackdropTheme.TRANSPARENT -> (16 * density).toInt().coerceIn(35, 60)
                    BackdropTheme.LIQUID_GLASS -> (24 * density).toInt().coerceIn(60, 95)
                    BackdropTheme.FROSTED_GLASS -> (28 * density).toInt().coerceIn(75, 115)
                    BackdropTheme.BLURRY -> (45 * density).toInt().coerceIn(120, 160)
                    else -> ((15f + (opacity * 34f)) * density).toInt().coerceIn(35, 160)
                }
                params.blurBehindRadius = targetRadius
            }
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Adds the shade overlay to the window stack and notifies the ViewModel. */
    fun show(expandQs: Boolean = false) {
        hideJob?.cancel()
        hideJob = null
        if (overlayView != null) return
        val owner = ShadeLifecycleOwner().also { lifecycleOwner = it }
        owner.start()
        val s = viewModel.state.value
        applyBackdropTheme(s.backdropTheme, s.backdropOpacity)

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeOnBackPressedDispatcherOwner(owner)
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (owner.onBackPressedDispatcher.hasEnabledCallbacks()) {
                        owner.onBackPressedDispatcher.onBackPressed()
                    } else {
                        hide()
                    }
                    true
                } else {
                    false
                }
            }
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.supershade.haptics.LocalSuperHaptics provides (haptics ?: com.supershade.haptics.SuperHaptics(context))
                ) {
                    ShadeRoot(
                        viewModel = viewModel,
                        onDismiss = { hide() },
                    )
                }
            }
        }
        overlayView = view
        windowManager.addView(view, params)
        // Dispatch window insets to the ComposeView so statusBarsPadding() and
        // similar modifiers resolve to the correct values in an overlay window.
        view.requestApplyInsets()
        if (!viewModel.state.value.isOpen) {
            viewModel.open(expandQs = expandQs)
        }
        scope.launch { governor?.collapse() }
    }

    /** Removes the shade overlay and cleans up Compose / Lifecycle resources after exit animation. */
    fun hide() {
        if (overlayView == null) return
        if (hideJob?.isActive == true) return
        viewModel.close()
        val viewToRemove = overlayView
        val ownerToStop = lifecycleOwner
        hideJob = scope.launch {
            delay(260L)
            if (viewToRemove != null) {
                try {
                    windowManager.removeView(viewToRemove)
                } catch (_: Exception) {}
            }
            if (overlayView === viewToRemove) {
                overlayView = null
                lifecycleOwner = null
            }
            ownerToStop?.stop()
            hideJob = null
        }
    }

    /** Returns true when the shade overlay is currently attached to the window. */
    fun isShowing(): Boolean = overlayView != null
}

/**
 * A LifecycleOwner / ViewModelStoreOwner / SavedStateRegistryOwner / OnBackPressedDispatcherOwner
 * for use with ComposeView displayed in a WindowManager overlay.
 */
private class ShadeLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreInstance = ViewModelStore()
    private val onBackPressedDispatcherInstance = OnBackPressedDispatcher()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStoreInstance
    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = onBackPressedDispatcherInstance

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStoreInstance.clear()
    }
}
