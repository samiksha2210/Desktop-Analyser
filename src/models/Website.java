package models;

public class Website {
    private int siteId;
    private String siteUrl;
    private int categoryId;

    public Website(int siteId, String siteUrl, int categoryId) {
        this.siteId = siteId;
        this.siteUrl = siteUrl;
        this.categoryId = categoryId;
    }

    public int getSiteId() { return siteId; }
    public void setSiteId(int siteId) { this.siteId = siteId; }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
}
