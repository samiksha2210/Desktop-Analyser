package models;

public class Application {
    private int appId;
    private String appName;
    private int categoryId;

    public Application(int appId, String appName, int categoryId) {
        this.appId = appId;
        this.appName = appName;
        this.categoryId = categoryId;
    }

    public int getAppId() { return appId; }
    public void setAppId(int appId) { this.appId = appId; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
}
