import com.sun.jna.Pointer;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

// --- Custom Psapi binding for wide-char version ---
interface MyPsapi extends com.sun.jna.Library {
    MyPsapi INSTANCE = Native.load("Psapi", MyPsapi.class);
    int GetModuleBaseNameW(HANDLE hProcess, HANDLE hModule, char[] lpBaseName, int nSize);
}

public class ProcessUtils {

    /**
     * Returns the executable name (e.g., "idea64.exe", "chrome.exe", "spotify.exe")
     * of the process that owns the current foreground window. Returns null on failure.
     */
    public static String getForegroundProcessName() {
        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return null;

            // Get process ID
            IntByReference pidRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
            int pid = pidRef.getValue();
            if (pid == 0) return null;

            // Open process for reading info
            HANDLE process = Kernel32.INSTANCE.OpenProcess(
                    Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
                    false,
                    pid
            );
            if (process == null) return null;

            // Read module base name (exe name)
            char[] buffer = new char[1024];
            int len = MyPsapi.INSTANCE.GetModuleBaseNameW(process, null, buffer, buffer.length);
            Kernel32.INSTANCE.CloseHandle(process);

            if (len > 0) {
                return new String(buffer, 0, len).toLowerCase();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
