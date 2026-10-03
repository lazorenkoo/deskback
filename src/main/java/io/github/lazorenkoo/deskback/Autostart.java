package io.github.lazorenkoo.deskback;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Autostart {
    private static final String RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    static void enable() throws IOException {
        Path jar;
        try {
            jar = Path.of(DeskBack.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException exception) {
            throw new IOException("Could not locate DeskBack", exception);
        }
        if (Files.isDirectory(jar)) {
            return;
        }
        Path javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
        String command = command(javaw, jar);
        Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, RUN_KEY);
        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, "DeskBack")
                || !command.equals(Advapi32Util.registryGetStringValue(
                        WinReg.HKEY_CURRENT_USER, RUN_KEY, "DeskBack"))) {
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, "DeskBack", command);
        }
    }

    static String command(Path javaw, Path jar) throws IOException {
        if (!Files.isRegularFile(javaw) || !Files.isRegularFile(jar)) {
            throw new IOException("Java or DeskBack JAR is missing");
        }
        return "\"" + javaw.toAbsolutePath().normalize() + "\" -jar \""
                + jar.toAbsolutePath().normalize() + "\"";
    }
}
