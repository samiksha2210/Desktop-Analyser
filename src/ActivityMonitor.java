import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Continuously monitors which application or website is active
 * and logs time spent into the database.
 */
public class ActivityMonitor {

    private static String lastItem = "";          // Last active app name or domain
    private static boolean lastWasWebsite = false;
    private static LocalDateTime startTime;

    public static void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1️⃣ Get active process name (e.g., "chrome.exe", "idea64.exe")
                String procExe = ProcessUtils.getForegroundProcessName();
                String appName = CodingGamingDetector.getAppNameForProcess(procExe);
                int categoryForProc = CodingGamingDetector.getCategoryForProcess(procExe);

                // 2️⃣ Check if it's a browser and extract domain from title
                String currentWindowTitle = AppTracker.getActiveWindowTitle();
                String domain = null;
                boolean isBrowser = false;

                if (procExe != null) {
                    String lowerProc = procExe.toLowerCase();
                    if (lowerProc.contains("chrome") ||
                            lowerProc.contains("edge") ||
                            lowerProc.contains("firefox") ||
                            lowerProc.contains("brave") ||
                            lowerProc.contains("opera") ||
                            lowerProc.contains("chromium")) {

                        isBrowser = true;
                        domain = WebsiteTracker.extractDomainFromTitle(currentWindowTitle);
                        if (domain != null) {
                            domain = WebsiteTracker.normalizeDomain(domain);
                        }
                    }
                }

                // 3️⃣ Determine the current item (either website domain or app name)
                String currentItem;
                boolean currentIsWebsite = false;

                if (domain != null) {
                    currentItem = domain;
                    currentIsWebsite = true;
                } else {
                    currentItem = (appName != null)
                            ? appName
                            : (procExe != null ? procExe : "Unknown");
                }

                // 4️⃣ Detect change → if user switched app/tab
                if (!currentItem.equals(lastItem)) {
                    // Log the previous app or website session
                    if (!lastItem.isEmpty() && startTime != null) {
                        LocalDateTime endTime = LocalDateTime.now();
                        int durationSeconds = (int) java.time.Duration
                                .between(startTime, endTime)
                                .toSeconds(); // Cast to int safely

                        // Ignore short flickers (less than 2s)
                        if (durationSeconds >= 2) {
                            if (lastWasWebsite) {
                                // Log website activity
                                int siteId = DatabaseHelper.insertWebsiteIfNotExists(lastItem, 2); // 2 = "Distracting"
                                if (siteId != -1) {
                                    DatabaseHelper.insertActivityLog(
                                            null, siteId, startTime, endTime, durationSeconds);
                                    System.out.println("[LOGGED WEBSITE] " + lastItem +
                                            " → " + durationSeconds + "s");
                                }
                            } else {
                                // Log application activity
                                int appId = DatabaseHelper.insertApplicationIfNotExists(
                                        lastItem, (categoryForProc > 0 ? categoryForProc : 1)); // 1 = "Productive"
                                if (appId != -1) {
                                    DatabaseHelper.insertActivityLog(
                                            appId, null, startTime, endTime, durationSeconds);
                                    System.out.println("[LOGGED APP] " + lastItem +
                                            " → " + durationSeconds + "s");
                                }
                            }
                        }
                    }

                    // 5️⃣ Update for next cycle
                    lastItem = currentItem;
                    lastWasWebsite = currentIsWebsite;
                    startTime = LocalDateTime.now();
                }

            } catch (Exception ex) {
                System.err.println("[ActivityMonitor] Error: " + ex.getMessage());
                ex.printStackTrace();
            }

        }, 0, 4, TimeUnit.SECONDS); // Run every 4 seconds
    }
}
