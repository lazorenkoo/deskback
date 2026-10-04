package io.github.lazorenkoo.deskback;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import java.io.IOException;

final class SingleInstance implements AutoCloseable {
    private final HANDLE mutex;
    private boolean closed;

    private SingleInstance(HANDLE mutex) {
        this.mutex = mutex;
    }

    static SingleInstance acquire() throws IOException {
        HANDLE mutex = Kernel32.INSTANCE.CreateMutex(null, false, "Local\\DeskBack");
        int error = Kernel32.INSTANCE.GetLastError();
        if (mutex == null) {
            throw new IOException("Could not create instance mutex: " + error);
        }
        if (error == WinError.ERROR_ALREADY_EXISTS) {
            Kernel32.INSTANCE.CloseHandle(mutex);
            return null;
        }
        return new SingleInstance(mutex);
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            if (!Kernel32.INSTANCE.CloseHandle(mutex)) {
                throw new IOException("Could not close instance mutex: " + Kernel32.INSTANCE.GetLastError());
            }
            closed = true;
        }
    }
}
