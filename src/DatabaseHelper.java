import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import javafx.util.Pair;
import java.io.File;  // NEW: For path normalization

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:activity_tracker.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void enableWALMode() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Categories (
                    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Applications (
                    app_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    category_id INTEGER,
                    FOREIGN KEY(category_id) REFERENCES Categories(category_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Websites (
                    site_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    url TEXT UNIQUE NOT NULL,
                    category_id INTEGER,
                    FOREIGN KEY(category_id) REFERENCES Categories(category_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Activity_Log (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app_id INTEGER,
                    site_id INTEGER,
                    start_time TEXT,
                    end_time TEXT,
                    duration_seconds INTEGER,
                    FOREIGN KEY(app_id) REFERENCES Applications(app_id),
                    FOREIGN KEY(site_id) REFERENCES Websites(site_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Insights (
                    date TEXT PRIMARY KEY,
                    total_seconds INTEGER,
                    productive_seconds INTEGER,
                    score INTEGER,
                    notes TEXT
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS BlockedApps (
                    app_id INTEGER PRIMARY KEY,
                    FOREIGN KEY(app_id) REFERENCES Applications(app_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS BlockedWebsites (
                    site_id INTEGER PRIMARY KEY,
                    FOREIGN KEY(site_id) REFERENCES Websites(site_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- INSERT METHODS (with normalization) -------------------
    public static void insertCategory(String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO Categories(name) VALUES(?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertApplication(String rawName, int categoryId) {
        // NEW: Normalize name - strip path and .exe
        String name = normalizeAppName(rawName);
        System.out.println("[DB DEBUG] Inserting normalized app name: '" + name + "' from '" + rawName + "'");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO Applications(name, category_id) VALUES(?, ?)")) {
            ps.setString(1, name);
            ps.setInt(2, categoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertWebsite(String rawUrl, int categoryId) {
        // NEW: Normalize URL - strip protocol/query if needed, but keep as-is for now (focus on apps)
        String url = rawUrl.startsWith("http") ? rawUrl : "http://" + rawUrl;  // Basic normalization
        System.out.println("[DB DEBUG] Inserting normalized website URL: '" + url + "' from '" + rawUrl + "'");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO Websites(url, category_id) VALUES(?, ?)")) {
            ps.setString(1, url);
            ps.setInt(2, categoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int insertApplicationIfNotExists(String rawName, int categoryId) {
        insertApplication(rawName, categoryId);
        String name = normalizeAppName(rawName);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT app_id FROM Applications WHERE name=?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("app_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int insertWebsiteIfNotExists(String rawUrl, int categoryId) {
        insertWebsite(rawUrl, categoryId);
        String url = rawUrl.startsWith("http") ? rawUrl : "http://" + rawUrl;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT site_id FROM Websites WHERE url=?")) {
            ps.setString(1, url);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("site_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // NEW: Helper to normalize app names (strip path and .exe)
    private static String normalizeAppName(String rawName) {
        if (rawName == null) return "";
        String name = rawName;
        if (name.contains("\\") || name.contains("/")) {
            name = new File(name).getName();
        }
        if (name.toLowerCase().endsWith(".exe")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.trim();
    }
    // NEW: Helper to normalize URLs (strip protocol/path for display and matching)
    private static String normalizeUrlForDisplay(String rawUrl) {
        if (rawUrl == null) return "";
        // Strip protocol, path, query; keep domain (e.g., 'https://www.example.com/path?foo' -> 'www.example.com')
        String normalized = rawUrl.replaceAll("^(https?://)?", "").replaceAll("/.*", "");
        return normalized.trim();
    }

    // ------------------- GET METHODS -------------------
    public static List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Categories")) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllApplications() {
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Applications")) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllWebsites() {
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT url FROM Websites")) {
            while (rs.next()) list.add(rs.getString("url"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ------------------- UPDATE CATEGORY METHODS -------------------
    public static void updateApplicationCategory(int appId, Integer categoryId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE Applications SET category_id=? WHERE app_id=?")) {
            if (categoryId != null) ps.setInt(1, categoryId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, appId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateWebsiteCategory(int siteId, Integer categoryId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE Websites SET category_id=? WHERE site_id=?")) {
            if (categoryId != null) ps.setInt(1, categoryId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, siteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------- ACTIVITY LOG METHODS -------------------
    public static void insertActivityLog(Integer appId, Integer siteId,
                                         LocalDateTime start, LocalDateTime end, int duration) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO Activity_Log(app_id, site_id, start_time, end_time, duration_seconds) VALUES(?,?,?,?,?)")) {
            if (appId != null) ps.setInt(1, appId); else ps.setNull(1, Types.INTEGER);
            if (siteId != null) ps.setInt(2, siteId); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, start.toString());
            ps.setString(4, end.toString());
            ps.setInt(5, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Pair<Integer,Integer> queryProductiveVsTotalToday() {
        int productive = 0, total = 0;
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT AL.duration_seconds, C.name AS cat " +
                             "FROM Activity_Log AL " +
                             "LEFT JOIN Applications A ON AL.app_id = A.app_id " +
                             "LEFT JOIN Websites W ON AL.site_id = W.site_id " +
                             "LEFT JOIN Categories C ON C.category_id = " +
                             "COALESCE(A.category_id, W.category_id) " +
                             "WHERE date(AL.start_time)=?")) {
            ps.setString(1, today.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int dur = rs.getInt("duration_seconds");
                String cat = rs.getString("cat");
                total += dur;
                if ("Productive".equalsIgnoreCase(cat)) productive += dur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Pair<>(productive, total);
    }

    public static void insertInsight(LocalDate date, int total, int productive, int score, String notes) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO Insights(date, total_seconds, productive_seconds, score, notes) VALUES(?,?,?,?,?)")) {
            ps.setString(1, date.toString());
            ps.setInt(2, total);
            ps.setInt(3, productive);
            ps.setInt(4, score);
            ps.setString(5, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Map.Entry<Integer, Pair<String,Integer>>> querySitesWithTodaySeconds() {
        List<Map.Entry<Integer, Pair<String,Integer>>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT site_id, url FROM Websites")) {
            while (rs.next()) {
                int siteId = rs.getInt("site_id");
                String url = rs.getString("url");
                int seconds = 0;
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT SUM(duration_seconds) AS total FROM Activity_Log " +
                                "WHERE site_id=? AND date(start_time)=?")) {
                    ps2.setInt(1, siteId);
                    ps2.setString(2, today.toString());
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) seconds = rs2.getInt("total");
                }
                result.add(new SimpleEntry<>(siteId, new Pair<>(url, seconds)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ------------------- NEW METHODS FOR DASHBOARD & CODING/GAMING -------------------
    public static List<Pair<String,Integer>> queryDailyCategoryTotals(int categoryId, int days) {
        List<Pair<String,Integer>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection()) {
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(AL.duration_seconds) AS total " +
                                "FROM Activity_Log AL " +
                                "LEFT JOIN Applications A ON AL.app_id = A.app_id " +
                                "LEFT JOIN Websites W ON AL.site_id = W.site_id " +
                                "WHERE date(AL.start_time)=? AND " +
                                "COALESCE(A.category_id, W.category_id)=?")) {
                    ps.setString(1, date.toString());
                    ps.setInt(2, categoryId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) total = rs.getInt("total");
                }
                result.add(new Pair<>(date.toString(), total));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<Pair<String,Integer>> queryAppTotalsToday() {
        List<Pair<String,Integer>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT app_id, name FROM Applications")) {
            while (rs.next()) {
                int appId = rs.getInt("app_id");
                String name = rs.getString("name");
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(duration_seconds) AS total FROM Activity_Log " +
                                "WHERE app_id=? AND date(start_time)=?")) {
                    ps.setInt(1, appId);
                    ps.setString(2, today.toString());
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next()) total = rs2.getInt("total");
                }
                result.add(new Pair<>(name, total));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<Pair<String,Integer>> querySiteTotalsToday() {
        List<Pair<String,Integer>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT site_id, url FROM Websites")) {
            while (rs.next()) {
                int siteId = rs.getInt("site_id");
                String url = rs.getString("url");
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(duration_seconds) AS total FROM Activity_Log " +
                                "WHERE site_id=? AND date(start_time)=?")) {
                    ps.setInt(1, siteId);
                    ps.setString(2, today.toString());
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next()) total = rs2.getInt("total");
                }
                result.add(new Pair<>(url, total));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ------------------- NEW METHOD FOR AppsPage -------------------
    public static List<Map.Entry<Integer, Pair<String,Integer>>> queryAppsWithTodaySeconds() {
        List<Map.Entry<Integer, Pair<String,Integer>>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT app_id, name FROM Applications")) {
            while (rs.next()) {
                int appId = rs.getInt("app_id");
                String name = rs.getString("name");
                int seconds = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(duration_seconds) AS total FROM Activity_Log " +
                                "WHERE app_id=? AND date(start_time)=?")) {
                    ps.setInt(1, appId);
                    ps.setString(2, today.toString());
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next()) seconds = rs2.getInt("total");
                }
                result.add(new SimpleEntry<>(appId, new Pair<>(name, seconds)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ------------------- BLOCKING METHODS WITH TRANSACTIONS -------------------
    public static void blockApp(int appId) {
        System.out.println("[DB DEBUG] Blocking app_id=" + appId);
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO BlockedApps(app_id) VALUES(?)")) {
                ps.setInt(1, appId);
                int rows = ps.executeUpdate();
                conn.commit();
                System.out.println("[DB DEBUG] Block app affected " + rows + " rows (1=inserted, 0=already exists)");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("[DB DEBUG] BlockApp failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void unblockApp(int appId) {
        System.out.println("[DB DEBUG] Unblocking app_id=" + appId);
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BlockedApps WHERE app_id=?")) {
                ps.setInt(1, appId);
                int rows = ps.executeUpdate();
                conn.commit();
                System.out.println("[DB DEBUG] Unblock app affected " + rows + " rows");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("[DB DEBUG] UnblockApp failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void blockWebsite(int siteId) {
        System.out.println("[DB DEBUG] Blocking site_id=" + siteId);
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO BlockedWebsites(site_id) VALUES(?)")) {
                ps.setInt(1, siteId);
                int rows = ps.executeUpdate();
                conn.commit();
                System.out.println("[DB DEBUG] Block website affected " + rows + " rows (1=inserted, 0=already exists)");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("[DB DEBUG] BlockWebsite failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void unblockWebsite(int siteId) {
        System.out.println("[DB DEBUG] Unblocking site_id=" + siteId);
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BlockedWebsites WHERE site_id=?")) {
                ps.setInt(1, siteId);
                int rows = ps.executeUpdate();
                conn.commit();
                System.out.println("[DB DEBUG] Unblock website affected " + rows + " rows");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("[DB DEBUG] UnblockWebsite failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isAppBlocked(int appId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM BlockedApps WHERE app_id=?")) {
            ps.setInt(1, appId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean isSiteBlocked(int siteId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM BlockedWebsites WHERE site_id=?")) {
            ps.setInt(1, siteId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean isAppBlockedByName(String appName) {
        if (appName == null) return false;
        // NEW: Normalize name before query
        String normalized = normalizeAppName(appName);
        System.out.println("[DB DEBUG] Checking block for normalized appName='" + normalized + "' (from '" + appName + "')");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM BlockedApps BA JOIN Applications A ON BA.app_id = A.app_id WHERE A.name = ?")) {
            ps.setString(1, normalized);
            ResultSet rs = ps.executeQuery();
            boolean blocked = rs.next();
            System.out.println("[DB DEBUG] App '" + normalized + "' blocked? " + blocked);
            return blocked;
        } catch (SQLException e) {
            System.err.println("[DB DEBUG] isAppBlockedByName failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean isSiteBlockedByUrl(String url) {
        if (url == null) return false;
        String normalized = normalizeUrlForDisplay(url);
        System.out.println("[DB DEBUG] Checking block for normalized url='" + normalized + "' (from '" + url + "')");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM BlockedWebsites BW JOIN Websites W ON BW.site_id = W.site_id WHERE W.url LIKE ?")) {
            ps.setString(1, "%" + normalized + "%"); // Fuzzy match on domain
            ResultSet rs = ps.executeQuery();
            boolean blocked = rs.next();
            System.out.println("[DB DEBUG] Site '" + normalized + "' blocked? " + blocked);
            return blocked;
        } catch (SQLException e) {
            System.err.println("[DB DEBUG] isSiteBlockedByUrl failed: " + e.getMessage());
            return false;
        }
    }

    // NEW: Trigger notification if blocked app launched (call from monitoring service)
    public static void notifyIfBlockedAppLaunched(String appName) {
        if (isAppBlockedByName(appName)) {
            NotificationHelper.showNotification("Focus Mode Alert", "Blocked app '" + normalizeAppName(appName) + "' detected. Stay focused!");
            System.out.println("[DB DEBUG] Notification sent for blocked app: " + appName);
        }
    }
    // NEW: Trigger notification if blocked site visited (call from monitoring service)
    public static void notifyIfBlockedSiteVisited(String url) {
        if (isSiteBlockedByUrl(url)) {
            String normalizedUrl = normalizeUrlForDisplay(url);
            NotificationHelper.showNotification("Focus Mode Alert", "Blocked website '" + normalizedUrl + "' accessed. Stay focused!");
            System.out.println("[DB DEBUG] Notification sent for blocked site: " + url);
        }
    }

    // NEW: Debug method to print blocked apps (call for testing)
    public static void debugBlockedApps() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT BA.app_id, A.name FROM BlockedApps BA JOIN Applications A ON BA.app_id = A.app_id")) {
            System.out.println("[DB DEBUG] Blocked Apps:");
            while (rs.next()) {
                System.out.println("  ID: " + rs.getInt("app_id") + ", Name: " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setFocusModeEnabled(boolean enabled) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO Settings(key, value) VALUES(?, ?)")) {
            ps.setString(1, "focus_mode_enabled");
            ps.setString(2, enabled ? "true" : "false");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isFocusModeEnabled() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT value FROM Settings WHERE key = ?")) {
            ps.setString(1, "focus_mode_enabled");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "true".equals(rs.getString("value"));
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<Map.Entry<Integer, Pair<String, Boolean>>> getAppsWithBlockedStatus() {
        List<Map.Entry<Integer, Pair<String, Boolean>>> result = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT app_id, name FROM Applications")) {
            while (rs.next()) {
                int appId = rs.getInt("app_id");
                String name = rs.getString("name");
                // Validation: Check if app_id is valid (redundant but logs issues)
                try (PreparedStatement validatePs = conn.prepareStatement("SELECT 1 FROM Applications WHERE app_id=?")) {
                    validatePs.setInt(1, appId);
                    if (!validatePs.executeQuery().next()) {
                        System.err.println("[DB DEBUG] Invalid app_id=" + appId + " in getAppsWithBlockedStatus!");
                        continue;
                    }
                }
                boolean blocked = isAppBlocked(appId);
                result.add(new SimpleEntry<>(appId, new Pair<>(name, blocked)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<Map.Entry<Integer, Pair<String, Boolean>>> getWebsitesWithBlockedStatus() {
        List<Map.Entry<Integer, Pair<String, Boolean>>> result = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT site_id, url FROM Websites")) {
            while (rs.next()) {
                int siteId = rs.getInt("site_id");
                String url = rs.getString("url");
                // Validation: Check if site_id is valid
                try (PreparedStatement validatePs = conn.prepareStatement("SELECT 1 FROM Websites WHERE site_id=?")) {
                    validatePs.setInt(1, siteId);
                    if (!validatePs.executeQuery().next()) {
                        System.err.println("[DB DEBUG] Invalid site_id=" + siteId + " in getWebsitesWithBlockedStatus!");
                        continue;
                    }
                }
                boolean blocked = isSiteBlocked(siteId);
                result.add(new SimpleEntry<>(siteId, new Pair<>(url, blocked)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}