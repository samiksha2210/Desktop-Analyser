package models;

import java.time.LocalDateTime;

public class ActivityLog {
    private int activityId;
    private Integer appId;
    private Integer siteId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration; // in seconds

    public ActivityLog(int activityId, Integer appId, Integer siteId,
                       LocalDateTime startTime, LocalDateTime endTime, int duration) {
        this.activityId = activityId;
        this.appId = appId;
        this.siteId = siteId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
    }

    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }

    public Integer getAppId() { return appId; }
    public void setAppId(Integer appId) { this.appId = appId; }

    public Integer getSiteId() { return siteId; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
