import java.util.HashMap;
import java.util.Map;

public class CodingGamingDetector {
    // 1 = Productive, 2 = Distracting, 0 = Unknown/Browser/Other
    private static final Map<String, String> PROCESS_TO_APP = new HashMap<>();
    private static final Map<String, Integer> PROCESS_TO_CATEGORY = new HashMap<>();

    static {
        // IDEs / coding (productive)
        PROCESS_TO_APP.put("idea64.exe", "IntelliJ IDEA");
        PROCESS_TO_CATEGORY.put("idea64.exe", 1);

        PROCESS_TO_APP.put("pycharm64.exe", "PyCharm");
        PROCESS_TO_CATEGORY.put("pycharm64.exe", 1);

        PROCESS_TO_APP.put("code.exe", "Visual Studio Code");
        PROCESS_TO_CATEGORY.put("code.exe", 1);

        PROCESS_TO_APP.put("eclipse.exe", "Eclipse");
        PROCESS_TO_CATEGORY.put("eclipse.exe", 1);

        PROCESS_TO_APP.put("sublime_text.exe", "Sublime Text");
        PROCESS_TO_CATEGORY.put("sublime_text.exe", 1);

        PROCESS_TO_APP.put("notepad++.exe", "Notepad++");
        PROCESS_TO_CATEGORY.put("notepad++.exe", 1);

        // Browsers (unknown category; websites will be logged separately)
        PROCESS_TO_APP.put("chrome.exe", "Google Chrome");
        PROCESS_TO_CATEGORY.put("chrome.exe", 0);

        PROCESS_TO_APP.put("msedge.exe", "Microsoft Edge");
        PROCESS_TO_CATEGORY.put("msedge.exe", 0);

        PROCESS_TO_APP.put("firefox.exe", "Mozilla Firefox");
        PROCESS_TO_CATEGORY.put("firefox.exe", 0);

        PROCESS_TO_APP.put("brave.exe", "Brave Browser");
        PROCESS_TO_CATEGORY.put("brave.exe", 0);

        // Common productive apps
        PROCESS_TO_APP.put("outlook.exe", "Outlook");
        PROCESS_TO_CATEGORY.put("outlook.exe", 1);

        PROCESS_TO_APP.put("slack.exe", "Slack");
        PROCESS_TO_CATEGORY.put("slack.exe", 1);

        // Games (distracting)
        PROCESS_TO_APP.put("steam.exe", "Steam");
        PROCESS_TO_CATEGORY.put("steam.exe", 2);

        PROCESS_TO_APP.put("valorant.exe", "Valorant");
        PROCESS_TO_CATEGORY.put("valorant.exe", 2);

        PROCESS_TO_APP.put("csgo.exe", "CS:GO");
        PROCESS_TO_CATEGORY.put("csgo.exe", 2);

        PROCESS_TO_APP.put("minecraft.exe", "Minecraft");
        PROCESS_TO_CATEGORY.put("minecraft.exe", 2);

        PROCESS_TO_APP.put("leagueoflegends.exe", "League of Legends");
        PROCESS_TO_CATEGORY.put("leagueoflegends.exe", 2);

        // Media / entertainment
        PROCESS_TO_APP.put("spotify.exe", "Spotify");
        PROCESS_TO_CATEGORY.put("spotify.exe", 2);

        PROCESS_TO_APP.put("vlc.exe", "VLC Media Player");
        PROCESS_TO_CATEGORY.put("vlc.exe", 2);

        // Add whatever else you use commonly...
    }

    public static String getAppNameForProcess(String processExe) {
        if (processExe == null) return null;
        return PROCESS_TO_APP.get(processExe.toLowerCase());
    }

    public static int getCategoryForProcess(String processExe) {
        if (processExe == null) return 0;
        return PROCESS_TO_CATEGORY.getOrDefault(processExe.toLowerCase(), 0);
    }

    /**
     * Convenience: ensure the app exists in DB and return app_id.
     * Relies on DatabaseHelper.insertApplicationIfNotExists(appName, categoryId).
     */
    public static int ensureAppInDb(String processExe) {
        String appName = getAppNameForProcess(processExe);
        if (appName == null) appName = processExe; // fallback to exe name
        int cat = getCategoryForProcess(processExe);
        return DatabaseHelper.insertApplicationIfNotExists(appName, cat);
    }
}
