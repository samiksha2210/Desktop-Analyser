import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebsiteTracker {

    // Common browser suffixes we expect in windows window titles
    private static final String[] BROWSER_SUFFIXES = {
            " - google chrome", " — google chrome", " – google chrome",
            " - microsoft edge", " - firefox", " - mozilla firefox",
            " - brave", " - opera", " - chromium", " - chrome"
    };

    // Simple domain keywords mapping (keyword -> canonical domain)
    // Extend this mapping as you use more websites
    private static final String[][] KEYWORD_MAP = {
            {"youtube", "youtube.com"},
            {"stack overflow", "stackoverflow.com"},
            {"stackoverflow", "stackoverflow.com"},
            {"github", "github.com"},
            {"gitlab", "gitlab.com"},
            {"gmail", "mail.google.com"},
            {"reddit", "reddit.com"},
            {"twitter", "twitter.com"},
            {"linkedin", "linkedin.com"},
            {"medium", "medium.com"},
            {"google drive", "drive.google.com"},
            {"notion", "notion.so"},
            {"discord", "discord.com"},
            {"zoom", "zoom.us"},
            {"coursera", "coursera.org"},
            {"udemy", "udemy.com"},
            {"amazon", "amazon.com"},
            {"youtube music", "music.youtube.com"}
    };

    // A small regex to capture domains when present like "site.com - Google Chrome"
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("([\\w.-]+\\.[a-z]{2,6})", Pattern.CASE_INSENSITIVE);

    /**
     * Try to extract a domain (site) from a window title.
     * Returns canonical domain string like "youtube.com" or null if unknown.
     */
    public static String extractDomainFromTitle(String title) {
        if (title == null) return null;
        String t = title.trim().toLowerCase(Locale.ROOT);

        // 1) Remove common browser suffixes
        for (String suffix : BROWSER_SUFFIXES) {
            if (t.endsWith(suffix)) {
                t = t.substring(0, t.length() - suffix.length()).trim();
                break;
            }
        }

        // 2) If the remaining title contains a raw domain, return it
        Matcher m = DOMAIN_PATTERN.matcher(t);
        if (m.find()) {
            String domain = m.group(1).toLowerCase(Locale.ROOT);
            // normalize www.
            if (domain.startsWith("www.")) domain = domain.substring(4);
            return domain;
        }

        // 3) Keyword mapping (title often contains site name)
        for (String[] kv : KEYWORD_MAP) {
            String keyword = kv[0];
            String domain = kv[1];
            if (t.contains(keyword)) return domain;
        }

        // 4) If title is of format "User - Inbox (10) - Gmail" or similar, keyword mapping may help
        return null;
    }

    /**
     * Normalize a raw domain into a canonical format (remove trailing slashes etc).
     */
    public static String normalizeDomain(String domain) {
        if (domain == null) return null;
        domain = domain.trim().toLowerCase(Locale.ROOT);
        if (domain.startsWith("http://")) domain = domain.substring(7);
        if (domain.startsWith("https://")) domain = domain.substring(8);
        if (domain.startsWith("www.")) domain = domain.substring(4);
        if (domain.endsWith("/")) domain = domain.substring(0, domain.length() - 1);
        return domain;
    }
}
