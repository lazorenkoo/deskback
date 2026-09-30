package io.github.lazorenkoo.deskback;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WindowsApps {
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    public record VisibleWindow(long processId, String title) {}
    public record RunningApp(long processId, String windowTitle, String name, Path executablePath) {}

    public static List<RunningApp> findRunningApps() {
        List<RunningApp> result = new ArrayList<>();
        for (VisibleWindow window : findVisibleWindows()) {
            WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                    WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, (int) window.processId());
            if (process == null) {
                continue;
            }
            try {
                String executable = Kernel32Util.QueryFullProcessImageName(process, 0);
                Path path = Path.of(executable);
                String name = path.getFileName().toString().replaceFirst("(?i)\\.exe$", "");
                result.add(new RunningApp(window.processId(), window.title(), name, path));
            } catch (Win32Exception exception) {
                continue;
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }
        return List.copyOf(result);
    }

    public static List<VisibleWindow> findVisibleWindows() {
        User32 windows = User32.INSTANCE;
        List<VisibleWindow> result = new ArrayList<>();
        boolean success = windows.EnumWindows((window, data) -> {
            if (!windows.IsWindowVisible(window)) {
                return true;
            }
            int style = windows.GetWindowLong(window, WinUser.GWL_EXSTYLE);
            if ((style & WS_EX_TOOLWINDOW) != 0) {
                return true;
            }
            char[] className = new char[256];
            windows.GetClassName(window, className, className.length);
            String type = Native.toString(className);
            if (type.equals("Progman") || type.equals("WorkerW")
                    || type.equals("Shell_TrayWnd") || type.equals("Shell_SecondaryTrayWnd")) {
                return true;
            }
            int length = windows.GetWindowTextLength(window);
            if (length == 0) {
                return true;
            }
            char[] text = new char[length + 1];
            windows.GetWindowText(window, text, text.length);
            String title = Native.toString(text).trim();
            IntByReference processId = new IntByReference();
            windows.GetWindowThreadProcessId(window, processId);
            long id = Integer.toUnsignedLong(processId.getValue());
            if (!title.isEmpty() && id != 0 && id != ProcessHandle.current().pid()) {
                result.add(new VisibleWindow(id, title));
            }
            return true;
        }, null);
        if (!success) {
            throw new Win32Exception(Native.getLastError());
        }
        return List.copyOf(result);
    }
}
