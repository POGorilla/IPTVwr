package com.proiect.iptv.dto;

public class WatchInfo {
    private String name;
    private String groupTitle;
    private String streamUrl;
    private String country;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}