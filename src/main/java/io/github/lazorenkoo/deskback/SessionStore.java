package io.github.lazorenkoo.deskback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class SessionStore {
    public record SavedApp(String name, Path executablePath) {}

    private final Path file;

    public SessionStore() {
        this(Path.of(System.getenv("LOCALAPPDATA"), "DeskBack", "session.properties"));
    }

    public SessionStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    public void save(List<WindowsApps.RunningApp> apps) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", "1");
        data.setProperty("count", Integer.toString(apps.size()));
        for (int index = 0; index < apps.size(); index++) {
            WindowsApps.RunningApp app = apps.get(index);
            if (app.name().isBlank() || !app.executablePath().isAbsolute()) {
                throw new IllegalArgumentException("App must have a name and an absolute executable path");
            }
            data.setProperty("app." + index + ".name", app.name());
            data.setProperty("app." + index + ".path", app.executablePath().toString());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "session-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                data.store(writer, null);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public List<SavedApp> load() throws IOException {
        Properties data = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
        } catch (NoSuchFileException exception) {
            return List.of();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid session file", exception);
        }
        try {
            if (!"1".equals(data.getProperty("version"))) {
                throw new IllegalArgumentException("Unsupported session version");
            }
            int count = Integer.parseInt(data.getProperty("count"));
            if (count < 0 || count > data.size() / 2) {
                throw new IllegalArgumentException("Invalid app count");
            }
            List<SavedApp> apps = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String name = data.getProperty("app." + index + ".name");
                String executable = data.getProperty("app." + index + ".path");
                if (name == null || name.isBlank() || executable == null) {
                    throw new IllegalArgumentException("Incomplete app entry");
                }
                Path path = Path.of(executable);
                if (!path.isAbsolute()) {
                    throw new IllegalArgumentException("Executable path must be absolute");
                }
                apps.add(new SavedApp(name, path));
            }
            return List.copyOf(apps);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid session file", exception);
        }
    }
}
