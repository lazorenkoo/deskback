package io.github.lazorenkoo.deskback;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class AppName {
    static String read(Path executablePath) {
        String fallback = executablePath.getFileName().toString().replaceFirst("(?i)\\.exe$", "");
        Version version = Version.INSTANCE;
        String path = executablePath.toAbsolutePath().toString();
        int size = version.GetFileVersionInfoSize(path, new IntByReference());
        if (size <= 0) {
            return fallback;
        }
        try (Memory data = new Memory(size)) {
            if (!version.GetFileVersionInfo(path, 0, size, data)) {
                return fallback;
            }
            PointerByReference value = new PointerByReference();
            IntByReference length = new IntByReference();
            Set<String> languages = new LinkedHashSet<>();
            if (version.VerQueryValue(data, "\\VarFileInfo\\Translation", value, length)
                    && value.getValue() != null) {
                for (int offset = 0; offset + 4 <= length.getValue(); offset += 4) {
                    int language = Short.toUnsignedInt(value.getValue().getShort(offset));
                    int codePage = Short.toUnsignedInt(value.getValue().getShort(offset + 2));
                    languages.add(String.format(Locale.ROOT, "%04x%04x", language, codePage));
                }
            }
            languages.add("040904b0");
            languages.add("040904e4");
            for (String field : new String[]{"ProductName", "FileDescription"}) {
                for (String language : languages) {
                    String query = "\\StringFileInfo\\" + language + "\\" + field;
                    if (version.VerQueryValue(data, query, value, length)
                            && length.getValue() > 0 && value.getValue() != null) {
                        String name = value.getValue().getWideString(0).strip();
                        if (!name.isEmpty()) {
                            return name;
                        }
                    }
                }
            }
        }
        return fallback;
    }
}
