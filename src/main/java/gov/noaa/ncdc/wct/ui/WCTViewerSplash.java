package gov.noaa.ncdc.wct.ui;

import javax.xml.xpath.XPathConstants;

/**
 * Launches the Toolkit
 *
 * @author steve.ansari
 */
public class WCTViewerSplash {
    public static void main(String[] args) {
    	
    	System.setProperty("javax.xml.xpath.XPathFactory:" +XPathConstants.DOM_OBJECT_MODEL, "net.sf.saxon.xpath.XPathFactoryImpl");

    	
    	
    	final String[] finalArgs = args;
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
				GenericViewerSplash.startApplication(
						finalArgs, 
						"gov.noaa.ncdc.wct.ui.WCTViewer", 
						"/images/splash-homepage.jpg");
//			}
//		});

    }
    
}

