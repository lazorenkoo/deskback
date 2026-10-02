package io.github.lazorenkoo.deskback;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import java.util.concurrent.CompletableFuture;

final class SessionEndListener {
    private static final int WM_QUERYENDSESSION = 0x0011;
    private static final int WM_ENDSESSION = 0x0016;

    interface ShutdownApi extends StdCallLibrary {
        ShutdownApi INSTANCE = Native.load("kernel32", ShutdownApi.class);
        boolean SetProcessShutdownParameters(int level, int flags);
    }

    private final SessionRecorder recorder;
    private final WinUser.WindowProc procedure = this::onMessage;

    private SessionEndListener(SessionRecorder recorder) {
        this.recorder = recorder;
    }

    static void start(SessionRecorder recorder) {
        if (!ShutdownApi.INSTANCE.SetProcessShutdownParameters(0x3ff, 0)) {
            throw new Win32Exception(Native.getLastError());
        }
        SessionEndListener listener = new SessionEndListener(recorder);
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Thread thread = new Thread(() -> listener.listen(ready), "deskback-session-end");
        thread.setDaemon(true);
        thread.start();
        ready.join();
    }

    private void listen(CompletableFuture<Void> ready) {
        User32 windows = User32.INSTANCE;
        HINSTANCE instance = Kernel32.INSTANCE.GetModuleHandle(null);
        String className = "DeskBackSessionEnd-" + ProcessHandle.current().pid();
        HWND window = null;
        boolean registered = false;
        try {
            WinUser.WNDCLASSEX windowClass = new WinUser.WNDCLASSEX();
            windowClass.hInstance = instance;
            windowClass.lpszClassName = className;
            windowClass.lpfnWndProc = procedure;
            var atom = windows.RegisterClassEx(windowClass);
            if (atom == null || atom.intValue() == 0) {
                throw new Win32Exception(Native.getLastError());
            }
            registered = true;
            window = windows.CreateWindowEx(0, className, "DeskBack Session Listener", 0,
                    0, 0, 0, 0, null, null, instance, null);
            if (window == null) {
                throw new Win32Exception(Native.getLastError());
            }
            ready.complete(null);
            WinUser.MSG message = new WinUser.MSG();
            int result;
            while ((result = windows.GetMessage(message, null, 0, 0)) > 0) {
                windows.TranslateMessage(message);
                windows.DispatchMessage(message);
            }
            if (result == -1) {
                throw new Win32Exception(Native.getLastError());
            }
        } catch (RuntimeException exception) {
            if (!ready.completeExceptionally(exception)) {
                System.err.println("Could not monitor session ending: " + exception.getMessage());
            }
        } finally {
            recorder.pause();
            if (window != null && windows.IsWindow(window)) {
                windows.DestroyWindow(window);
            }
            if (registered) {
                windows.UnregisterClass(className, instance);
            }
        }
    }

    private LRESULT onMessage(HWND window, int message, WPARAM wParam, LPARAM lParam) {
        if (message == WM_QUERYENDSESSION) {
            recorder.pause();
            return new LRESULT(1);
        }
        if (message == WM_ENDSESSION) {
            if (wParam.longValue() == 0) {
                recorder.resume();
            } else {
                recorder.pause();
            }
            return new LRESULT(0);
        }
        if (message == WinUser.WM_DESTROY) {
            User32.INSTANCE.PostQuitMessage(0);
            return new LRESULT(0);
        }
        return User32.INSTANCE.DefWindowProc(window, message, wParam, lParam);
    }
}
