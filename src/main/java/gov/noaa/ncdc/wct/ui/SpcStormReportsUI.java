package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.gui.swing.tables.FeatureTableModel;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.nexradiv.BaseMapStyleInfo;
import gov.noaa.ncdc.wct.WCTProperties;

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
	private JButton displayButton, animateButton, clearButton;
	
	
//    
//    public final static float Z_INDEX = 2+((float)1)/100 + 0.201f;
//    public final static String LAYER_NAME = "U.S. Drought Monitor";
//    public final static Color EMPTY_BACKGROUND_COLOR = new Color(255, 255, 255);
//    
//    private JList dateList;
//    private NdmcDroughtMonitor ndm = new NdmcDroughtMonitor();
//    private HashMap<String, String> dateMap = new HashMap<String, String>();
//    private AnimationFrame animationFrame = new AnimationFrame();
//    
//    private final JComboBox transparency = new JComboBox(new Object[] {
//            "  0 %", " 10 %", " 20 %", " 30 %", " 40 %", " 50 %", 
//            " 60 %", " 70 %", " 80 %", " 90 %", "100 %"
//    }); 
//    
//    private final static String OLD_TO_NEW = "Old to New";
//    private final static String NEW_TO_OLD = "New to Old";    
//    private JComboBox animationOrder = new JComboBox(new Object[] {
//            OLD_TO_NEW, NEW_TO_OLD
//    });
//    private JLabel animationOrderLabel = new JLabel("     Order:");
    
    public SpcStormReportsUI(WCTViewer parent) {      
        super(parent, "U.S. Storm Reports Browser", false);
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


		picker.setTimeZone(TimeZone.getTimeZone("GMT"));
		picker.getMonthView().setUpperBound(new Date());
		picker.getMonthView().setFlaggedDayForeground(Color.BLUE.darker());
		String lastDateProp = WCTProperties.getWCTProperty("spcReports_Date");
		if (lastDateProp != null) {
			try {
				picker.setDate(SDF_YYYYMMDD.parse(lastDateProp));
			} catch (ParseException e1) {
				picker.setDate(new Date());
				e1.printStackTrace();
			}
		}
		else {
			picker.setDate(new Date());
		}
		picker.addPropertyChangeListener(e -> {
			if ("date".equals(e.getPropertyName())) {
				// refresh upper bound, in case the next GMT day becomes available after picker has been created
				picker.getMonthView().setUpperBound(new Date());
				try {
					displayReports();
				} catch (Exception e1) {
					e1.printStackTrace();
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
		prevDayButton.addActionListener(e -> {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.setTime(picker.getDate());
			cal.add(Calendar.DAY_OF_MONTH, -1);
			picker.setDate(cal.getTime());
			try {
				displayReports();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		JButton nextDayButton = new JButton("+1 Day");
		nextDayButton.addActionListener(e -> {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.setTime(picker.getDate());
			cal.add(Calendar.DAY_OF_MONTH, 1);
			picker.setDate(cal.getTime());			
			try {
				displayReports();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

        JPanel datePanel = new JPanel();
        datePanel.setBorder(WCTUiUtils.myTitledBorder("Select Date", 10));
        datePanel.setLayout(new RiverLayout());
        datePanel.add(prevDayButton);
        datePanel.add(picker, "hfill");
        datePanel.add(nextDayButton);
//        datePanel.add(listScrollPane, "hfill vfill");
//        datePanel.add(new JLabel("Hold the 'Select' or 'Control' keys"), "br center");
//        datePanel.add(new JLabel("to make multiple selections"), "br center");
        
//        featureTableModel.setFeatureCollection(stormReports.getFcWind());
        

        reportsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportsTable.setSortable(true);
        reportsTable.getSelectionModel().addListSelectionListener(e->{
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
        );
        

        displayButton = new JButton("Display");
        displayButton.setPreferredSize(new Dimension(60, displayButton.getPreferredSize().height));
        displayButton.addActionListener(new ActionListener() {
//            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    foxtrot.Worker.post(new foxtrot.Task() {
                        public Object run() throws Exception {
                            displayReports();
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
        
        
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new ActionListener() {
//            @Override
            public void actionPerformed(ActionEvent e) {
                showInfo();
            }
        });
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
//            @Override
            public void actionPerformed(ActionEvent e) {
                removeReportsMapLayers();
            }
        });
        clearOnClose = new JCheckBox("Clear on Close", true);
        
        JButton dataButton = new JButton("Raw Data (KML/CSV/Excel)");
        dataButton.addActionListener(new ActionListener() {
//            @Override
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
//            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.getMapSelector().setVisible(true);
                viewer.getMapSelector().setSelectedTab(2);
            }
        });
        
        
        JCheckBox showLabelsCheckBox = new JCheckBox("Show Labels?", true);
        showLabelsCheckBox.addActionListener(e -> {
        	stormReports.setLabelEnabled(showLabelsCheckBox.isSelected());
        	try {
				displayReports();
			} catch (Exception e1) {
				e1.printStackTrace();
				WCTUiUtils.showErrorMessage(this, "Error loading NOAA/SPC Reports", e1);
			}
        });
        

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new RiverLayout());
        
//        buttonPanel.add(new JLabel("Transparency: "));
//        buttonPanel.add(transparency);
        
        buttonPanel.add(displayButton, "br center");
        buttonPanel.add(showLabelsCheckBox);
        buttonPanel.add(clearButton);
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
//            @Override
            public void windowActivated(WindowEvent arg0) {
            }
//            @Override
            public void windowClosed(WindowEvent arg0) {
            }
//            @Override
            public void windowClosing(WindowEvent e) {
                if (clearOnClose.isSelected()) {
                    removeReportsMapLayers();
                }
            }
//            @Override
            public void windowDeactivated(WindowEvent arg0) {
            }
//            @Override
            public void windowDeiconified(WindowEvent arg0) {
            }
//            @Override
            public void windowIconified(WindowEvent arg0) {
            }
//            @Override
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
    
    
    
    
    
    
    
    
    
    
    public void displayReports() throws Exception {
        
//    	String yyyymmdd = "20110405";
    	String yyyymmdd = SDF_YYYYMMDD.format(picker.getDate());
    	
    	
    	stormReports.loadReports(yyyymmdd);
    	
        narrativeList.clear();
        mappedFeatures.clear();
        selectedFeatures.clear();
        
        DefaultTableModel displayTableModel = new DefaultTableModel(
        		new String[] { "Type", "Time", "Size", "Location" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
               //all cells false
               return false;
            }
        };
        
        FeatureIterator iter = stormReports.getFcTorn().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	String time = f.getAttribute("time").toString().substring(0, 2) + ":"+f.getAttribute("time").toString().substring(2, 4)+" GMT";
        	displayTableModel.addRow(new String[] {
        			"Tornado", 
        			time,
        			f.getAttribute("size").toString(), 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }
        iter = stormReports.getFcHail().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	String time = f.getAttribute("time").toString().substring(0, 2) + ":"+f.getAttribute("time").toString().substring(2, 4)+" GMT";
        	displayTableModel.addRow(new String[] {
        			"Hail", 
        			time,
        			f.getAttribute("size").toString()+" IN.", 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }
        iter = stormReports.getFcWind().features();
        while (iter.hasNext()) {
        	Feature f = iter.next();
        	String time = f.getAttribute("time").toString().substring(0, 2) + ":"+f.getAttribute("time").toString().substring(2, 4)+" GMT";
        	String size = f.getAttribute("size").toString();
        	displayTableModel.addRow(new String[] {
        			"Wind", 
        			time,
        			(size.equals("UNK")) ? size : size+" MPH", 
        			f.getAttribute("location").toString() 
        		});
        	narrativeList.add(f.getAttribute("narrative").toString());
        	mappedFeatures.add(f);
        }

        System.out.println(displayTableModel);
        System.out.println(narrativeList);
        
//        reportsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
        reportsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_LAST_COLUMN);
        reportsTable.setModel(displayTableModel);
        reportsTable.getColumn(0).setPreferredWidth(50);
        reportsTable.getColumn(1).setPreferredWidth(40);
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
