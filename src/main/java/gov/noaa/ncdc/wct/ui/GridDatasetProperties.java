package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.WCTException;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.cdm.GridDatasetUtils;
import gov.noaa.ncdc.wct.ui.animation.WCTAnimator;
import gov.noaa.ncdc.wct.ui.event.LoadDataListener;
import gov.noaa.ncdc.wct.ui.event.MousePopupListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import opendap.dap.DAP2Exception;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.PatternFilter;

import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;

import com.javaswingcomponents.accordion.JSCAccordion;
import com.javaswingcomponents.accordion.TabOrientation;
import com.javaswingcomponents.accordion.plaf.basic.BasicAccordionUI;
import com.javaswingcomponents.accordion.plaf.basic.BasicVerticalTabRenderer;
import com.javaswingcomponents.framework.painters.configurationbound.GradientPalettePainter;
import com.javaswingcomponents.framework.palettes.Palette;
import com.jidesoft.swing.StyledLabel;
import com.jidesoft.swing.StyledLabelBuilder;

public class GridDatasetProperties extends JDialog {

    private static final long serialVersionUID = 1L;

    private Frame parent = null;
    
    private boolean isNewFileLoading = false;

    private URL gridDatasetURL;
    private GridDataset gds = null;
    private List<GridDatatype> gridList;
    private String fileTypeDescription = "";
    private HashMap<Integer, double[]> zCoordsCacheByVariableIndex = new HashMap<Integer, double[]>();
    
    private JXList gridJList = new JXList();
    private JXList rtJList = new JXList();
    private JXList timeJList = new JXList();
    private JXList zJList = new JXList();
    private JXLabel jlGridDescription = new JXLabel("");
    private JTextField jtfGridDescription = new JTextField();

    JPanel overviewPanel = new JPanel(new RiverLayout(3, 3));
    JScrollPane overviewPanelScrollPane = new JScrollPane(overviewPanel);
    

    private final JSCAccordion accordion = new JSCAccordion();
    
    JPanel gridListPanel = new JPanel();
    JPanel rtListPanel = new JPanel();
    JPanel timeListPanel = new JPanel();
    JPanel zListPanel = new JPanel();
    JPanel gridPanelParent = new JPanel();
    JPanel rtPanelParent = new JPanel();
    JPanel timePanelParent = new JPanel();
    JPanel zPanelParent = new JPanel();
    

    private JPanel mainPanel = new JPanel(new RiverLayout(1, 1));
    private JScrollPane mainScrollPane = new JScrollPane(mainPanel);
    

    private JTextField jtfVariableFilter = new JTextField(10);
    private JScrollPane gridListScrollPane = null;
    private JScrollPane timeListScrollPane = null;
    
    private int selectedGridIndex = -1;
    private int selectedRuntimeIndex = -1;
    private int selectedTimeIndex = -1;
    private int selectedZIndex = -1;
    
    private int selectedTimeZoneIndex = 0;
    
    private String varFilterString = "";
    private String timeFilterString = "";
    private String runtimeFilterString = "";
    
    private Vector<LoadDataListener> listeners = new Vector<LoadDataListener>();
    private WCTViewer viewer = null;
    private WCTAnimator animator = null;
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private static SimpleDateFormat sdf2 = new SimpleDateFormat("E yyyy-MM-dd HH:mm:ss z");
    private static NumberFormat fmt03 = new DecimalFormat("0.000");
    static {
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    private ModalityType modality = ModalityType.MODELESS;
    
    private ListSelectionListener gridListListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            
            System.out.println("gridListListener event fired");
            
//            pack();
//            getContentPane().validate();
//            validateTree();
        }
    };
    
    
    private ListCellRenderer gridListRenderer = new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			JLabel label = (JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
			
			JLabel label1 = new JLabel();
			label1.setText(label.getText().split("\\|")[0]);
			if (index % 2 == 1 && ! isSelected) {
				label1.setBackground(new Color(220, 220, 220));
			}
			else {
				label1.setBackground(label.getBackground());
			}
			label1.setForeground(label.getForeground());
			label1.setFont(label.getFont());
//			label1.setBorder(label.getBorder());
			label1.setHorizontalAlignment(label.getHorizontalAlignment());
			label1.setHorizontalTextPosition(label.getHorizontalTextPosition());

			
			
			JLabel label2 = null;
			if (label.getText().split("\\|").length > 1) {

				label2 = new JLabel();
				//			label2.setLineWrap(true);
				label2.setText(" "+label.getText().split("\\|")[1]);
				if (index % 2 == 1 && ! isSelected) {
					label2.setBackground(new Color(220, 220, 220));
				}
				else {
					label2.setBackground(label.getBackground());
				}
				label2.setBackground(label.getBackground());
				if (isSelected) {
					label2.setForeground(Color.WHITE);				
				}
				else {
					label2.setForeground(new Color(100, 100, 100));
				}
				label2.setFont(label.getFont().deriveFont(11.0f));
				//			label2.setBorder(label.getBorder());
				label2.setHorizontalAlignment(label.getHorizontalAlignment());
				label2.setHorizontalTextPosition(label.getHorizontalTextPosition());

			}

			JPanel panel = new JPanel(new BorderLayout());
			if (index % 2 == 1 && ! isSelected) {
				panel.setBackground(new Color(248, 248, 232));
			}
			else {
				panel.setBackground(label.getBackground());
			}
//			panel.setBackground(label.getBackground());
			panel.setBorder(label.getBorder());
			JPanel topPanel = new JPanel(new RiverLayout(0, 0));
			if (index % 2 == 1 && ! isSelected) {
				topPanel.setBackground(new Color(248, 248, 232));
			}
			else {
				topPanel.setBackground(label.getBackground());
			}
			
			topPanel.add(label1, "left");
			if (isSelected) {
//				JXHyperlink jxhMetadata = new JXHyperlink();
//				jxhMetadata.setText("[?]");
//				topPanel.add(jxhMetadata, "left");
			}
//			panel.add(label1, BorderLayout.NORTH);
			panel.add(topPanel, BorderLayout.NORTH);
			if (label2 != null) {
				panel.add(label2, BorderLayout.CENTER);
			}
			
			return panel;
		}
	};
	
	
    
    public GridDatasetProperties(Frame parent) {
        this(parent, ModalityType.MODELESS);
    }
    
    
    public GridDatasetProperties(Frame parent, ModalityType modality) {
        super(parent, "Grid Dataset Properties", modality);
        this.modality = modality;
        createGUI();
        pack();
        setPreferredSize(new Dimension(433, 565));
//        setVisible(true);        
    }
    
    
    private void createGUI() {

        this.setLayout(new RiverLayout(4, 4));

        final JButton loadButton = new JButton("Load");
        final JButton selectButton = new JButton("Select");
        loadButton.setPreferredSize(new Dimension(100, (int)loadButton.getPreferredSize().getHeight()));
        selectButton.setPreferredSize(new Dimension(100, (int)selectButton.getPreferredSize().getHeight()));
        
    
//        gridJList.setVisibleRowCount(8);
        gridJList.setCellRenderer(gridListRenderer);
//        rtJList.setVisibleRowCount(6);
//        timeJList.setVisibleRowCount(8);
//        zJList.setVisibleRowCount(4);
        
        
        gridListPanel.setLayout(new RiverLayout(3, 3));
        gridListPanel.setLayout(new BorderLayout());
        rtListPanel.setLayout(new RiverLayout(3, 3));
        timeListPanel.setLayout(new RiverLayout(3, 3));
        zListPanel.setLayout(new RiverLayout(3, 3));
        gridPanelParent.setLayout(new RiverLayout(2, 2));
        rtPanelParent.setLayout(new RiverLayout(2, 2));
        timePanelParent.setLayout(new RiverLayout(2, 2));
        zPanelParent.setLayout(new RiverLayout(2, 2));
        
//        timeListPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
//        timePanelParent.setBorder(BorderFactory.createLineBorder(Color.RED));
//        rtPanelParent.setBorder(BorderFactory.createLineBorder(Color.RED));
//        zPanelParent.setBorder(BorderFactory.createLineBorder(Color.RED));

        gridJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gridJList.addListSelectionListener(gridListListener);

        

        gridJList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
//				gridListScrollPane.setPreferredSize(new Dimension(300, 400));
//				gridPanelParent.setPreferredSize(new Dimension(300, 400));
//		        timeListScrollPane.setPreferredSize(new Dimension(300, 110));
//		        timePanelParent.setPreferredSize(new Dimension(300, 110));
		        System.out.println("grid list click");
//		        timeline.playReverse();
			}        	
        });
        
        timeJList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
//				timeListScrollPane.setPreferredSize(new Dimension(300, 400));
//				timePanelParent.setPreferredSize(new Dimension(300, 400));
//		        gridListScrollPane.setPreferredSize(new Dimension(300, 110));
//		        gridPanelParent.setPreferredSize(new Dimension(300, 110));
		        System.out.println("time list click");
//		        timeline.play();
			}        	
        });
        
        
        
        gridJList.setFilterEnabled(true);
        rtJList.setFilterEnabled(true);
        timeJList.setFilterEnabled(true);
        gridJList.setFont(new Font("monospaced", Font.PLAIN, 12));        
        rtJList.setFont(new Font("monospaced", Font.PLAIN, 12));        
        timeJList.setFont(new Font("monospaced", Font.PLAIN, 12));
        zJList.setFont(new Font("monospaced", Font.PLAIN, 12));        

        
        jtfVariableFilter.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                varFilterString = jtfVariableFilter.getText(); 
                filterVariableList();
            }
            public void keyTyped(KeyEvent e) {
            }
        });

        final JTextField jtfTimeFilter = new JTextField(10);
        jtfTimeFilter.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                timeFilterString = jtfTimeFilter.getText(); 
                filterTimeList();
            }
            public void keyTyped(KeyEvent e) {
            }
        });

        final JTextField jtfRuntimeFilter = new JTextField(10);
        jtfRuntimeFilter.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                runtimeFilterString = jtfRuntimeFilter.getText(); 
                filterRuntimeList();
            }
            public void keyTyped(KeyEvent e) {
            }
        });
        
        
        String[] tzIdArray = TimeZone.getAvailableIDs();
        Vector<String> ids = new Vector<String>(tzIdArray.length);
        ids.addElement("Default ("+TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)+")");
        ids.addElement("GMT");
        ids.addElement("Both");
        final JComboBox jcbTimeFormat = new JComboBox(ids);
        jcbTimeFormat.setSelectedIndex(1);
        selectedTimeZoneIndex = 1;
        jcbTimeFormat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedTimeZoneIndex = jcbTimeFormat.getSelectedIndex();
                processTimeDimension();
            }
        });
        
        jlGridDescription.setLineWrap(true);
//        jlGridDescription.setPreferredSize(new Dimension(gridJList.getPreferredSize().width, jlGridDescription.getPreferredSize().height));
        
        
        
//        gridListPanel.add("left tab hfill", new JLabel("Gridded Variables:"));
//        gridListPanel.add(new JLabel("Filter:"), "right");
//        gridListPanel.add(jtfVariableFilter, "right");

        JPanel gridListPanelTop = new JPanel(new RiverLayout(3, 3));
        gridListPanelTop.add("left tab hfill", new JLabel("Gridded Variables:"));
        gridListPanelTop.add(new JLabel("Filter:"), "right");
        gridListPanelTop.add(jtfVariableFilter, "right");
        gridListPanel.add(gridListPanelTop, BorderLayout.NORTH);
        
        
        
        jtfGridDescription.setEditable(false);
//        gridListPanel.add("left hfill br", jtfGridDescription);
//        gridListPanel.add(jbSearchWikipedia);
//        gridListPanel.add(jbSearchGoogle);
//        gridListPanel.add(jbSearchYahoo);
        
        //        JPanel gridPanel = new JPanel();
//        gridPanel.add(new JScrollPane(gridJList));
//        gridListPanel.add(gridPanel, "hfill br");
//        gridListPanel.add(gridPanel, "left hfill br");
        
        
//        gridListScrollPane = new JScrollPane(gridJList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        gridListPanel.add("left hfill br", gridListScrollPane);

        
        JPanel gridJListScrollPanePanel = new JPanel(new RiverLayout(2, 2));
        gridJListScrollPanePanel.add(new JScrollPane(gridJList), "hfill vfill");
//        gridListScrollPane.setPreferredSize(new Dimension(300, 250));
//        gridListPanel.add(gridJListScrollPanePanel, BorderLayout.CENTER);
//        gridListPanel.add(gridListScrollPane, BorderLayout.CENTER);
//        gridListPanel.add(new JScrollPane(gridJList), BorderLayout.CENTER);
        gridListPanel.add(gridJListScrollPanePanel, BorderLayout.CENTER);
        
        
        
//        gridListPanel.add("left hfill br", gridJList);
//        gridListPanel.add("left br", jlGridDescription);

        
        
        rtListPanel.add("left tab hfill", new JLabel("Available Model Run Times:"));
        rtListPanel.add(new JLabel("Filter:"), "right");
        rtListPanel.add(jtfRuntimeFilter, "right");
        rtListPanel.add(new JScrollPane(rtJList), "hfill vfill br");
        timeListPanel.add("left", new JLabel("Available Date/Times:"));
        timeListPanel.add("br left tab hfill", jcbTimeFormat);
        timeListPanel.add(new JLabel("Filter:"), "right");
        timeListPanel.add(jtfTimeFilter, "right");
        timeListScrollPane = new JScrollPane(timeJList);
        timeListPanel.add(timeListScrollPane, "hfill vfill br");
        zListPanel.add(new JLabel("Available Vertical Levels:"));
        zListPanel.add(new JScrollPane(zJList), "hfill vfill br");
        
        
        gridPanelParent.add(gridListPanel, "hfill vfill");
//        gridListPanel.setPreferredSize(new Dimension(300, gridPanelParent.getPreferredSize().height+26+
//        		jtfGridDescription.getPreferredSize().height));
//        gridPanelParent.setPreferredSize(new Dimension(300, gridPanelParent.getPreferredSize().height+26+
//        		jtfGridDescription.getPreferredSize().height));
        
        
        
    
        
        
        
        
        
        
        
        
        accordion.setUI(new BasicAccordionUI());
        
        accordion.setDrawShadow(false);
        
        accordion.setTabOrientation(TabOrientation.VERTICAL);
        BasicVerticalTabRenderer tabRenderer = (BasicVerticalTabRenderer)accordion.getTabRenderer();
        tabRenderer.setShowIndex(false);
        tabRenderer.setFontSize(12f);
        
        GradientPalettePainter backgroundPainter = (GradientPalettePainter)tabRenderer.getBackgroundSelectedPainter();
        
//        Color[] palette = new Color[5];
//        palette[0] = new Color(97, 125, 176);
//        palette[1] = new Color(97, 125, 176);
//        palette[2] = new Color(97, 125, 176).brighter();
//        palette[3] = new Color(97, 125, 176).brighter();
//        palette[4] = new Color(97, 125, 176).brighter().brighter();
        
        GradientPalettePainter gpp = new GradientPalettePainter(new Palette() {
			@Override
			public Color[] initializePalette(Color[] palette) {
		        palette[0] = new Color(220, 220, 230);
		        palette[1] = new Color(220, 220, 230);
		        palette[2] = Color.WHITE;
		        palette[3] = Color.WHITE;
		        palette[4] = Color.WHITE;
				return palette;
			}
		});
        
        GradientPalettePainter gppSelected = new GradientPalettePainter(new Palette() {
			@Override
			public Color[] initializePalette(Color[] palette) {
		        palette[0] = new Color(162, 181, 205);
		        palette[1] = new Color(162, 181, 205);
		        palette[2] = Color.WHITE;
		        palette[3] = Color.WHITE;
		        palette[4] = Color.WHITE;
				return palette;
			}
		});
        
//        backgroundPainter.getPalette().initializePalette(palette);
        tabRenderer.setBackgroundPainter(gpp);
        tabRenderer.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        tabRenderer.setFontMouseOverColor(Color.RED);
        tabRenderer.setBackgroundSelectedPainter(gppSelected);
//        accordion.setTabHeight(25);
//        accordion.set
        
        
//        LinearGradientColorPainter backgroundPainter = (LinearGradientColorPainter) accordion.getBackgroundPainter();
//		backgroundPainter.setPaletteStartColor(PaletteColor.);
//		backgroundPainter.setPnew Color(97, 125, 176).brighter().brighter() });
		
//        tabRenderer.setBackground(new Color(97, 125, 176));

        
//        overviewPanel.setPreferredSize(
//        		new Dimension(accordion.getPreferredSize().width, overviewPanel.getPreferredSize().height) );
        overviewPanel.setPreferredSize(
        		new Dimension(accordion.getPreferredSize().width, 1300) );
//        overviewPanelScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        
        accordion.addTab("Grid", gridPanelParent);
        accordion.addTab("Information", overviewPanelScrollPane);
//        System.out.println(gridList);
//		if (gridList.get(0).getCoordinateSystem().getTimeAxis1D() != null) {
//			accordion.addTab("Time", timePanelParent);
//		}
//		if (gridList.get(0).getCoordinateSystem().getVerticalAxis() != null) {
//			accordion.addTab("Height", zPanelParent);
//		}
//		if (gridList.get(0).getCoordinateSystem().getRunTimeAxis() != null) {
//			accordion.addTab("Runtime", rtPanelParent);
//		}		

//        accordion.addTab("Time", timePanelParent);
//        accordion.addTab("Height", zPanelParent);
//        accordion.addTab("Runtime", rtPanelParent);
       
		
		
        
//		timeListScrollPane.setPreferredSize(new Dimension(300, 500));
//		timePanelParent.setPreferredSize(new Dimension(300, 500));
//        gridListScrollPane.setPreferredSize(new Dimension(300, 500));
//        gridPanelParent.setPreferredSize(new Dimension(300, 500));
        
        
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(accordion, BorderLayout.CENTER);
        
        
        
        
        
        
//        mainPanel.add(gridPanelParent, "hfill br");
//        mainPanel.add(rtPanelParent, "hfill br");
//        mainPanel.add(timePanelParent, "hfill br");
//        mainPanel.add(zPanelParent, "hfill br");
        
//        mainPanel.add(loadButton, "center p");
        this.setLayout(new BorderLayout());
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(mainScrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new RiverLayout());
        if (modality == ModalityType.MODELESS) {
        	
        	JXHyperlink moreLink = getMoreLink();
        	moreLink.setText("More...");
        	
        	JPanel spacerPanel = new JPanel();
        	spacerPanel.setPreferredSize(new Dimension(
        			(int)moreLink.getPreferredSize().getWidth(), 
        			(int)loadButton.getPreferredSize().getHeight()));
            
        	
        	buttonPanel.add(new JLabel("Hold the 'Shift' or 'Control' keys to animate multiple selections"), "center");
            buttonPanel.add(spacerPanel, "br center");
        	buttonPanel.add(loadButton);
            buttonPanel.add(moreLink);
        }
        else {
        	buttonPanel.add(new JLabel("  "), "center");
            buttonPanel.add(selectButton, "br center");
        }
        this.add(buttonPanel, BorderLayout.SOUTH);
        
        
        gridJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
        		System.out.println("grid valueChanged: firstIndex: "+e.getFirstIndex());
        		if (e.getFirstIndex() < 0) {
        			gridJList.setSelectedIndex(0);
        		}

            	processGridListSelectionEvent();
            }
        });

        rtJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

            	if (! isNewFileLoading) {
            		
            	//                System.out.println("runtime valueChanged: firstIndex: "+e.getFirstIndex());
            		if (e.getFirstIndex() < 0 && rtJList.getModel().getSize() > 0) {
            			rtJList.setSelectedIndex(0);
            		}
            		processTimeDimension();
            		processZDimension();
            		selectedRuntimeIndex = (rtJList.getSelectedIndex() < 0) ? -1 : rtJList.convertIndexToModel(rtJList.getSelectedIndex());
            		processButtonText(loadButton);
                
            	}
            	
            }
        });

        timeJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
            	

            	if (! isNewFileLoading) {
            	
//                System.out.println("time valueChanged: firstIndex: "+e.getFirstIndex());
            		if (e.getFirstIndex() < 0 && timeJList.getModel().getSize() > 0) {
            			timeJList.setSelectedIndex(0);
            		}
            		processZDimension();
            		selectedTimeIndex = (timeJList.getSelectedIndex() < 0) ? -1 : timeJList.convertIndexToModel(timeJList.getSelectedIndex());
            		processButtonText(loadButton);
                
            	}
            }
        });
        
        zJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
            	

            	if (! isNewFileLoading) {
            		
            	
//                System.out.println("   z valueChanged: firstIndex: "+e.getFirstIndex());
            		if (e.getFirstIndex() < 0  && zJList.getModel().getSize() > 0) {
            			zJList.setSelectedIndex(0);
            		}
            		selectedZIndex = (zJList.getSelectedIndex() < 0) ? -1 : zJList.convertIndexToModel(zJList.getSelectedIndex());
            		processButtonText(loadButton);
                
            	}
            }
        });
        
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);                
            }
        });
        
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                if (rtJList.getSelectedIndices().length > 1 || 
                        timeJList.getSelectedIndices().length > 1 || 
                        zJList.getSelectedIndices().length > 1) {
                
                    animate();
                }
                else {
                    
                    for (LoadDataListener listener : listeners) {
                        System.out.println("grid selection: "+gridJList.getSelectedIndex());
                        System.out.println("  rt selection: "+rtJList.getSelectedIndex());
                        System.out.println("time selection: "+timeJList.getSelectedIndex());
                        System.out.println("   z selection: "+zJList.getSelectedIndex());

                        try {
                        
                            viewer.getGridDatasetRaster().setGridIndex(getSelectedGridIndex());
                            viewer.getGridDatasetRaster().setRuntimeIndex(getSelectedRuntimeIndex());
                            viewer.getGridDatasetRaster().setTimeIndex(getSelectedTimeIndex());
                            viewer.getGridDatasetRaster().setZIndex(getSelectedZIndex());

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        listener.loadData();
                    }
                
                }
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
            }
        });

    }
    
    
    private JXHyperlink getMoreLink() {
    	
    	JXHyperlink moreLink = new JXHyperlink();
    	final Component finalThis = this;
    	
        final JPopupMenu morePopupMenu = new JPopupMenu("Additional Operations");
        JMenuItem titleItem = new JMenuItem("<html><b>Additional Operations</b></html>");
        titleItem.setToolTipText("List of additional operations");
        titleItem.setEnabled(false);
        morePopupMenu.add(titleItem);
        morePopupMenu.addSeparator();
        
        JMenuItem item7 = new JMenuItem("Math Tool");
        item7.setToolTipText("Display Math Tool");
        item7.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
    			RasterMathDialog mathUI = new RasterMathDialog(viewer, finalThis);
            }
        });
        morePopupMenu.add(item7);

        JMenuItem item8 = new JMenuItem("Point Subset Tool (BETA)");
        item8.setToolTipText("Display Point Subset Tool");
        item8.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
    			PointSubsetDialog pointSubsetUI = new PointSubsetDialog(viewer, finalThis);
            }
        });
        morePopupMenu.add(item8);

        moreLink.addMouseListener(new MousePopupListener(moreLink, morePopupMenu));
        
        return moreLink;
    }

    
    private void processButtonText(JButton loadButton) {
        if (rtJList.getSelectedIndices().length > 1 || 
                timeJList.getSelectedIndices().length > 1 || 
                zJList.getSelectedIndices().length > 1) {
        
            loadButton.setText("Animate");
        }
        else {
            loadButton.setText("Load");
        }
    }
    
    private void animate() {
        if (viewer.getCurrentViewType() == WCTViewer.CurrentViewType.GOOGLE_EARTH ||
                viewer.getCurrentViewType() == WCTViewer.CurrentViewType.GOOGLE_EARTH_SPLIT_GEOTOOLS) {
            JOptionPane.showMessageDialog(this, 
                    "Animations are not currently supported in the Google Earth view", 
                    "Animation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (animator == null) {
            boolean isTimeMorphingAvailable = MorphDialog.getInstance(viewer).isMorphingEngaged();
            MorphDialog.getInstance(viewer).setMorphingEngaged(false);
            
            animator = new WCTAnimator(viewer);            

            MorphDialog.getInstance(viewer).setMorphingEngaged(isTimeMorphingAvailable);
        }
        animator.setLocationRelativeTo(this);
        animator.setVisible(true);
        
        
    }
    
    
    private void processGridListSelectionEvent() {
    	if (! isNewFileLoading) {

    		int oldTimeListSize = timeJList.getElementCount();
    		int[] selectedTimeIndices = timeJList.getSelectedIndices();
//    		System.out.println("SELECTED TIMES: "+Arrays.toString(selectedTimeIndices));
//    		System.out.println("OLD TIME LIST SIZE: "+oldTimeListSize);

    		processRuntimeDimension();
    		processTimeDimension();
    		processZDimension();
    		setSelectedGridIndex((gridJList.getSelectedIndex() < 0) ? -1 : gridJList.convertIndexToModel(gridJList.getSelectedIndex()));
    		System.out.println("grid list change event: selected index = "+getSelectedGridIndex());


    		if (timeJList.getElementCount() == oldTimeListSize) {
    			timeJList.setSelectedIndices(selectedTimeIndices);
    		}

//    		System.out.println("NEW TIME LIST SIZE: "+timeJList.getElementCount());

    		accordion.removeTab("Information");

    		if (gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D() == null) {
    			accordion.removeTab("Time");
    		}
    		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D() != null && ! isTabOnAccordion(accordion, "Time")) {
    			accordion.addTab("Time", timePanelParent);
    		}
    		
    		if (gridList.get(selectedGridIndex).getCoordinateSystem().getVerticalAxis() == null) {    			
    			accordion.removeTab("Height");
    		}
    		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getVerticalAxis() != null && ! isTabOnAccordion(accordion, "Height")) {
    			accordion.addTab("Height", zPanelParent);
    		}
    		
    		if (gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis() == null) {
    			accordion.removeTab("Runtime");
    		}
    		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis() != null && ! isTabOnAccordion(accordion, "Runtime")) {
    			accordion.addTab("Runtime", rtPanelParent);
    		}
    		

    		accordion.addTab("Information", overviewPanelScrollPane);
    		
    		validate();
    		repaint();

    	}

    }
    
    

    private static boolean isTabOnAccordion(JSCAccordion accordion, String tabTitle) {
    	boolean isTabFound = false;
    	for (int n=0; n<accordion.getTabCount(); n++) {
    		if (accordion.getTabs().get(n).getText().equals(tabTitle)) {
    			isTabFound = true;
    		}
    	}
    	return isTabFound;
    }
    


    private void readGridStructure() throws DAP2Exception, IOException, WCTException, 
    	IllegalAccessException, InstantiationException, WCTNoGridsFoundException {
    	
    	isNewFileLoading = true;
        
        int lastGridIndex = getSelectedGridIndex();
        int lastZIndex = selectedZIndex;
        int lastRuntimeIndex = selectedRuntimeIndex;
        int lastTimeIndex = selectedTimeIndex;
        
        StringBuilder errlog = new StringBuilder();
        gds = GridDatasetUtils.openGridDataset(gridDatasetURL.toString(), errlog);
        
        this.gridList = gds.getGrids();
        try {
        this.fileTypeDescription = gds.getNetcdfFile().getFileTypeDescription();
        } catch (Exception e) {
        	e.printStackTrace();
        	this.fileTypeDescription = "";
        }
        // build z coordinate cache, since the vert. coordinate system object doesn't
        // store a cache of this data - which is different from the time coord. sys.
        for (int n=0; n<gridList.size(); n++) {
        	GridDatatype grid = gridList.get(n);
        	if (grid.getCoordinateSystem().getVerticalAxis() != null) {
        		double[] zVals = grid.getCoordinateSystem().getVerticalAxis().getCoordValues();
    			zCoordsCacheByVariableIndex.put(n, zVals);
        	}
        }

//        gds.close();
        GridDatasetUtils.scheduleToClose(gds);
        
        if (gridList == null || gridList.size() == 0) {
        	// check for a canceled read, which will produce zero grids
        	if (WCTUtils.getSharedCancelTask().isCancel()) {
        		throw new IOException("Operation canceled");
        	}
            throw new WCTNoGridsFoundException("No Grids Found.  Check for CF-compliance or change specified data type.");
        }
        
        DefaultListModel listModel = new DefaultListModel();
        
        if (gridList.size() == 0) {
            listModel.addElement("No Grids Found.");
            gridJList.setModel(listModel);
            return;
        }
        
        for (GridDatatype grid : gridList) {
            System.out.println("NAME: "+grid.getName());
            if (grid.getTimeDimension() != null) System.out.println("TIME DIM: "+grid.getTimeDimension().toString());
            if (grid.getZDimension() != null) System.out.println("   Z DIM: "+grid.getZDimension().toString());
            System.out.println("   Y DIM: "+grid.getYDimension().toString());
            System.out.println("   X DIM: "+grid.getXDimension().toString());
            

//            varList.addElement(grid.getName()+ " ("+grid.getDescription()+")");
//            listModel.addElement((grid.getDescription().trim().length() == 0) ? grid.getName() : (grid.getName()+" ("+grid.getDescription()+")") );
            String units = (grid.getUnitsString() != null && grid.getUnitsString().length() > 0) ? "["+grid.getUnitsString()+"]" : "";
            listModel.addElement(grid.getName()+"|"+grid.getDescription()+units);
        }        
      
        gridJList.removeListSelectionListener(gridListListener);
        
        System.out.println("view index for grid: "+gridJList.convertIndexToView(lastGridIndex));
        
        filterVariableList();

        if (gridJList.getSelectedIndex() < 0 || gridJList.convertIndexToView(gridJList.getSelectedIndex()) < 0) {
        	jtfVariableFilter.setText("");
        	varFilterString = "";
            filterVariableList();
        }

        
//        if (listModel.getSize() < 8 && listModel.getSize() >= 4) {
//        	gridJList.setVisibleRowCount(listModel.getSize());
//        }
//        else if (listModel.getSize() < 4) {
//        	gridJList.setVisibleRowCount(4);
//        }
        
        gridJList.setModel(listModel);
//        gridJList.setSelectedIndex((lastGridIndex >= 0 && lastGridIndex < gridJList.getElementCount()) ? gridJList.convertIndexToView(lastGridIndex) : 0);
        setSelectedGridIndex((lastGridIndex >= 0 && lastGridIndex < gridJList.getElementCount()) ? gridJList.convertIndexToView(lastGridIndex) : 0);
        rtJList.setSelectedIndex((lastRuntimeIndex >= 0 && lastRuntimeIndex < rtJList.getElementCount()) ? rtJList.convertIndexToView(lastRuntimeIndex) : 0);
        timeJList.setSelectedIndex((lastTimeIndex >= 0 && lastTimeIndex < timeJList.getElementCount()) ? timeJList.convertIndexToView(lastTimeIndex) : 0);
        zJList.setSelectedIndex((lastZIndex >= 0 && lastZIndex < zJList.getElementCount()) ? zJList.convertIndexToView(lastZIndex) : 0);
     
        gridJList.addListSelectionListener(gridListListener);
        
    
        
        processRuntimeDimension();
        processTimeDimension();
        processZDimension();

        
        
        
        overviewPanel.setBackground(Color.WHITE);
        overviewPanel.removeAll();
        List<Attribute> attList = gds.getGlobalAttributes();
        List<String> orderedSpecialList = Arrays.asList(new String[] {
        				"cdr_program",
        				"license",
        				"title",        				
        				"summary",
        				"source",
        				"creator_name",
        				"creator_url",
        				"creator_email",
        				"references",
        				"institution",
        				"platform",
        				"sensor",
        				"Originating_or_generating_Center", 
        				"Originating_or_generating_Subcenter",
        				"Type_of_generating_process"
        		});
        
        for (String attName: orderedSpecialList) {
        	Attribute att = gds.findGlobalAttributeIgnoreCase(attName);
        	if (att != null) {
        		String text = "{"+att.getShortName() + ":bold}: "+att.getStringValue();
        		StyledLabel label = new StyledLabel(text);
        		label.setBackground(Color.WHITE);
        		StyledLabelBuilder.setStyledText(label, text);
        		label.setLineWrap(true);
        		overviewPanel.add(label, "hfill br");
        	}
        }
        overviewPanel.add(new JLabel(" "), "p");
        // print rest of the global attributes
        for (Attribute att : attList) {
        	if (! orderedSpecialList.contains(att.getShortName())) {
        		String text = "{"+att.getShortName() + ":bold}: "+att.getStringValue();
        		StyledLabel label = new StyledLabel(text);
        		label.setBackground(Color.WHITE);
        		StyledLabelBuilder.setStyledText(label, text);
        		label.setLineWrap(true);
        		overviewPanel.add(label, "hfill br");
        	}
        }
        
        
        
        

        accordion.removeTab("Information");

		if (gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D() == null) {
			accordion.removeTab("Time");
		}
		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D() != null && ! isTabOnAccordion(accordion, "Time")) {
			accordion.addTab("Time", timePanelParent);
		}
		
		if (gridList.get(selectedGridIndex).getCoordinateSystem().getVerticalAxis() == null) {    			
			accordion.removeTab("Height");
		}
		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getVerticalAxis() != null && ! isTabOnAccordion(accordion, "Height")) {
			accordion.addTab("Height", zPanelParent);
		}
		
		if (gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis() == null) {
			accordion.removeTab("Runtime");
		}
		else if (gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis() != null && ! isTabOnAccordion(accordion, "Runtime")) {
			accordion.addTab("Runtime", rtPanelParent);
		}
		
		accordion.addTab("Information", new JScrollPane(overviewPanel));
		
		accordion.setSelectedIndex(0);
        
        
        validate();
        pack();
        
        isNewFileLoading = false;

        
        int timeIndex = 0;
        if (viewer.getGridDatasetRaster().getLastProcessedDateTime() != null &&
        		gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D() != null) {
        	
        	timeIndex = gridList.get(selectedGridIndex).getCoordinateSystem().getTimeAxis1D().findTimeIndexFromDate(
        			viewer.getGridDatasetRaster().getLastProcessedDateTime());
        }
        
        int runtimeIndex = 0;
        if (viewer.getGridDatasetRaster().getLastProcessedRuntime() != null &&
        		gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis() != null) {
        	
        	runtimeIndex = gridList.get(selectedGridIndex).getCoordinateSystem().getRunTimeAxis().findTimeIndexFromDate(
        			viewer.getGridDatasetRaster().getLastProcessedRuntime());
        }
              
        // default to first index for first read
        viewer.getGridDatasetRaster().setGridIndex(getSelectedGridIndex());
//        viewer.getGridDatasetRaster().setRuntimeIndex(getSelectedRuntimeIndex());
        viewer.getGridDatasetRaster().setRuntimeIndex(runtimeIndex);
//        viewer.getGridDatasetRaster().setTimeIndex(getSelectedTimeIndex());
        viewer.getGridDatasetRaster().setTimeIndex(timeIndex);
        viewer.getGridDatasetRaster().setZIndex(getSelectedZIndex());

        
        
    }
    
    private void filterVariableList() {

        varFilterString = varFilterString.trim();
        StringBuilder patternString = new StringBuilder();
        patternString.append(".*");
        for (int n=0; n<varFilterString.length(); n++) {
            patternString.append("[").append(varFilterString.substring(n, n+1).toLowerCase());
            patternString.append(varFilterString.substring(n, n+1).toUpperCase()).append("]");
        }
        patternString.append(".*");
//        gridJList.setFilters(new FilterPipeline(new ShuttleSorter(), new PatternFilter(patternString.toString(), 0, 0)));
        gridJList.setFilters(new FilterPipeline(new PatternFilter(patternString.toString(), 0, 0)));

    }
    
    private void filterTimeList() {
        
        timeFilterString = timeFilterString.trim();
        
//        if (true) return;
        
        StringBuilder patternString = new StringBuilder();
        patternString.append(".*");
        for (int n=0; n<timeFilterString.length(); n++) {
            patternString.append("[").append(timeFilterString.substring(n, n+1).toLowerCase());
            patternString.append(timeFilterString.substring(n, n+1).toUpperCase()).append("]");
        }
        patternString.append(".*");
        timeJList.setFilters(new FilterPipeline(new PatternFilter(patternString.toString(), 0, 0)));
    }
    
    private void filterRuntimeList() {
      runtimeFilterString = runtimeFilterString.trim();
      StringBuilder patternString = new StringBuilder();
      patternString.append(".*");
      for (int n=0; n<runtimeFilterString.length(); n++) {
          patternString.append("[").append(runtimeFilterString.substring(n, n+1).toLowerCase());
          patternString.append(runtimeFilterString.substring(n, n+1).toUpperCase()).append("]");
      }
      patternString.append(".*");
      rtJList.setFilters(new FilterPipeline(new PatternFilter(patternString.toString(), 0, 0)));
  }
  
    
    private void processRuntimeDimension() {
        if (gridJList.getSelectedIndex() < 0 || 
                gridJList.convertIndexToModel(gridJList.getSelectedIndex()) >= gridList.size()) {
            gridJList.setSelectedIndex(0);
//            return;
        }
        GridDatatype grid = gridList.get( gridJList.convertIndexToModel( gridJList.getSelectedIndex() ));
        DefaultListModel rtListModel = new DefaultListModel();
        if (grid.getCoordinateSystem().getRunTimeAxis() != null) {
            Date[] dates = grid.getCoordinateSystem().getRunTimeAxis().getTimeDates();
            for (Date date : dates) {
                rtListModel.addElement(sdf.format(date));
            }
        }
        rtJList.setModel(rtListModel);
        if (rtListModel.getSize() > 0) {
            rtPanelParent.add(rtListPanel, "hfill vfill br");
        }
        else {
            rtPanelParent.remove(rtListPanel);
        }
    }



    private void processTimeDimension() {
        int lastSelectedIndex = (selectedTimeIndex < 0) ? 0 : selectedTimeIndex;
        
        if (gridJList.getSelectedIndex() < 0 || 
                gridJList.convertIndexToModel(gridJList.getSelectedIndex()) >= gridList.size()) {
            gridJList.setSelectedIndex(0);
//            return;
        }
        GridDatatype grid = gridList.get( gridJList.convertIndexToModel( gridJList.getSelectedIndex() ));
        DefaultListModel timeListModel = new DefaultListModel();
        if (grid.getCoordinateSystem().hasTimeAxis1D()) {
            Date[] dates = grid.getCoordinateSystem().getTimeAxis1D().getTimeDates();
            for (Date date : dates) {
                String timeString = null;
                if (selectedTimeZoneIndex == 0) {
                    timeString = sdf2.format(date);
                }
                else if (selectedTimeZoneIndex == 1) {
                    timeString = sdf.format(date);
                }
                else {
                    timeString = sdf2.format(date) + " ("+sdf.format(date)+")";    
                }
                timeListModel.addElement(timeString);
            }
        }
        timeJList.setModel(timeListModel);
        timeJList.setSelectedIndex(lastSelectedIndex);
        
        if (timeListModel.getSize() > 0) {
            timePanelParent.add(timeListPanel, "hfill vfill br");
        }
        else {
            timePanelParent.remove(timeListPanel);
        }
        
        filterTimeList();
    }


    private void processZDimension() {
        if (selectedZIndex == -1) {
            selectedZIndex = 0;
        }
        
        int lastSelectedIndex = selectedZIndex;
        
        if (gridJList.getSelectedIndex() < 0 || 
                gridJList.convertIndexToModel(gridJList.getSelectedIndex()) >= gridList.size()) {
            gridJList.setSelectedIndex(0);
//            return;
        }
        int gridIndex = gridJList.convertIndexToModel( gridJList.getSelectedIndex());
        GridDatatype grid = gridList.get(gridIndex);
        DefaultListModel zListModel = new DefaultListModel();
        if (grid.getCoordinateSystem().getVerticalAxis() != null) {
        	try {
//        		double[] zVals = grid.getCoordinateSystem().getVerticalAxis().getCoordValues();
        		double[] zVals = zCoordsCacheByVariableIndex.get(gridIndex);
            	for (double zval : zVals) {
                	zListModel.addElement(fmt03.format(zval) + " "+grid.getCoordinateSystem().getVerticalAxis().getUnitsString());
            	}
        	} catch (Exception e) {
        		System.out.println(e);
        	}
        }
        zJList.setModel(zListModel);
        zJList.setSelectedIndex(lastSelectedIndex);
        if (zListModel.getSize() > 0) {
            zPanelParent.add(zListPanel, "hfill vfill br");
        }
        else {
            zPanelParent.remove(zListPanel);
        }

    }

    
    
    
    
    
    /**
     * Reads the file and populates UI with any grids read from file.
     * @param gridDatasetURL
     * @throws IOException
     * @throws WCTException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws WCTNoGridsFoundException
     */
    public void setGridDatasetURL(URL gridDatasetURL) 
    	throws DAP2Exception, IOException, WCTException, IllegalAccessException, 
    	InstantiationException, WCTNoGridsFoundException {
    	
    	
        this.gridDatasetURL = gridDatasetURL;
        
        readGridStructure();
        pack();
    }

    public URL getGridDatasetURL() {
        return gridDatasetURL;
    }
    
    


    public void addLoadDataListener(LoadDataListener listener) {
        listeners.add(listener);
    }
    public void removeLoadDataListener(LoadDataListener listener) {
        listeners.remove(listener);
    }



    public void setSelectedGridIndex(int selectedGridIndex) {
        if (selectedGridIndex >= gridList.size()) {
        	selectedGridIndex = 0;
        }
        this.selectedGridIndex = selectedGridIndex;
        
        
        gridJList.setSelectedIndex(selectedGridIndex);
        jtfGridDescription.setText(gridList.get(selectedGridIndex).getDescription()+" ("+
        		gridList.get(selectedGridIndex).getUnitsString()+")");
        
        jtfGridDescription.setCaretPosition(0);
        
//        gridPanelParent.setPreferredSize(new Dimension(300, gridListScrollPane.getPreferredSize().height + 26 +
//        		jtfGridDescription.getPreferredSize().height + jtfVariableFilter.getPreferredSize().height));
        
        try {
        	SearchDialog.getInstance(viewer).setAutoFillDataType(viewer.getFileScanner().getLastScanResult().getDataType().toString());
        	SearchDialog.getInstance(viewer).setAutoFillFileFormat(fileTypeDescription);
        	SearchDialog.getInstance(viewer).setAutoFillVariableUnits(gridList.get(selectedGridIndex).getUnitsString());
        	SearchDialog.getInstance(viewer).setAutoFillVariableName(gridList.get(selectedGridIndex).getName());
        	SearchDialog.getInstance(viewer).setAutoFillVariableDesc(gridList.get(selectedGridIndex).getDescription());
        	
		} catch (Exception e) {
//			e.printStackTrace();
		}
        
//        jlGridDescription.setPreferredSize(new Dimension(gridJList.getPreferredSize().width, jlGridDescription.getPreferredSize().height));
//        jtfGridDescription.setPreferredSize(new Dimension(gridJList.getPreferredSize().width, 15));

//        gridPanelParent.setPreferredSize(new Dimension(300, gridPanelParent.getPreferredSize().height+6+jlGridDescription.getPreferredSize().height));
//        gridPanelParent.setPreferredSize(new Dimension(300, gridPanelParent.getPreferredSize().height+6+jlGridDescription.getPreferredSize().height));

        validate();
    }
    public int getSelectedGridIndex() {
        if (selectedGridIndex == -1) {
            selectedGridIndex = 0;
        }
        return selectedGridIndex;
    }
    public void setSelectedTimeIndex(int selectedTimeIndex) {
        this.selectedTimeIndex = selectedTimeIndex;
        timeJList.setSelectedIndex(selectedTimeIndex);        
    }
    public int getSelectedTimeIndex() {
        if (gridList.get(getSelectedGridIndex()).getCoordinateSystem().getTimeAxis1D() == null) {
            return -1;
        }
        return selectedTimeIndex;
    }
    public void setSelectedRuntimeIndex(int selectedRuntimeIndex) {
        this.selectedRuntimeIndex = selectedRuntimeIndex;
        rtJList.setSelectedIndex(selectedRuntimeIndex);        
    }
    public int getSelectedRuntimeIndex() {
        if (gridList.get(getSelectedGridIndex()).getCoordinateSystem().getRunTimeAxis() == null) {
            return -1;
        }
        return selectedRuntimeIndex;
    }
    public void setSelectedZIndex(int selectedZIndex) {
        this.selectedZIndex = selectedZIndex;
        zJList.setSelectedIndex(selectedZIndex);        
    }
    public int getSelectedZIndex() {
        if (gridList.get(getSelectedGridIndex()).getCoordinateSystem().getVerticalAxis() == null) {
            return -1;
        }
        return selectedZIndex;
    }

    
    
    
    
    
    public int[] getSelectedTimeIndices() {
        int[] indices = Arrays.copyOf(timeJList.getSelectedIndices(), timeJList.getSelectedIndices().length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = timeJList.convertIndexToModel(indices[n]);
        }        
        return indices; 
    }
    public void setSelectedTimeIndices(int[] selectedTimeIndices) {
        int[] indices = Arrays.copyOf(selectedTimeIndices, selectedTimeIndices.length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = timeJList.convertIndexToView(indices[n]);
        }        
        timeJList.setSelectedIndices(indices);
    }
    
    public int[] getSelectedRunTimeIndices() {
        int[] indices = Arrays.copyOf(rtJList.getSelectedIndices(), rtJList.getSelectedIndices().length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = rtJList.convertIndexToModel(indices[n]);
        }        
        return indices; 
    }
    public void setSelectedRuntimeIndices(int[] selectedTimeIndices) {
        int[] indices = Arrays.copyOf(selectedTimeIndices, selectedTimeIndices.length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = rtJList.convertIndexToView(indices[n]);
        }        
        rtJList.setSelectedIndices(indices);
    }

    public int[] getSelectedZIndices() {
        int[] indices = Arrays.copyOf(zJList.getSelectedIndices(), zJList.getSelectedIndices().length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = zJList.convertIndexToModel(indices[n]);
        }        
        return indices; 
    }
    public void setSelectedZIndices(int[] selectedTimeIndices) {
        int[] indices = Arrays.copyOf(selectedTimeIndices, selectedTimeIndices.length);
        for (int n=0; n<indices.length; n++) {
            indices[n] = zJList.convertIndexToView(indices[n]);
        }        
        zJList.setSelectedIndices(indices);
    }

    
    
    public void setViewer(WCTViewer viewer) {
        this.viewer = viewer;
    }
    
    
    public List<GridDatatype> getGrids() {
        return gridList;
    }
    
    public JXList getGridJXList() {
        return gridJList;
    }
    
    public JXList getZJXList() {
        return zJList;
    }

    public JXList getRuntimeJXList() {
        return rtJList;
    }

    public JXList getTimeJXList() {
        return timeJList;
    }
    
    public void setMultipleSelectionZ(boolean isMultipleAllowed) {
        zJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void setMultipleSelectionRuntime(boolean isMultipleAllowed) {
        rtJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void setMultipleSelectionTime(boolean isMultipleAllowed) {
        timeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

}
