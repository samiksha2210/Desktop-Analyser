import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

/**
 * Tracks the currently active window and identifies the application name.
 */
public class AppTracker {

    // Interface for calling Psapi.dll functions
    public interface MyPsapi extends com.sun.jna.Library {
        MyPsapi INSTANCE = Native.load("Psapi", MyPsapi.class);
        int GetModuleBaseNameW(HANDLE hProcess, HANDLE hModule, char[] lpBaseName, int nSize);
    }

    // Get the title of the currently active window
    public static String getActiveWindowTitle() {
        try {
            char[] buffer = new char[1024];
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Unknown";
            User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
            return Native.toString(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    // Get the process (exe) name of the active window
    public static String getForegroundProcessName() {
        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Unknown";

            IntByReference pidRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
            int pid = pidRef.getValue();

            HANDLE process = Kernel32.INSTANCE.OpenProcess(
                    Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
                    false,
                    pid
            );

            if (process == null) return "Unknown";

            char[] exeName = new char[512];
            int len = MyPsapi.INSTANCE.GetModuleBaseNameW(process, null, exeName, exeName.length);
            Kernel32.INSTANCE.CloseHandle(process);

            if (len > 0) {
                return new String(exeName, 0, len);
            } else {
                return "Unknown";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    // Map process name or window title to readable app label
    public static String mapWindowToApp(String windowTitle) {
        String processName = getForegroundProcessName().toLowerCase();
        windowTitle = (windowTitle != null ? windowTitle.toLowerCase() : "");

        if (processName.contains("idea")) return "IntelliJ IDEA";
        if (processName.contains("chrome")) return "Google Chrome";
        if (processName.contains("code")) return "VS Code";
        if (processName.contains("spotify")) return "Spotify";
        if (processName.contains("steam")) return "Steam";
        if (processName.contains("firefox")) return "Mozilla Firefox";
        if (processName.contains("edge")) return "Microsoft Edge";
        if (processName.contains("brave")) return "Brave Browser";
        if (processName.contains("opera")) return "Opera Browser";

        // Fallback by title
        if (windowTitle.contains("intellij")) return "IntelliJ IDEA";
        if (windowTitle.contains("chrome")) return "Google Chrome";

        return "Other";
    }
}
