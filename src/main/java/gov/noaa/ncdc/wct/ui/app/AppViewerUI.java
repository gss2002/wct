package gov.noaa.ncdc.wct.ui.app;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.ui.CDOServicesUI;
import gov.noaa.ncdc.wct.ui.WCTViewer;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public class AppViewerUI extends JDialog {

    private WCTViewer viewer;
    private JList<String> appList;
    private JLabel statusLabel = new JLabel();
    
    private static AppViewerUI singleton = null;
    
    

    WebView view = null;
    WebEngine engine = null;

    
    
    
    
    
    public static AppViewerUI getInstance(final WCTViewer viewer) {
    	if (singleton == null) {
    		

//            Platform.runLater(new Runnable() {
//                @Override public void run() {
//                	   try {

//                   		singleton = new AppViewerUI(viewer);

//                       } catch (Exception ex) {
//                           ex.printStackTrace();
//                       }
//               }     
//           });
//        	
    		singleton = new AppViewerUI(viewer);
    	}
    	return singleton;
    }
    
    
    private AppViewerUI(WCTViewer viewer) {      
        super(viewer, "App Browser", false);
        this.viewer = viewer;
        createGUI();
        setSize(800, 660);
    }
    
    
    
    private void createGUI() {
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        appList = new JList<String>();
        JScrollPane listScrollPane = new JScrollPane(appList);
        
        try {
			AppConfigManager appConfig = AppConfigManager.getInstance();
			List<AppInfo> appInfoList = appConfig.getAppList();
			String[] nameArray = new String[appInfoList.size()];
			for (int n=0; n<appInfoList.size(); n++) {
				nameArray[n] = appInfoList.get(n).getName();
			}
			appList.setListData(nameArray);
			
		} catch (NumberFormatException | XPathExpressionException
				| ParserConfigurationException | SAXException | IOException e1) {
			e1.printStackTrace();
		}
        
        JButton loadButton = new JButton("Load");
        loadButton.setActionCommand("SUBMIT");
        loadButton.addActionListener(new SubmitListener(this));

        
        
        

        final JFXPanel jfxPanel = new JFXPanel();
        
        Platform.runLater(new Runnable() {
            @Override public void run() {

                view = new WebView();
                engine = view.getEngine();

                jfxPanel.setScene(new Scene(view));
                
                
                AppConfigManager appConfig;
				try {
					appConfig = AppConfigManager.getInstance();
//	                AppInfo appInfo = appConfig.getAppInfo(appList.getSelectedValue());
	                AppInfo appInfo = appConfig.getAppInfo(appList.getModel().getElementAt(0));
	                
	                engine.load(appInfo.getURL());
	            	
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
        
        
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton copyButton = new JButton("Copy (Screenshot)");
        
        JButton openExtButton = new JButton("Open in Web Browser");
        openExtButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
	                AppInfo appInfo = AppConfigManager.getInstance().getAppInfo(appList.getSelectedValue());
	                Desktop.getDesktop().browse(new URI(appInfo.getURL()));
	            	
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}        	
        });
        
        
        
        JPanel buttonPanel = new JPanel(new RiverLayout());
        buttonPanel.add(loadButton);
        buttonPanel.add(addButton, "tab");
        buttonPanel.add(removeButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(openExtButton);
        
        
        mainPanel.add(listScrollPane, BorderLayout.WEST);
        mainPanel.add(jfxPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        
//        mainPanel.add(listStationsButton, "br");
//        mainPanel.add(showDataFormButton);
//        mainPanel.add(statusLabel);
        
        this.add(mainPanel);
        
        this.setSize(700, 600);
        

    }
    
    
    private void loadApp() {
    	

        try {
        	 
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    
                    AppConfigManager appConfig;
    				try {
    					appConfig = AppConfigManager.getInstance();
    	                AppInfo appInfo = appConfig.getAppInfo(appList.getSelectedValue());
    	                
    	                engine.load(appInfo.getURL());
    	            	
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                }
            });
            
        } catch (Exception e) {
//            siteListCombo = 
        	e.printStackTrace();
        }
    }
    
    private void setIsLoading(boolean isLoading) {
    	if (isLoading) {
    		statusLabel.setIcon(new ImageIcon(CDOServicesUI.class.getResource("/icons/ajax-loader.gif")));  
    	}
    	else {
    		statusLabel.setIcon(null);
    	}
    }
    
    
    
    
    
    

	private final class SubmitListener implements ActionListener {
        private Dialog parent;
        public SubmitListener(Dialog parent) {
            this.parent = parent;
        }
        public void actionPerformed(ActionEvent e) {

            if (e.getActionCommand().equalsIgnoreCase("SUBMIT")) {
            	
            	((JButton)e.getSource()).setEnabled(false);
                try {
                	
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                        	   try {

                               	setIsLoading(true);
                               	loadApp();
                               	setIsLoading(false);

                               } catch (Exception ex) {
                                   ex.printStackTrace();
                               }
                       }     
                   });
                	
                	
//                    foxtrot.Worker.post(new foxtrot.Task() {
//                        public Object run() {
//
//                            try {
//
//                            	setIsLoading(true);
//                            	loadApp();
//                            	setIsLoading(false);
//
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
//
//                            return "DONE";
//                        }
//                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            	((JButton)e.getSource()).setEnabled(true);

            }
            else {
            	parent.dispose();
            }
        }
	}
	
}
