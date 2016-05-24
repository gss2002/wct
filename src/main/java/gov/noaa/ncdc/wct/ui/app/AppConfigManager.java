package gov.noaa.ncdc.wct.ui.app;

import gov.noaa.ncdc.wct.ResourceUtils;
import gov.noaa.ncdc.wct.WCTConstants;
import gov.noaa.ncdc.wct.WCTUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class AppConfigManager {

    private static AppConfigManager config = null;
    private Document doc;
    private XPath xpath;
    private ArrayList<AppInfo> appList = new ArrayList<AppInfo>();

    
    private AppConfigManager() throws ParserConfigurationException, SAXException, IOException {
    }
    
    public static AppConfigManager getInstance() throws ParserConfigurationException, SAXException, IOException, NumberFormatException, XPathExpressionException {
        if (config == null) {
            config = new AppConfigManager();
            try {
                config.addConfig(ResourceUtils.getInstance().getJarResource(
                        new URL(WCTConstants.CONFIG_JAR_URL), ResourceUtils.CONFIG_CACHE_DIR, "/config/appInfo.xml", null));
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return config;
    }

    
    
    public void addConfig(URL url) throws SAXException, IOException, ParserConfigurationException, NumberFormatException, XPathExpressionException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = domFactory.newDocumentBuilder();

        System.out.println("AppConfigManager::: LOADING: "+url);
        this.doc = builder.parse(url.toString());

        XPathFactory factory = XPathFactory.newInstance();
        this.xpath = factory.newXPath();

        int count = WCTUtils.getXPathValues(doc, xpath, "//app/@name").size();

        for (int n=1; n<count+1; n++) {
            //              System.out.println(n+": "+getXPathValue(doc, xpath, "//filename["+n+"]/filePattern/text()"));

            AppInfo a = new AppInfo();
            a.setName(WCTUtils.getXPathValue(doc, xpath, "//app["+n+"]/@name"));
            a.setURL(WCTUtils.getXPathValue(doc, xpath, "//app["+n+"]/url/@href"));
            a.setIconURL(WCTUtils.getXPathValue(doc, xpath, "//app["+n+"]/icon/@href"));
            a.setInfo(WCTUtils.getXPathValue(doc, xpath, "//app["+n+"]/info/text()"));

            appList.add(a);
        }
    }


    public ArrayList<AppInfo> getAppList() {
        return appList;
    }

    /**
     * Get a list of available apps.
     * @return
     * @throws Exception
     */
    public ArrayList<String> getAppNames() throws Exception {
        return WCTUtils.getXPathValues(doc, xpath, "//app//@name");
    }

    
    public AppInfo getAppInfo(String name) throws Exception {
        AppInfo a = new AppInfo();
        a.setName(WCTUtils.getXPathValue(doc, xpath, "//app[@name='"+name+"']/@name"));
        a.setURL(WCTUtils.getXPathValue(doc, xpath, "//app[@name='"+name+"']/url/@href"));
        a.setIconURL(WCTUtils.getXPathValue(doc, xpath, "//app[@name='"+name+"']/icon/@href"));
        a.setInfo(WCTUtils.getXPathValue(doc, xpath, "//app[@name='"+name+"']/info/text()"));
        
        return a;
    }

}
