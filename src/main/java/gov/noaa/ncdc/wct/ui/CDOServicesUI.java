package gov.noaa.ncdc.wct.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.map.DefaultMapLayer;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXLabel;
import org.json.simple.parser.ParseException;

import com.lowagie.text.DocumentException;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.nexradiv.BaseMapStyleInfo;

public class CDOServicesUI extends WCTFrame {

    private WCTViewer viewer;
	private CDOStationDataForm form = new CDOStationDataForm(this);
    private JList<String> siteList;
    private JLabel statusLabel = new JLabel();
    private JLabel countLabel = new JLabel();
    private JPanel mainPanel = new JPanel(new BorderLayout());
    private JXLabel infoLabel = new JXLabel("This tool provides access to quality-controlled daily weather "
			  + "station data from NOAA's National Centers for Environmental "
			  + "Information (NCEI). The data displayed is the Global Historical "
			  + "Climatology Network - Daily (GHCN-D), which includes over "
			  + "100,000 worldwide stations and is updated daily.");
    
    private JComboBox<String> selectYearCombo = new JComboBox<String>(new String[] { "2016", "2015", "2014" });
    private JComboBox<String> selectMonthCombo = new JComboBox<String>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" });
  
    
    public final static Color BACKGROUND_COLOR_1 = Color.WHITE;
    public final static Color BACKGROUND_COLOR_2 = new Color(240, 240, 230);
    
    
    private static CDOServicesUI singleton = null;
    
    public static CDOServicesUI getInstance(WCTViewer viewer) {
    	if (singleton == null) {
    		singleton = new CDOServicesUI(viewer);
    	}
    	return singleton;
    }
    
    
    private CDOServicesUI(WCTViewer viewer) {      
        super("Climate Data Online: Data Browser");
        this.viewer = viewer;
        createGUI();
//        pack();
    }
    
    
    
    private void createGUI() {
        
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new RiverLayout());
        
        siteList = new JList<String>(new String[] { "-- Select year, month and list stations below --" });
		ListCellRenderer<? super String> defaultRenderer = siteList.getCellRenderer();
		
        siteList.setCellRenderer(new ListCellRenderer<String>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean hasFocus) {
				
			    JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
			    // renderer.setBorder(cellHasFocus ? focusBorder : noFocusBorder);
			    if (! isSelected) {
			    	renderer.setBackground(index % 2 == 1 ? BACKGROUND_COLOR_2 : BACKGROUND_COLOR_1);
			    }
		    	
		    	
//		        if (isSelected) {
//		            setBackground(list.getSelectionBackground());
//		            setForeground(list.getSelectionForeground());
//		        } else {
//		            setBackground(list.getBackground());
//		            setForeground(list.getForeground());
//		        }
			    return renderer;
			}
        });
        JScrollPane listScrollPane = new JScrollPane(siteList);
        
        
        JButton listStationsButton = new JButton("List Stations in View Extent");
        listStationsButton.setActionCommand("LIST_STATIONS");
        listStationsButton.addActionListener(new SubmitListener(this));
        
        JButton showDataFormButton = new JButton("Add Data");
        showDataFormButton.setActionCommand("LOAD_DATA");
        showDataFormButton.addActionListener(new SubmitListener(this));

        JButton clearDataFormButton = new JButton("Clear Data");
        clearDataFormButton.setActionCommand("CLEAR_DATA");
        clearDataFormButton.addActionListener(new SubmitListener(this));

        JButton clearSitesFormButton = new JButton("Clear List");
        clearSitesFormButton.setActionCommand("CLEAR_LIST");
        clearSitesFormButton.addActionListener(new SubmitListener(this));
  

        infoLabel.setPreferredSize(new Dimension(350, 90));
//        infoLabel.setMaxLineSpan(10);
        infoLabel.setLineWrap(true);
        
//        infoLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        JPanel labelPanel = new JPanel(new BorderLayout());
//        labelPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        labelPanel.add(infoLabel, BorderLayout.CENTER);
        JPanel linkPanel = new JPanel();
        JXHyperlink ghcnLink = new JXHyperlink();
        ghcnLink.setText("Documentation");
        ghcnLink.addActionListener(e -> {
        	WCTUiUtils.browse(this, "https://gis.ncdc.noaa.gov/geoportal/catalog/search/resource/details.page?id=gov.noaa.ncdc:C00861", "Error browsing to NOAA/NCEI");
        });
        JXHyperlink cdoLink = new JXHyperlink();
        cdoLink.setText("Online Search/Access");
        cdoLink.addActionListener(e -> {
        	WCTUiUtils.browse(this, "http://www.ncdc.noaa.gov/cdo-web/search?datasetid=GHCND", "Error browsing to NOAA/NCEI");
        });

        JXHyperlink ftpLink = new JXHyperlink();
        ftpLink.setText("Data Files");
        ftpLink.addActionListener(e -> {
        	WCTUiUtils.browse(this, "https://www.ncdc.noaa.gov/data-access/land-based-station-data/land-based-datasets/global-historical-climatology-network-ghcn", "Error browsing to NOAA/NCEI");
        });

        linkPanel.add(new JLabel("GHCN-Daily: "));
        linkPanel.add(ghcnLink);
        linkPanel.add(new JLabel(" | "));
        linkPanel.add(cdoLink);
        linkPanel.add(new JLabel(" | "));
        linkPanel.add(ftpLink);
        labelPanel.add(linkPanel, BorderLayout.SOUTH);
        
        listPanel.add(labelPanel);
        listPanel.add(listScrollPane, "br hfill vfill");
        listPanel.add(countLabel, "br");
        listPanel.add(selectYearCombo, "br");
        listPanel.add(selectMonthCombo);
        listPanel.add(listStationsButton);
        listPanel.add(clearSitesFormButton);
        listPanel.add(showDataFormButton, "br");
        listPanel.add(clearDataFormButton);
        listPanel.add(statusLabel);
        mainPanel.add(listPanel, BorderLayout.WEST);

		if (! CDOStationDataForm.CACHE_PDF.exists()) {
			JPanel panel = new JPanel();
			panel.add(new JLabel(" <-- Please select a station and date"));
	        mainPanel.add(panel, BorderLayout.CENTER);
		}
		else {
			try {
				JPanel pdfPanel = form.loadForm(new ArrayList<String>(), 
						selectYearCombo.getSelectedItem().toString(), selectMonthCombo.getSelectedItem().toString());
		        mainPanel.add(pdfPanel, BorderLayout.CENTER);
		        
			} catch (IOException | DocumentException e1) {
				e1.printStackTrace();
			}
		}


        
        
        this.add(mainPanel);
        
        this.setSize(1050, 700);        

    }
    
    
    private void listSites() throws FactoryConfigurationError, SchemaException, IOException, ParseException, java.text.ParseException {
    	
//    	FeatureCollection fc = FeatureCollections.newCollection();
        

        	CDOServicesSupport cdo = new CDOServicesSupport();
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    		
    		viewer.getStationPointFeatures().clear();
    		
    		String yyyy = selectYearCombo.getSelectedItem().toString();
    		String mm = selectMonthCombo.getSelectedItem().toString();
    		cdo.getStationFeatures(viewer.getStationPointFeatures(), viewer.getCurrentExtent(), sdf.parse(yyyy+"-"+mm+"-01"));
    		
    		
            DefaultListModel<String> listModel = new DefaultListModel<String>();
            FeatureIterator iter = viewer.getStationPointFeatures().features();
            int cnt = 0;
            while (iter.hasNext()) {
            	Feature f = iter.next();
            	String desc = f.getAttribute("station_desc").toString();
            	if (desc.length() > 30) {
            		desc = desc.substring(0, 30)+"...";
            	}
            	String displayString = desc + " ("+f.getAttribute("station_id")+")";
                listModel.add(cnt++, displayString);
            }
            siteList.setModel(listModel);
            
            siteList.addListSelectionListener(new ListSelectionListener() {				
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (siteList.getSelectedIndex() < 0) {
						viewer.getStationPointSelectedFeatures().clear();
					}
					else {
						viewer.getStationPointSelectedFeatures().clear();
						int[] indices = siteList.getSelectedIndices();
						for (int idx : indices) {
							Feature f = (Feature)viewer.getStationPointFeatures().toArray()[idx];
							viewer.getStationPointSelectedFeatures().add(f);
						}
					}
				}
			});
            
            if (cnt < 100) {
            	countLabel.setText("<html>Found "+cnt+" stations within view extent.</html>");
            }
            else {
            	countLabel.setText("<html>Found "+cnt+" stations within view extent. (Max=100)<br/>"
            			+ "<font color=\"red\"><b>NOTICE: Not all stations are plotted.  Please zoom in and list again. </b><font></html>");
            }
            
//            infoLabel.setPreferredSize(preferredSize);
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
        private Frame parent;
        public SubmitListener(Frame parent) {
            this.parent = parent;
        }
        public void actionPerformed(ActionEvent e) {

            if (e.getActionCommand().equalsIgnoreCase("LIST_STATIONS")) {
            	
            	((JButton)e.getSource()).setEnabled(false);
                try {
                    foxtrot.Worker.post(new foxtrot.Task() {
                        public Object run() {

                            try {

                            	setIsLoading(true);
                            	listSites();
                            	setIsLoading(false);

                            } catch (Exception ex) {
                                WCTUiUtils.showErrorMessage(viewer, "Error listing sites from NCEI web service", ex);
                                setIsLoading(false);
                            }

                            return "DONE";
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    WCTUiUtils.showErrorMessage(viewer, "Error listing sites from NCEI web service", ex);
                }
            	((JButton)e.getSource()).setEnabled(true);

            }
            else if (e.getActionCommand().equalsIgnoreCase("LOAD_DATA")) {
				FeatureIterator iter = viewer.getStationPointSelectedFeatures().features();
				ArrayList<String> selectedStationList = new ArrayList<String>();
				while (iter.hasNext()) {
					selectedStationList.add(iter.next().getAttribute("station_id").toString());
				}

//				station = "GHCND:US1GARB0006";
            	((JButton)e.getSource()).setEnabled(false);
                try {
                	foxtrot.Worker.post(new foxtrot.Task() {
                		public Object run() {

                        	setIsLoading(true);
                			try {
                				if (mainPanel.getComponentCount() > 1) {
                					mainPanel.remove(1);
                				}
                				JPanel pdfPanel = form.loadForm(selectedStationList, 
                						selectYearCombo.getSelectedItem().toString(), selectMonthCombo.getSelectedItem().toString());
                				mainPanel.add(pdfPanel, BorderLayout.CENTER);
                				mainPanel.validate();
                				
                			} catch (IOException e1) {
                				// TODO Auto-generated catch block
                				e1.printStackTrace();
                			} catch (DocumentException e1) {
                				// TODO Auto-generated catch block
                				e1.printStackTrace();
                			}
                        	setIsLoading(false);

                			return "DONE";
                		}
                	});
                } catch (Exception ex) {
                    ex.printStackTrace();
                    WCTUiUtils.showErrorMessage(viewer, "Error loading data from NCEI web service", ex);
                }
            	((JButton)e.getSource()).setEnabled(true);
				
            }
            else if (e.getActionCommand().equalsIgnoreCase("CLEAR_DATA")) {
            	try {
            		form.deleteCachedForm();

            		if (mainPanel.getComponentCount() > 1) {
            			mainPanel.remove(1);
            		}
            		JPanel panel = new JPanel();
            		panel.setPreferredSize(new Dimension(800, 600));
            		panel.add(new JLabel(" <-- Please select a station and date"));
            		mainPanel.add(panel, BorderLayout.CENTER);
    				mainPanel.validate();

    			} catch (IOException e1) { 
    				WCTUiUtils.showErrorMessage(parent, "Error deleting form in cache", e1);
    			}

            }
            else if (e.getActionCommand().equalsIgnoreCase("CLEAR_LIST")) {
            	try {
            		((DefaultListModel<String>)siteList.getModel()).removeAllElements();
            		((DefaultListModel<String>)siteList.getModel()).addElement("-- Select year, month and list stations below --");
					viewer.getStationPointFeatures().clear();
					viewer.getStationPointSelectedFeatures().clear();
            		countLabel.setText("");

    			} catch (Exception e1) { 
    				WCTUiUtils.showErrorMessage(parent, "Error deleting form in cache", e1);
    			}

            }
            else {
            	parent.dispose();
            }
        }
	}
	
}
