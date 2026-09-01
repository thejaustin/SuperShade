package com.supershade.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
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
import com.supershade.ui.shade.ShadeRoot
import com.supershade.viewmodel.ShadeViewModel

/**
 * Creates and manages the full-screen [TYPE_APPLICATION_OVERLAY] Compose window
 * that renders [ShadeRoot].
 *
 * Because this object lives inside a [android.app.Service] — not an Activity —
 * it provides its own [LifecycleOwner], [ViewModelStoreOwner], and
 * [SavedStateRegistryOwner] to the hosted [ComposeView] so that all Compose
 * APIs that depend on those owners work correctly.
 *
 * API:
 *   [show]  — present the shade (no-op if already visible)
 *   [hide]  — dismiss the shade (no-op if not visible)
 *   [isShowing] — query current visibility
 */
class ShadeWindowManager(
    private val context: Context,
    private val viewModel: ShadeViewModel,
) {

    private val windowManager: WindowManager = context.getSystemService()!!
    private var overlayView: ComposeView? = null

    // A fresh ShadeLifecycleOwner is created on each show() call because
    // LifecycleRegistry cannot transition out of DESTROYED back to RESUMED.
    private var lifecycleOwner: ShadeLifecycleOwner? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Adds the shade overlay to the window stack and notifies the ViewModel. */
    fun show() {
        if (overlayView != null) return
        val owner = ShadeLifecycleOwner().also { lifecycleOwner = it }
        owner.start()
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                ShadeRoot(
                    viewModel = viewModel,
                    onDismiss = { hide() },
                )
            }
        }
        overlayView = view
        windowManager.addView(view, params)
        viewModel.open()
    }

    /** Removes the shade overlay and cleans up Compose / Lifecycle resources. */
    fun hide() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // WindowManager.BadTokenException or IllegalArgumentException —
                // the view token is already gone; nothing we can do.
            }
            overlayView = null
        }
        lifecycleOwner?.stop()
        lifecycleOwner = null
        viewModel.close()
    }

    /** Returns true when the shade overlay is currently attached to the window. */
    fun isShowing(): Boolean = overlayView != null
}

/**
 * A minimal LifecycleOwner / ViewModelStoreOwner / SavedStateRegistryOwner
 * for use with ComposeView displayed in a WindowManager overlay.
 */
private class ShadeLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreInstance = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStoreInstance

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStoreInstance.clear()
    }
}
