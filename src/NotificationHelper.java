import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javafx.util.Pair;

public class NotificationHelper {
    private static TrayIcon trayIcon;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "notif-thread");
        t.setDaemon(true);
        return t;
    });

    public static void initTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image img = Toolkit.getDefaultToolkit().createImage(new byte[0]); // replace with real icon bytes or file
            trayIcon = new TrayIcon(img, "ActivityTracker");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // schedule periodic check for continuous coding > thresholdSeconds
    public static void startBreakChecker(int thresholdSeconds, int checkIntervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Simple approach: sum today's continuous coding. Here we sum today's productive durations.
                var p = DatabaseHelper.queryProductiveVsTotalToday();
                int productive = p.getKey();
                if (productive >= thresholdSeconds) {
                    showNotification("Take a break", "You've been productive for " + (productive/60) + " minutes.");
                }
            } catch (Throwable t) { t.printStackTrace(); }
        }, 10, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    // Periodic checker for blocked apps (call startBlockedAppChecker(30) in main app)
    public static void startBlockedAppChecker(int checkIntervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<String> runningApps = getRunningApps();
                for (String app : runningApps) {
                    if (DatabaseHelper.isAppBlockedByName(app)) {
                        showNotification("Blocked App Detected", "Close '" + normalizeAppName(app) + "' to stay in Focus Mode.");
                        break; // Avoid spam
                    }
                }
            } catch (Throwable t) { t.printStackTrace(); }
        }, 10, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    // NEW: Schedule a one-time delayed notification (e.g., 10s after Focus Mode enable)
    public static void scheduleDelayedNotification(String title, String message, int delaySeconds) {
        scheduler.schedule(() -> {
            try {
                showNotification(title, message);
            } catch (Throwable t) { t.printStackTrace(); }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private static List<String> getRunningApps() {
        return ProcessHandle.allProcesses()
                .map(ph -> ph.info().command().orElse(""))
                .filter(cmd -> !cmd.isEmpty())
                .map(NotificationHelper::normalizeAppName)
                .collect(Collectors.toList());
    }

    // NEW: Helper for normalization (mirror from DB for consistency)
    private static String normalizeAppName(String rawName) {
        if (rawName == null) return "";
        String name = rawName;
        if (name.contains("\\") || name.contains("/")) {
            name = new java.io.File(name).getName();
        }
        if (name.toLowerCase().endsWith(".exe")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.trim();
    }

    public static void showNotification(String title, String message) {
        System.out.println("[NOTIF DEBUG] Showing: " + title + " - " + message); // Always console
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        } else {
            System.out.println("[Notification] " + title + " - " + message);
        }
    }

    public static void stop() {
        scheduler.shutdownNow();
    }
}