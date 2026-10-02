package io.github.lazorenkoo.deskback;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class SessionRecorder {
    private final SessionStore sessions;
    private final Supplier<List<WindowsApps.RunningApp>> runningApps;
    private List<SessionStore.SavedApp> previous;

    SessionRecorder(SessionStore sessions, Supplier<List<WindowsApps.RunningApp>> runningApps) {
        this.sessions = sessions;
        this.runningApps = runningApps;
    }

    void start(int intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("Update interval must be positive");
        }
        var scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "deskback-session-recorder");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                update();
            } catch (IOException | RuntimeException exception) {
                System.err.println("Could not save session: " + exception.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    void update() throws IOException {
        if (previous == null) {
            try {
                previous = sorted(sessions.load());
            } catch (IOException exception) {
                previous = null;
            }
        }
        List<WindowsApps.RunningApp> apps = runningApps.get();
        List<SessionStore.SavedApp> current = sorted(apps.stream()
                .map(app -> new SessionStore.SavedApp(app.name(), app.executablePath()))
                .toList());
        if (!current.equals(previous)) {
            sessions.save(apps);
            previous = current;
        }
    }

    private static List<SessionStore.SavedApp> sorted(List<SessionStore.SavedApp> apps) {
        return apps.stream()
                .map(app -> new SessionStore.SavedApp(app.name(), app.executablePath().toAbsolutePath().normalize()))
                .sorted(Comparator.comparing(app -> app.executablePath().toString().toLowerCase(Locale.ROOT)))
                .toList();
    }
}
