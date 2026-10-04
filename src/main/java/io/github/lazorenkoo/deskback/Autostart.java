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
                $user = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
                $service = New-Object -ComObject Schedule.Service
                $service.Connect()
                $folder = $service.GetFolder('\\')
                $arguments = '-jar "' + $env:DESKBACK_JAR + '"'
                $workingDirectory = Split-Path -LiteralPath $env:DESKBACK_JAR
                $existing = $null
                try { $existing = $folder.GetTask('DeskBack') } catch {}
                if (!$existing -or $existing.Definition.Actions.Item(1).Path -ne $env:DESKBACK_JAVA -or
                        $existing.Definition.Actions.Item(1).Arguments -ne $arguments -or
                        $existing.Definition.Actions.Item(1).WorkingDirectory -ne $workingDirectory -or
                        !$existing.Enabled -or $existing.Definition.Triggers.Item(1).Type -ne 9) {
                    $task = $service.NewTask(0)
                    $task.RegistrationInfo.Description = 'DeskBack'
                    $task.Principal.UserId = $user
                    $task.Principal.LogonType = 3
                    $task.Principal.RunLevel = 0
                    $trigger = $task.Triggers.Create(9)
                    $trigger.UserId = $user
                    $trigger.Delay = 'PT0S'
                    $task.Settings.Enabled = $true
                    $task.Settings.DisallowStartIfOnBatteries = $false
                    $task.Settings.StopIfGoingOnBatteries = $false
                    $task.Settings.RunOnlyIfIdle = $false
                    $task.Settings.ExecutionTimeLimit = 'PT0S'
                    $task.Settings.MultipleInstances = 2
                    $action = $task.Actions.Create(0)
                    $action.Path = $env:DESKBACK_JAVA
                    $action.Arguments = $arguments
                    $action.WorkingDirectory = $workingDirectory
                    $folder.RegisterTaskDefinition('DeskBack', $task, 6, $user, $null, 3) | Out-Null
                }
                $shortcut = Join-Path ([Environment]::GetFolderPath('Startup')) 'DeskBack.lnk'
                if (Test-Path -LiteralPath $shortcut) { Remove-Item -LiteralPath $shortcut }
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
                throw new IOException("Could not register logon task: "
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
