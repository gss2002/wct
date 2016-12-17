package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.gui.swing.tables.FeatureTableModel;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.renderer.j2d.LegendPosition;
import org.geotools.renderer.j2d.RenderedLogo;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;

import com.vividsolutions.jts.geom.Geometry;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.nexradiv.BaseMapStyleInfo;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.nexrad.RadarHashtables;
import gov.noaa.ncdc.wct.decoders.nexrad.RadarHashtables.SearchFilter;
import gov.noaa.ncdc.wct.io.ScanResults;
import gov.noaa.ncdc.wct.io.WCTDataSourceDB;

public class SpcStormReportsUI extends JDialog {
	
	private SpcStormReports stormReports = null;
	private MapLayer hailReportsMapLayer, tornadoReportsMapLayer, windReportsMapLayer;
	private ArrayList<Feature> mappedFeatures = new ArrayList<Feature>();
	private FeatureCollection selectedFeatures = FeatureCollections.newCollection();
	private MapLayer selectedReportsMapLayer = null;
	private final JXDatePicker picker = new JXDatePicker(new Date(), Locale.US);
	private final FeatureTableModel featureTableModel = new FeatureTableModel();
	private final ArrayList<String> narrativeList = new ArrayList<String>();
	private final JTextPane narrativeTextPane = new JTextPane();
    private final JXTable reportsTable = new JXTable();
	
	public final static SimpleDateFormat SDF_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
	
	private WCTViewer viewer;
	private JCheckBox clearOnClose;
	private JButton displayButton, clearButton;
	private JLabel statusLabel = new JLabel();
	
	
    
    public SpcStormReportsUI(WCTViewer parent) {      
        super(parent, "U.S. Preliminary Storm Reports Browser", false);
        this.viewer = parent;
        
        init();
        
        
        
//        ndm.setName(LAYER_NAME);
//        ndm.setZIndex(Z_INDEX);
        
        createGUI();
        pack();
        
        try {
			displayReports();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private void init() {
    	try {
			stormReports = new SpcStormReports();
		} catch (Exception e) {
			e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this, "Init Exception: "+e, 
                    "INIT EXCEPTION", javax.swing.JOptionPane.ERROR_MESSAGE);

		}
    }


    private void createGUI() {

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new RiverLayout());

        final JDialog finalThis = this;

//		picker.setTimeZone(TimeZone.getTimeZone("GMT"));  -- this screwed things up, perhaps bug in picker?
		picker.getMonthView().setUpperBound(new Date());
		picker.getMonthView().setFlaggedDayForeground(Color.BLUE.darker());
		String lastDateProp = WCTProperties.getWCTProperty("spcReports_Date");
		if (lastDateProp != null) {
			try {
				picker.setDate(SDF_YYYYMMDD.parse(lastDateProp));
				System.out.println("1 set picker date to: "+picker.getDate() + " from "+lastDateProp);
			} catch (ParseException e1) {
				picker.setDate(new Date());
				System.out.println("2 set picker date to: "+picker.getDate() + " from nothing");
				e1.printStackTrace();
			}
		}
		else {
			picker.setDate(new Date());
			System.out.println("3 set picker date to: "+picker.getDate() + " from "+new Date());
		}
		picker.addPropertyChangeListener(new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if ("date".equals(e.getPropertyName())) {
					// refresh upper bound, in case the next GMT day becomes available after picker has been created
					picker.getMonthView().setUpperBound(new Date());
					try {
						displayReports();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		picker.getMonthView().addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent evt) {
			}
			@Override
			public void mouseMoved(MouseEvent evt) {
				Date d = picker.getMonthView().getDayAtLocation(evt.getX(), evt.getY());
				if (d != null) {
					picker.getMonthView().clearFlaggedDates();
					picker.getMonthView().setFlaggedDates(d);
				}
			}
		});
        

		JButton prevDayButton = new JButton("-1 Day");
		prevDayButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				cal.setTime(picker.getDate());
				cal.add(Calendar.DAY_OF_MONTH, -1);
				picker.setDate(cal.getTime());
				System.out.println("4 set picker date to: "+picker.getDate() + " from "+cal.toString());
				try {
                    foxtrot.Worker.post(new foxtrot.Task() {
                        public Object run() throws Exception {
                        	setIsLoading(true);
                        	System.out.println(picker.getDate());
                            displayReports();
                            setIsLoading(false);
                            return "DONE";
                        }
                    });
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		JButton nextDayButton = new JButton("+1 Day");
		nextDayButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				cal.setTime(picker.getDate());
				cal.add(Calendar.DAY_OF_MONTH, 1);
				picker.setDate(cal.getTime());		
				System.out.println("5 set picker date to: "+picker.getDate() + " from "+cal.toString());	
				try {                    
					foxtrot.Worker.post(new foxtrot.Task() {
						public Object run() throws Exception {
                        	setIsLoading(true);
                        	System.out.println(picker.getDate());
                            displayReports();
                            setIsLoading(false);
							return "DONE";
						}
					});
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

        JPanel datePanel = new JPanel();
        datePanel.setBorder(WCTUiUtils.myTitledBorder("Select Date", 10));
        datePanel.setLayout(new RiverLayout());
        datePanel.add(prevDayButton);
        datePanel.add(picker, "hfill");
        datePanel.add(statusLabel);
        datePanel.add(nextDayButton);
//        datePanel.add(listScrollPane, "hfill vfill");
//        datePanel.add(new JLabel("Hold the 'Select' or 'Control' keys"), "br center");
//        datePanel.add(new JLabel("to make multiple selections"), "br center");
        
//        featureTableModel.setFeatureCollection(stormReports.getFcWind());
        

        reportsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportsTable.setSortable(true);
        reportsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        	@Override
        	public void valueChanged(ListSelectionEvent e) {
        		//        	System.out.println(reportsTable.getSelectedRow() + "  : "+ e.getFirstIndex() + " : "+narrativeList.size());
        		//        	System.out.println(narrativeList);

        		if (reportsTable.getSelectedRow() >= 0 && reportsTable.getSelectedRow() < narrativeList.size()) {
        			narrativeTextPane.setText(narrativeList.get(reportsTable.getSelectedRow()));
        			selectedFeatures.clear();
        			//        			FeatureCollection fc = FeatureCollections.newCollection();
        			//        			fc.add
        			selectedFeatures.add(mappedFeatures.get(reportsTable.getSelectedRow()));
        		}
        		else {
        			narrativeTextPane.setText("");
        		}
        	}        	
        }
        		);
        

        displayButton = new JButton("Display");
        displayButton.setPreferredSize(new Dimension(60, displayButton.getPreferredSize().height));
        displayButton.addActionListener(new ActionListener() {
//            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    foxtrot.Worker.post(new foxtrot.Task() {
                        public Object run() throws Exception {
                        	setIsLoading(true);
                            displayReports();
                            setIsLoading(false);
                            return "DONE";
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
//        animateButton = new JButton("Animate");
//        animateButton.setPreferredSize(new Dimension(60, animateButton.getPreferredSize().height));
//        animateButton.addActionListener(new ActionListener() {
////            @Override
//            public void actionPerformed(ActionEvent e) {
//                try {
//                    animateWms();
//                    
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
        
        
        
        JButton zoomToButton = new JButton("Zoom To Selected Event");
        zoomToButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToSelected();
            }
        });
        
        
        JButton exportButton = new JButton("Export Data");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportData();
            }
        });
        
        JButton loadL2Radar = new JButton("Load Radar Data (Level-2)");
        loadL2Radar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
					loadL2Radar();
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(viewer, e1.getMessage());
					e1.printStackTrace();
				}
            }
        });
        
        JButton orderL3Radar = new JButton("Order Radar Products (Level-3)");
        orderL3Radar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
					orderL3Radar();
				} catch (MalformedURLException e1) {
					JOptionPane.showMessageDialog(viewer, e1.getMessage());
					e1.printStackTrace();
				}
            }
        });
        
        
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInfo();
            }
        });
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeReportsMapLayers();
            }
        });
        clearOnClose = new JCheckBox("Clear on Close", true);
        
        JButton dataButton = new JButton("NOAA/NWS Storm Reports Website");
        dataButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    WCTUiUtils.browse(viewer, "http://www.spc.noaa.gov/climo/reports/", "Error browsing to: http://www.spc.noaa.gov/climo/reports/");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(viewer, "No Default Browser found.\n"+
                            "Please direct you browser to \" http://www.spc.noaa.gov/climo/reports/ \"", 
                            "BROWSER CONTROL ERROR", javax.swing.JOptionPane.ERROR_MESSAGE);            
                }
            }
        });

//        transparency.setEditable(true);
//        transparency.addActionListener(new ActionListener() {
////            @Override
//            public void actionPerformed(ActionEvent e) {
//                System.out.println("itemStateChanged event");
//                
//                String value = transparency.getSelectedItem().toString().replaceAll("%", "").trim();
//                int alpha = 255 - ((int)((Integer.parseInt(value)/100.0)*255));
//                try {
//                    viewer.setWMSTransparency(LAYER_NAME, alpha, EMPTY_BACKGROUND_COLOR);
//                } catch (Exception e1) {
//                    e1.printStackTrace();
//                }
//            }
//        });

        JButton backgroundMapButton = new JButton("Background Maps");
        backgroundMapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.getMapSelector().setVisible(true);
                viewer.getMapSelector().setSelectedTab(2);
            }
        });
        
        
        final JCheckBox showLabelsCheckBox = new JCheckBox("Show Labels?", true);
        showLabelsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	stormReports.setLabelEnabled(showLabelsCheckBox.isSelected());
            	try {
					foxtrot.Worker.post(new foxtrot.Task() {
						public Object run() throws Exception {
                        	setIsLoading(true);
                            displayReports();
                            setIsLoading(false);
							return "DONE";
						}
					});
            	} catch (Exception e1) {
            		e1.printStackTrace();
            		WCTUiUtils.showErrorMessage(finalThis, "Error loading NOAA/SPC Reports", e1);
            	}
            }
        });
        

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new RiverLayout());
        
//        buttonPanel.add(new JLabel("Transparency: "));
//        buttonPanel.add(transparency);
        
        buttonPanel.add(zoomToButton, "br center");
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(loadL2Radar, "br center");
        buttonPanel.add(orderL3Radar);
        buttonPanel.add(showLabelsCheckBox, "br center");
        buttonPanel.add(clearOnClose);
//        buttonPanel.add(animateButton, "br center");
//        buttonPanel.add(animationOrderLabel);
//        buttonPanel.add(animationOrder);
        buttonPanel.add(backgroundMapButton, "br center");
        buttonPanel.add(dataButton);
        buttonPanel.add(aboutButton);

        JScrollPane reportsTableScrollPane = new JScrollPane(reportsTable);
        reportsTableScrollPane.setSize(250, 180);
        reportsTableScrollPane.setPreferredSize(new Dimension(250, 180));
        
        narrativeTextPane.setEditable(false);
        narrativeTextPane.setText("\n\n\n\n  ");
        
        JScrollPane narrativeTextScrollPane = new JScrollPane(narrativeTextPane);
        narrativeTextScrollPane.setSize(250, 180);
        
        mainPanel.add(datePanel, "hfill");
        mainPanel.add(reportsTableScrollPane, "br hfill");
        mainPanel.add(narrativeTextScrollPane, "br hfill vfill");
        mainPanel.add(buttonPanel, "br center");

        
//        animateButton.setEnabled(false);
//        animationOrderLabel.setEnabled(false);
//        animationOrder.setEnabled(false);


        this.add(mainPanel);

        this.setSize(700, 600);

                
        
        
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent arg0) {
            }
            @Override
            public void windowClosed(WindowEvent arg0) {
            }
            @Override
            public void windowClosing(WindowEvent e) {
                if (clearOnClose.isSelected()) {
                    removeReportsMapLayers();
                }
            }
            @Override
            public void windowDeactivated(WindowEvent arg0) {
            }
            @Override
            public void windowDeiconified(WindowEvent arg0) {
            }
            @Override
            public void windowIconified(WindowEvent arg0) {
            }
            @Override
            public void windowOpened(WindowEvent arg0) {
            }
        });
        
        
        JRootPane rootPane = this.getRootPane();
        InputMap iMap = rootPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

        ActionMap aMap = rootPane.getActionMap();
        aMap.put("escape", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
                if (clearOnClose.isSelected()) {
                    removeReportsMapLayers();   	
                }
            }
        });
        
        
        
        
        
        selectedReportsMapLayer = new DefaultMapLayer(selectedFeatures, getStyle());
        

    }
    
    
    private void exportData() {
    	WCTQuickExportUI exportUI = new WCTQuickExportUI(viewer, "spc_reports_export_dir", "spc_reports_export_file");
    	exportUI.showExportFeaturesDialog(mappedFeatures);
    }
    
    private void zoomToSelected() {
    	if (selectedFeatures.size() == 0) {
    		JOptionPane.showMessageDialog(viewer, "A Storm Report must be selected to proceed.");
    		return;
    	}
    	double lat = selectedFeatures.features().next().getDefaultGeometry().getCoordinate().y;
    	double lon = selectedFeatures.features().next().getDefaultGeometry().getCoordinate().x;
    	// zoom to 2 deg box centered around selected point
    	viewer.setCurrentExtent(new Rectangle2D.Double(lon-1, lat-1, 2.0, 2.0));
    }
    
    private void loadL2Radar() throws Exception {
    	if (selectedFeatures.size() == 0) {
    		JOptionPane.showMessageDialog(viewer, "A Storm Report must be selected to proceed.");
    		return;
    	}
    	Geometry g = selectedFeatures.features().next().getDefaultGeometry();
		RadarHashtables radhash = RadarHashtables.getSharedInstance();
		String closestId = radhash.getClosestICAO(g.getCoordinate().y, g.getCoordinate().x, 999999999, SearchFilter.NEXRAD_ONLY_NO_TEST_SITES);
		System.out.println("report coord: "+g.getCoordinate()+" closest found: "+closestId);
		
    	// 012345678901234567890
    	// 2016-08-22 13:34 GMT
    	String dateString = selectedFeatures.features().next().getAttribute("date").toString();
    	

        viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.NOAA_BDP_AWS);
		
        NexradBDPAccessPanel bdpAccessPanel = viewer.getDataSelector().getDataSourcePanel().getNexradBDPAccessPanel();
        bdpAccessPanel.setDate(dateString.substring(0, 4) + dateString.substring(5, 7) + dateString.substring(8, 10));
        bdpAccessPanel.setSite(closestId);

        viewer.getDataSelector().submitListFiles();
        
        
        ScanResults[] scanResults = viewer.getDataSelector().getScanResults();
        int closestIndex = 0;
    	long reportMillis = WCTUtils.DATE_HOUR_MIN_FORMATTER.parse(dateString.substring(0, 16)).getTime();
    	long scannedMillis = WCTUtils.SCAN_RESULTS_FORMATTER.parse(scanResults[0].getTimestamp()).getTime();
    	long diff = Math.abs(reportMillis-scannedMillis);
    	
        for (int n=1; n<scanResults.length; n++) {
//        	System.out.print(dateString + " vs. ");
//        	System.out.println(scanResults[n].getTimestamp());
//        	012345678901234567890
//        	2016-07-28 23:22 GMT  reports timestamp
//        	20160728 21:49:24  scan results timestamp
        	reportMillis = WCTUtils.DATE_HOUR_MIN_FORMATTER.parse(dateString.substring(0, 16)).getTime();
        	scannedMillis = WCTUtils.SCAN_RESULTS_FORMATTER.parse(scanResults[n].getTimestamp()).getTime();
        	if (Math.abs(reportMillis-scannedMillis) < diff) {
        		diff = Math.abs(reportMillis-scannedMillis);
        		closestIndex = n;
        	}
//        	if (scanResults[n].getFileName().equals(folders[7])) {
//        		viewer.getDataSelector().getResultsList().setSelectedIndex(n);
//              viewer.getDataSelector().getResultsList().ensureIndexIsVisible(n);
//        	}
        }         
        
        // don't match if we can't get within 10 min
        if (diff > 10L*60*1000) {
        	JOptionPane.showMessageDialog(viewer, "Unable to find a matching Level-2 NEXRAD file within 10 minutes of storm report");
        }
        else {
        	viewer.getDataSelector().getResultsList().setSelectedIndex(closestIndex);
        	viewer.getDataSelector().getResultsList().ensureIndexIsVisible(closestIndex);
        	viewer.getDataSelector().setIsAutoExtentSelected(false);
        	viewer.getDataSelector().loadData();
        }
		
    }
    
    private void orderL3Radar() throws MalformedURLException {
    	if (selectedFeatures.size() == 0) {
    		JOptionPane.showMessageDialog(viewer, "A Storm Report must be selected to proceed.");
    		return;
    	}
    	Geometry g = selectedFeatures.features().next().getDefaultGeometry();
		RadarHashtables radhash = RadarHashtables.getSharedInstance();
		String closestId = radhash.getClosestICAO(g.getCoordinate().y, g.getCoordinate().x, 999999999, SearchFilter.NEXRAD_ONLY_NO_TEST_SITES); 

    	// 012345678901234567890
    	// 2016-08-22 13:34 GMT
    	String dateString = selectedFeatures.features().next().getAttribute("date").toString();
    	String yyyy = dateString.substring(0, 4);
    	String mm = dateString.substring(5, 7);
    	String dd = dateString.substring(8, 10);
    	
//    	https://www.ncdc.noaa.gov/nexradinv/displaygraphs.jsp?mm=07&dd=25&yyyy=2016&product=ABL3ALL&filter=&id=KGSP
    	String webpage = "https://www.ncdc.noaa.gov/nexradinv/displaygraphs.jsp?"
    			+ "mm="+mm+"&dd="+dd+"&yyyy="+yyyy+"&product=ABL3ALL&filter=&id="+closestId;

        WCTUiUtils.browse(viewer, webpage, "Error browsing to: "+webpage);
    }
    
    
    private Style getStyle() {
    	StyleBuilder sb = new StyleBuilder();
		try {
	        Mark plmark = sb.createMark(StyleBuilder.MARK_CIRCLE, Color.BLACK, Color.YELLOW, 3.5);
		    plmark.getFill().setOpacity(sb.literalExpression(0.0));
//	        plmark.setSize(sb.literalExpression(100));
	        Graphic plgr = sb.createGraphic(null, plmark, null);
	        plgr.setSize(sb.literalExpression(20.0));
	        PointSymbolizer plps = sb.createPointSymbolizer(plgr);
	        Style style = sb.createStyle(plps);

          	String attName = "label";
        	String[] attNameArray = attName.split(",");	        	
    		double yDisplacment = -5;
    		Symbolizer[] symbArray = new Symbolizer[attNameArray.length];
        	for (int n=0; n<attNameArray.length; n++) {
        		TextSymbolizer ts = sb.createTextSymbolizer(
        				Color.GREEN.darker(), BaseMapManager.GT_FONT_ARRAY[0], attNameArray[n]);
        		ts.setHalo(sb.createHalo(new Color(10, 10, 10), .7, 2.2));
        		ts.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 5.0, yDisplacment, 0));
        		yDisplacment += 10;    		
        		symbArray[n] = ts;
        	}
        	
    		style.addFeatureTypeStyle(sb.createFeatureTypeStyle(symbArray, 
    				BaseMapStyleInfo.NO_MIN_SCALE, BaseMapStyleInfo.NO_MAX_SCALE));
    		

    		return style;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
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
    
    
    
    public void displayReports() throws Exception {
        
//    	String yyyymmdd = "20110405";
    	System.out.println(picker.getDate());
    	String yyyymmdd = SDF_YYYYMMDD.format(picker.getDate());
    	System.out.println(yyyymmdd);
    	
    	stormReports.loadReports(yyyymmdd);
    	
        narrativeList.clear();
        mappedFeatures.clear();
        selectedFeatures.clear();
        
        DefaultTableModel displayTableModel = new DefaultTableModel(
        		new String[] { "Type", "Date/Time", "Size", "Location" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
               //all cells false
               return false;
            }
        };
        
        FeatureIterator iter = stormReports.getFcTorn().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	displayTableModel.addRow(new String[] {
        			"Tornado", 
        			f.getAttribute("date").toString(),
        			f.getAttribute("size").toString(), 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }
        iter = stormReports.getFcHail().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	displayTableModel.addRow(new String[] {
        			"Hail", 
        			f.getAttribute("date").toString(),
        			f.getAttribute("size").toString()+" IN.", 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }
        iter = stormReports.getFcWind().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	String size = f.getAttribute("size").toString();
        	displayTableModel.addRow(new String[] {
        			"Wind", 
        			f.getAttribute("date").toString(),
        			(size.equals("UNK")) ? size : size+" MPH", 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }

//        System.out.println(displayTableModel);
//        System.out.println(narrativeList);
        
//        reportsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
        reportsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_LAST_COLUMN);
        reportsTable.setModel(displayTableModel);
        reportsTable.getColumn(0).setPreferredWidth(50);
        reportsTable.getColumn(1).setPreferredWidth(95);
        reportsTable.getColumn(2).setPreferredWidth(40);
//        reportsTable.getColumn(3).setWidth(4000);

        
        
    	
        narrativeTextPane.setText("");
    	removeReportsMapLayers();

    	hailReportsMapLayer = stormReports.getHailMapLayer();
    	tornadoReportsMapLayer = stormReports.getTornadoMapLayer();
    	windReportsMapLayer = stormReports.getWindMapLayer();
    	
    	
    	System.out.println("hail bounds: "+hailReportsMapLayer.getFeatureSource().getBounds());
    	System.out.println("hail feature count: "+hailReportsMapLayer.getFeatureSource().getFeatures().getCount());
//    	viewer.setCurrentExtent(WCTUtils.toRectangle(hailReportsMapLayer.getFeatureSource().getBounds()));
    	
    	
    	hailReportsMapLayer.setVisible(true);
    	tornadoReportsMapLayer.setVisible(true);
    	windReportsMapLayer.setVisible(true);
    	selectedReportsMapLayer.setVisible(true);
    	
    	MapContext map = viewer.getMapContext();
        map.addLayer(windReportsMapLayer);
        map.addLayer(hailReportsMapLayer);
        map.addLayer(tornadoReportsMapLayer);
        map.addLayer(selectedReportsMapLayer);
        

        featureTableModel.setFeatureCollection(stormReports.getFcWind());
        
        revalidate();
        
        URL imageURL = WCTViewer.class.getResource("/images/spc-reports-legend.png");
        Image img = null;
        if (imageURL != null) {
            img = new ImageIcon(imageURL).getImage();
        }
        else {
            System.err.println("NIDIS Logo image not found");
        }

        RenderedLogo logo = new RenderedLogo(img);
        logo.setZOrder(500.3f);
        logo.setPosition(LegendPosition.SOUTH_EAST);
        logo.setInsets(new Insets(0, 0, 18, img.getWidth(null)));
        
        viewer.displayCustomLegend("Storm Reports Legend", logo);
        
        
        
//        for (MapLayer l : map.getLayers()) {
//        	System.out.println("MAP LAYER: "+l.getTitle() + " : feature_count="+l.getFeatureSource().getFeatures().getCount());
//        }
//        map.moveLayer(map.indexOf(hailReportsMapLayer), map.getLayerCount()-1);

//        hailReportsMapLayer.setVisible(false);
//        hailReportsMapLayer.setVisible(true);
    	
        
        
        
//        WCTMapPane mp = new WCTMapPane();
//        WCTMapContext context = new WCTMapContext();
//        context.addLayer(hailReportsMapLayer);
//        context.addLayer(viewer.getMapContext().getLayer(0));
//        context.addLayer(viewer.getMapContext().getLayer(1));
//        context.addLayer(viewer.getMapContext().getLayer(2));
//        mp.setMapContext(context);
//        JFrame frame = new JFrame("test");
//        frame.getContentPane().add(mp);
//        frame.pack();
//        frame.setVisible(true);
        
    }
    

    public void removeReportsMapLayers() {
    	if (hailReportsMapLayer != null) {
    		viewer.getMapContext().removeLayer(hailReportsMapLayer);
    	}
    	if (tornadoReportsMapLayer != null) {
    		viewer.getMapContext().removeLayer(tornadoReportsMapLayer);
    	}
    	if (windReportsMapLayer != null) {
    		viewer.getMapContext().removeLayer(windReportsMapLayer);
    	}
    	if (selectedReportsMapLayer != null) {
    		viewer.getMapContext().removeLayer(selectedReportsMapLayer);
    	}
    	

        viewer.removeCustomLegend("Storm Reports Legend");
    }
   
    
//    public void animateWms() {
//        
//        if (viewer.getMapPaneZoomChange().getWmsResources().contains(ndm)) {
//            viewer.getMapPaneZoomChange().removeWmsResource(ndm);
//        }
//
//        
//        int[] selectedIndices = dateList.getSelectedIndices();
//        NdmcDroughtMonitor[] wmsResources = new NdmcDroughtMonitor[selectedIndices.length];
//        for (int n=0; n<selectedIndices.length; n++) {
//            wmsResources[n] = new NdmcDroughtMonitor();
//            wmsResources[n].setYYMMDD(dateMap.get(dateList.getModel().getElementAt(selectedIndices[n])));            
//            wmsResources[n].setName(LAYER_NAME);
//            wmsResources[n].setZIndex(Z_INDEX);
//            wmsResources[n].setImageSize(viewer.getMapPane().getWCTZoomableBounds(new java.awt.Rectangle()));
//        }
//        if (animationOrder.getSelectedItem().equals(OLD_TO_NEW)) {
//            WCTUtils.flipArray(wmsResources);
//        }
//        
//        LoadWmsAnimationThread animationThread = new LoadWmsAnimationThread(
//                viewer, 
//                animationFrame,
//                wmsResources, animateButton, true);
//        
//        String value = transparency.getSelectedItem().toString().replaceAll("%", "").trim();
//        int alpha = 255 - ((int)((Integer.parseInt(value)/100.0)*255));
//        animationThread.setAlpha(alpha);
//        animationThread.setEmptyBackgroundColor(null);
//        
//        animationThread.start();
//                
//    }
//    
//    public void clearWmsBackground() {
//        viewer.removeWMS(LAYER_NAME);
//        viewer.getMapPaneZoomChange().removeWmsResource(ndm);
//    }
    

    
    public void showInfo() {
//        JEditorPane editPane = new JEditorPane();
//        editPane.setContentType("text/plain");
//        editPane.setText(ndm.getInfo());
//        
//        JDialog info = new JDialog(this, true);
//        info.add(editPane);
//        info.setVisible(true);
        
        JOptionPane.showMessageDialog(viewer,
                stormReports.getInfo(), 
                "About the Drought Monitor", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    

//    private void listDates() {
//
//        try {
//            
//            SimpleDateFormat sdfIn = new SimpleDateFormat("yyMMdd");
//            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE, MMM dd, yyyy");
//            dateMap.clear();
//
//
//            ArrayList<String> dates = ndm.getDates();
//            DefaultListModel listModel = new DefaultListModel();
//            for (int n=0; n<dates.size(); n++) {
//                String yyMMdd = dates.get(dates.size()-n-1);
//                String dateString = sdfOut.format(sdfIn.parse(yyMMdd));
//                dateMap.put(dateString, yyMMdd);
//                listModel.add(n, dateString);
//            }
//            dateList.setModel(listModel);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            dateMap.clear();
//            DefaultListModel listModel = new DefaultListModel();
//            listModel.add(0, "Error Getting Dates");
//            dateList.setModel(listModel);
//        }
//    }

    
    
    
    class ClickHandler implements MouseListener {

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                try {
                    displayReports();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    String message = e.getMessage();
                    javax.swing.JOptionPane.showMessageDialog(null, message, "DATA LOAD ERROR", JOptionPane.INFORMATION_MESSAGE);
                    e.printStackTrace();
                }

            }
        }

        public void mouseEntered(MouseEvent arg0) {
        }

        public void mouseExited(MouseEvent arg0) {
        }

        public void mousePressed(MouseEvent arg0) {
        }

        public void mouseReleased(MouseEvent arg0) {
        }

    }

}
