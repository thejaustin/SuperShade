package com.supershade.shizuku;

import android.content.Context;
import androidx.annotation.Keep;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Keep
public class ShadeCommanderService extends IShadeCommander.Stub {

    @Keep
    public ShadeCommanderService() {
    }

    @Keep
    public ShadeCommanderService(Context context) {
    }

    @Override
    public boolean exec(String[] cmd) {
        try {
            Process process = new ProcessBuilder(cmd).start();
            try {
                return process.waitFor() == 0;
            } finally {
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
                process.destroy();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String execForOutput(String[] cmd) {
        try {
            Process process = new ProcessBuilder(cmd).start();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(line);
                }
                return sb.toString().trim();
            } finally {
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
                process.destroy();
            }
        } catch (Exception e) {
            return "";
        }
    }

    public void destroy() {
        System.exit(0);
    }
}
