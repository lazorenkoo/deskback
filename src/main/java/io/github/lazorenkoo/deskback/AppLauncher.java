package io.github.lazorenkoo.deskback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppLauncher {
    public record Result(int started, int skipped, List<String> failures) {}

    public static Result launch(List<SessionStore.SavedApp> apps) {
        Set<String> running = new HashSet<>();
        try (var processes = ProcessHandle.allProcesses()) {
            processes.forEach(process -> process.info().command().ifPresent(
                    command -> running.add(pathKey(Path.of(command)))));
        }
        int started = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        for (SessionStore.SavedApp app : apps) {
            Path executable = app.executablePath().toAbsolutePath().normalize();
            String key = pathKey(executable);
            if (running.contains(key)) {
                skipped++;
                continue;
            }
            if (!app.executablePath().isAbsolute() || !Files.isRegularFile(executable)
                    || !executable.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe")
                    || WindowsApps.isSystemComponent(executable)) {
                failures.add(app.name() + ": executable is unavailable or unsupported");
                continue;
            }
            try {
                Process process = new ProcessBuilder(executable.toString())
                        .directory(executable.getParent().toFile())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                process.getOutputStream().close();
                running.add(key);
                started++;
            } catch (IOException | SecurityException exception) {
                failures.add(app.name() + ": " + exception.getMessage());
            }
        }
        return new Result(started, skipped, List.copyOf(failures));
    }

    private static String pathKey(Path path) {
        return path.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
    }
}
