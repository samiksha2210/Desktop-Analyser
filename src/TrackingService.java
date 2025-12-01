import java.time.LocalDateTime;
import java.util.concurrent.*;
import javafx.util.Pair;

public class TrackingService {
    // -------------------- CONFIGURABLE SETTINGS --------------------
    private static final int POLL_INTERVAL_SECONDS = 4; // Check every X seconds
    private static final int MIN_LOG_SECONDS = 2; // Ignore durations shorter than this
    private static final int DEBOUNCE_COUNT = 2; // Require N stable readings to accept switch
    // -------------------- BACKGROUND SCHEDULER --------------------
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tracking-thread");
        t.setDaemon(true);
        return t;
    });
    // -------------------- STATE VARIABLES --------------------
    private String lastItem = "";
    private boolean lastWasWebsite = false;
    private LocalDateTime startTime = null;
    private String pendingItem = null;
    private int pendingCount = 0;
    private LocalDateTime lastNotificationTime = null;  // ADD: Cooldown tracker
    // -------------------- PUBLIC METHODS --------------------
    public void start() {
        // Ensure database ready
        DatabaseHelper.enableWALMode();
        DatabaseHelper.createTables();
        scheduler.scheduleAtFixedRate(this::pollOnce, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("TrackingService started (poll every " + POLL_INTERVAL_SECONDS + "s)");
    }
    public void stop() {
        System.out.println("Stopping TrackingService...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS))
                scheduler.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        flushCurrent();
        System.out.println("TrackingService stopped.");
    }
    // -------------------- CORE POLLING LOGIC --------------------
    private void pollOnce() {
        try {
            // 1) Detect active window and map to app name
            String windowTitle = AppTracker.getActiveWindowTitle();
            String appName = AppTracker.mapWindowToApp(windowTitle);
            // 2) If browser, try to extract domain name
            String domain = null;
            if (isBrowser(appName)) {
                domain = WebsiteTracker.extractDomainFromTitle(windowTitle);
                if (domain != null)
                    domain = WebsiteTracker.normalizeDomain(domain);
            }
            String currentItem = (domain != null) ? domain : appName;
            boolean currentIsWebsite = (domain != null);
            System.out.println("[TRACK DEBUG] Polling: appName='" + appName + "', domain='" + domain + "', currentItem='" + currentItem + "' (website? " + currentIsWebsite + ")");
            // 3) Debounce logic to avoid flickers
            if (!currentItem.equals(pendingItem)) {
                pendingItem = currentItem;
                pendingCount = 1;
            } else {
                pendingCount++;
            }
            if (pendingCount < DEBOUNCE_COUNT) {
                // Still unstable — ignore this cycle
                return;
            }
            if (DatabaseHelper.isFocusModeEnabled()) {
                System.out.println("[TRACK DEBUG] Focus mode ON - checking block for '" + currentItem + "'");
                boolean isBlocked = currentIsWebsite
                        ? DatabaseHelper.isSiteBlockedByUrl(currentItem)
                        : DatabaseHelper.isAppBlockedByName(currentItem);
                if (isBlocked) {
                    System.out.println("[TRACK DEBUG] BLOCKED DETECTED - showing notification for '" + currentItem + "'");
                    if (lastNotificationTime == null || java.time.Duration.between(lastNotificationTime, LocalDateTime.now()).getSeconds() > 30) {  // 30s cooldown
                        NotificationHelper.showNotification("Focus Mode Active",
                                "Avoid " + currentItem + " - Stay focused and try to reduce distractions!");
                        lastNotificationTime = LocalDateTime.now();
                    } else {
                        System.out.println("[TRACK DEBUG] Blocked, but cooldown active - skipping notif");
                    }
                } else {
                    System.out.println("[TRACK DEBUG] Not blocked");
                }
            } else {
                System.out.println("[TRACK DEBUG] Focus mode OFF - skipping check");
            }
            // 4) If item switched → log previous
            if (!currentItem.equals(lastItem)) {
                if (!lastItem.isEmpty() && startTime != null) {
                    LocalDateTime endTime = LocalDateTime.now();
                    int duration = (int) java.time.Duration.between(startTime, endTime).toSeconds();
                    if (duration >= MIN_LOG_SECONDS) {
                        if (lastWasWebsite) {
                            int siteId = DatabaseHelper.insertWebsiteIfNotExists(lastItem, 2); // Default: Distracting
                            if (siteId != -1) {
                                DatabaseHelper.insertActivityLog(null, siteId, startTime, endTime, duration);
                                System.out.println("[LOGGED WEBSITE] " + lastItem + " → " + duration + "s");
                            }
                        } else {
                            int appId = DatabaseHelper.insertApplicationIfNotExists(lastItem, 1); // Default: Productive
                            if (appId != -1) {
                                DatabaseHelper.insertActivityLog(appId, null, startTime, endTime, duration);
                                System.out.println("[LOGGED APP] " + lastItem + " → " + duration + "s");
                            }
                        }
                    }
                }
                // Update tracking state
                lastItem = currentItem;
                lastWasWebsite = currentIsWebsite;
                startTime = LocalDateTime.now();
                lastNotificationTime = null;  // ADD: Reset cooldown on new item
                // Reset debounce
                pendingItem = null;
                pendingCount = 0;
            }
        } catch (Throwable t) {
            // Never allow the scheduler thread to die
            System.err.println("[TrackingService] Error in pollOnce:");
            t.printStackTrace();
        }
    }
    // -------------------- FLUSH LAST ITEM ON STOP --------------------
    private void flushCurrent() {
        if (lastItem != null && !lastItem.isEmpty() && startTime != null) {
            LocalDateTime endTime = LocalDateTime.now();
            int duration = (int) java.time.Duration.between(startTime, endTime).getSeconds();
            if (duration >= MIN_LOG_SECONDS) {
                if (lastWasWebsite) {
                    int siteId = DatabaseHelper.insertWebsiteIfNotExists(lastItem, 2);
                    if (siteId != -1)
                        DatabaseHelper.insertActivityLog(null, siteId, startTime, endTime, duration);
                } else {
                    int appId = DatabaseHelper.insertApplicationIfNotExists(lastItem, 1);
                    if (appId != -1)
                        DatabaseHelper.insertActivityLog(appId, null, startTime, endTime, duration);
                }
            }
        }
    }
    // -------------------- HELPER: Detect Browsers --------------------
    private boolean isBrowser(String appName) {
        if (appName == null) return false;
        String name = appName.toLowerCase();
        return name.contains("chrome") || name.contains("edge") ||
                name.contains("firefox") || name.contains("brave") ||
                name.contains("opera") || name.contains("chromium");
    }
}