package io.github.lazorenkoo.deskback;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WindowsApps {
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int DWMWA_CLOAKED = 14;
    private static final Set<String> SYSTEM_COMPONENTS = Set.of(
            "textinputhost.exe", "searchhost.exe", "searchapp.exe", "startmenuexperiencehost.exe",
            "shellexperiencehost.exe", "lockapp.exe", "widgets.exe", "widgetservice.exe",
            "applicationframehost.exe");

    interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);
        int DwmGetWindowAttribute(HWND window, int attribute, IntByReference value, int size);
    }
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
                if (isSystemComponent(path)) {
                    continue;
                }
                String name = appName(path);
                result.add(new RunningApp(window.processId(), window.title(), name, path));
            } catch (Win32Exception exception) {
                continue;
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }
        return uniqueApps(result);
    }

    static String appName(Path executablePath) {
        return AppName.read(executablePath);
    }

    static List<RunningApp> uniqueApps(List<RunningApp> apps) {
        Map<String, RunningApp> unique = new LinkedHashMap<>();
        for (RunningApp app : apps) {
            String path = app.executablePath().toAbsolutePath().normalize().toString()
                    .toLowerCase(Locale.ROOT);
            unique.putIfAbsent(path, app);
        }
        return List.copyOf(unique.values());
    }

    static boolean isSystemComponent(Path executablePath) {
        String windowsDirectory = System.getenv("SystemRoot");
        if (windowsDirectory == null) {
            return false;
        }
        Path path = executablePath.toAbsolutePath().normalize();
        return path.startsWith(Path.of(windowsDirectory).toAbsolutePath().normalize())
                && SYSTEM_COMPONENTS.contains(path.getFileName().toString().toLowerCase(Locale.ROOT));
    }

    static long findHostedProcess(HWND window, long hostProcessId) {
        Set<Long> processes = new LinkedHashSet<>();
        User32.INSTANCE.EnumChildWindows(window, (child, data) -> {
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(child, className, className.length);
            if (!Native.toString(className).equals("Windows.UI.Core.CoreWindow")) {
                return true;
            }
            IntByReference processId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(child, processId);
            long id = Integer.toUnsignedLong(processId.getValue());
            if (id != 0 && id != hostProcessId && id != ProcessHandle.current().pid()) {
                processes.add(id);
            }
            return true;
        }, null);
        return processes.size() == 1 ? processes.iterator().next() : 0;
    }

    public static List<VisibleWindow> findVisibleWindows() {
        User32 windows = User32.INSTANCE;
        List<VisibleWindow> result = new ArrayList<>();
        boolean success = windows.EnumWindows((window, data) -> {
            if (!windows.IsWindowVisible(window)) {
                return true;
            }
            IntByReference cloaked = new IntByReference();
            int status = DwmApi.INSTANCE.DwmGetWindowAttribute(window, DWMWA_CLOAKED, cloaked, Integer.BYTES);
            if (status == 0 && cloaked.getValue() != 0) {
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
            if (type.equals("ApplicationFrameWindow")) {
                id = findHostedProcess(window, id);
            }
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
