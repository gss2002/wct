package gov.noaa.ncdc.wct.ui.app;

public class AppInfo {

    private String name;
    private String url;
    private String iconUrl;
    private String info;
    
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }   
    public void setURL(String url) {
        this.url = url;
    }
    public String getURL() {
        return url;
    }

    public void setIconURL(String iconUrl) {
        this.iconUrl = iconUrl;
    }
    public String getIconURL() {
        return iconUrl;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public String getInfo() {
        return info;
    }
    public String toString() {
        return this.name+": "+this.url+" , "+this.iconUrl+" , "+this.info;
    }

}
