package io.github.lazorenkoo.deskback;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

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
        command(javaw, jar);
        String script = """
                $ErrorActionPreference = 'Stop'
                $path = Join-Path ([Environment]::GetFolderPath('Startup')) 'DeskBack.lnk'
                $shell = New-Object -ComObject WScript.Shell
                $link = $shell.CreateShortcut($path)
                $arguments = '-jar "' + $env:DESKBACK_JAR + '"'
                if (!(Test-Path -LiteralPath $path) -or $link.TargetPath -ne $env:DESKBACK_JAVA -or $link.Arguments -ne $arguments) {
                    $link.TargetPath = $env:DESKBACK_JAVA
                    $link.Arguments = $arguments
                    $link.WorkingDirectory = Split-Path -LiteralPath $env:DESKBACK_JAR
                    $link.Description = 'DeskBack'
                    $link.Save()
                }
                """;
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-WindowStyle", "Hidden", "-EncodedCommand",
                Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE)))
                .redirectErrorStream(true);
        builder.environment().put("DESKBACK_JAVA", javaw.toAbsolutePath().normalize().toString());
        builder.environment().put("DESKBACK_JAR", jar.toAbsolutePath().normalize().toString());
        Process process = builder.start();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                throw new IOException("Autostart setup timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Could not create startup shortcut: "
                        + new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Autostart setup interrupted", exception);
        } finally {
            process.destroy();
        }
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, "DeskBack")) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, "DeskBack");
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
