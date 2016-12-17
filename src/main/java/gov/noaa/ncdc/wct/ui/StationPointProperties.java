package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.OpenFileFilter;
import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.WCTException;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.StreamingProcess;
import gov.noaa.ncdc.wct.decoders.StreamingProcessException;
import gov.noaa.ncdc.wct.export.WCTExportDialog;
import gov.noaa.ncdc.wct.export.WCTExport.ExportFormat;
import gov.noaa.ncdc.wct.export.vector.StreamingCsvExport;
import gov.noaa.ncdc.wct.export.vector.StreamingShapefileExport;
import gov.noaa.ncdc.wct.export.vector.StreamingWKTExport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.filter.BetweenFilter;
import org.geotools.filter.FilterFactory;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.jdesktop.swingx.JXHyperlink;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.ui.PointFeatureDatasetViewer;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import com.eteks.jeks.JeksTable;
import com.eteks.jeks.JeksTableModel;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class StationPointProperties extends JDialog {

	public final static AttributeType[] POINT_ATTRIBUTES = {
		AttributeTypeFactory.newAttributeType("geom", Geometry.class),
		AttributeTypeFactory.newAttributeType("station_id", String.class),
		AttributeTypeFactory.newAttributeType("station_desc", String.class),
		AttributeTypeFactory.newAttributeType("lat", Double.class),
		AttributeTypeFactory.newAttributeType("lon", Double.class),
		AttributeTypeFactory.newAttributeType("alt", Double.class),
		AttributeTypeFactory.newAttributeType("value", Double.class),
		AttributeTypeFactory.newAttributeType("label", String.class)
	};
	private FeatureType schema = null;
	private GeometryFactory geoFactory = new GeometryFactory();


	private final static Color[] PLOT_COLORS = new Color[] {
		Color.BLUE, Color.GREEN.darker(), Color.DARK_GRAY, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.RED,
		Color.BLUE, Color.GREEN.darker(), Color.DARK_GRAY, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.RED,
		Color.BLUE, Color.GREEN.darker(), Color.DARK_GRAY, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.RED,
		Color.BLUE, Color.GREEN.darker(), Color.DARK_GRAY, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.RED,
	};

	private WCTViewer viewer = null;
	private static StationPointProperties dialog = null;
	private FeatureDataset fd = null;
	private StationTimeSeriesFeatureCollection stsfc = null;
	private List<Station> stationList = null;
	private ArrayList<String> plottedVariableList = new ArrayList<String>();
	// private String[] currentVariableUnits = null;

	private JComboBox<String> jcomboVariables = new JComboBox<String>(new String[]{});
	private JComboBox<String> jcomboUniqueDates = new JComboBox<String>();


	private JTabbedPane tabPane = new JTabbedPane();

	private JTextArea textArea = new JTextArea(30, 80);
	private JeksTable jeksTable = new JeksTable() {
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component cell = super.prepareRenderer(renderer, row, column);
			if (row % 2 == 0 && ! isCellSelected(row, column)) {
				cell.setBackground(new Color(235, 235, 250));
			}
			else if (isCellSelected(row, column)){
				cell.setBackground(getSelectionBackground());
			}
			else {
				cell.setBackground(getBackground());
			}
			return cell;
		}
	};
	JButton jbCancel = null;
	private boolean cancel = false;
	private DateFormatter df = new DateFormatter();

	private JComboBox<String> jcomboStationIds = new JComboBox<String>();
	private StationPointProperties(WCTViewer viewer) {
		super(viewer, "Point Timeseries Data");
		this.viewer = viewer;

		init();

		// Show tool tips immediately
		ToolTipManager.sharedInstance().setInitialDelay(0);

		createUI();
		pack();
//		setSize(new Dimension(getSize().width+100, getSize().height));
		setSize(new Dimension(480, Math.min(viewer.getSize().height, 650) ));
		setLocation(viewer.getX()+25, viewer.getY()+25);
	}

	public static StationPointProperties getInstance(WCTViewer viewer) {
		if (dialog == null) {
			dialog = new StationPointProperties(viewer);
		}
		return dialog;
	}



	private void init() {
		try {
			schema = FeatureTypeFactory.newFeatureType(POINT_ATTRIBUTES, "Station/Point Attributes");
		} catch (Exception e) {
			e.printStackTrace();
			javax.swing.JOptionPane.showMessageDialog(this, "Point Feature Init Exception: "+e, 
					"POINT FEATURE INIT EXCEPTION", javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}


	private void createUI() {
		this.setLayout(new RiverLayout());

		//		JTabbedPane dateOrStationTabPane = new JTabbedPane();

		//		JPanel uniqueStationPanel = new JPanel(new RiverLayout());		
		this.add(new JLabel("<html><b>Select Station: </b></html>"));
		this.add(jcomboStationIds, "hfill");

		//		JPanel uniqueDatePanel = new JPanel(new RiverLayout());
		this.add(new JLabel("<html><b> -- OR -- </b></html>"), "br");
		this.add(new JLabel("<html><b>Select Date: </b></html>"), "br");
		this.add(jcomboUniqueDates, "hfill");


		//		dateOrStationTabPane.add(uniqueStationPanel, "Unique Station");
		//		dateOrStationTabPane.add(uniqueDatePanel, "Unique Date");
		//		this.add(dateOrStationTabPane, "hfill");



		JPanel tablePanel = new JPanel(new RiverLayout());
		JPanel textPanel = new JPanel(new RiverLayout());
		textPanel.add(new JScrollPane(textArea), "br hfill vfill");
		tablePanel.add(new JScrollPane(jeksTable), "br hfill vfill");
		tabPane.addTab("Table", tablePanel);
		tabPane.addTab("Text", textPanel);
		// tabPane.addTab("Plot", plotPanel);
		// tabPane.setShowCloseButtonOnTab(true);
		this.add(tabPane, "br hfill vfill");

		final JDialog finalThis = this;
		jcomboStationIds.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadCurrentlySelectedStation();
			}
		});

		jbCancel = new JButton("Cancel");
		jbCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel = true;
			}
		});



		jcomboUniqueDates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				foxtrot.Worker.post(new foxtrot.Job() {
					public Object run() {
						String selectedTime = null;
						try {
							if (jcomboUniqueDates.getSelectedItem() != null) {						

								selectedTime = jcomboUniqueDates.getSelectedItem().toString();
								//								readUniqueTime(selectedTime);	
								readUniqueTime(jcomboUniqueDates.getSelectedIndex());
								//								setViewerExtent(s);
								//								setViewerSelectedStation(s);

							}
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (NoDataException e) {
							if (selectedTime != null) {
								JOptionPane.showMessageDialog(finalThis, "No observations are present for this time: "+selectedTime);
							}
						} catch (InvalidRangeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchElementException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAttributeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return "DONE";
					}
				});
			}
		});

		jcomboVariables.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				FeatureDatasetPoint fdp = null;
				if (fd != null && fd.getFeatureType().isPointFeatureType()) {
					fdp = (FeatureDatasetPoint)fd;
				}
				else {
					WCTUiUtils.showErrorMessage(finalThis, "Data does not appear to be of CDM type 'FeatureDatasetPoint'  ", new IOException());
				}

				//		get unique date list		
				try {
					Variable currentVariable = fdp.getNetcdfFile().findVariable(jcomboVariables.getSelectedItem().toString());
					List<String> uniqueDateList = getUniqueTimeList(((FeatureDatasetPoint)fd), currentVariable);
					System.out.println(currentVariable);
					jcomboUniqueDates.setModel(new DefaultComboBoxModel<String>(uniqueDateList.toArray(new String[uniqueDateList.size()])));
					jcomboUniqueDates.setEnabled(true);

				} catch (NoSharedTimeDimensionException ex) {
					ex.printStackTrace();
					jcomboUniqueDates.setModel(new DefaultComboBoxModel<String>(new String[] { "Not Supported (Requires Shared Time Dimension)" } ));
					jcomboUniqueDates.setEnabled(false);
				} catch (IOException ex) {
					WCTUiUtils.showErrorMessage(finalThis, "Error obtaining unique time list", ex);
				}

			}
		});

		jeksTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					if (jcomboVariables.getSelectedItem() != null) {
						// col '0' is the date/time column 
						if (jeksTable.getSelectedColumn() > 0) {
							jcomboVariables.setSelectedIndex(jeksTable.getSelectedColumn()-1);
						}
					}
				}
			}
		});
		// String[] vars = null;
		//        try {
		//         vars = getStationVariables(jcomboStationIds.getSelectedItem().toString().split(" ")[0]);
		//     } catch (WCTException e1) {
		// e1.printStackTrace();
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }
		//
		// if (vars == null) {
		// JOptionPane.showMessageDialog(this, "Could not list variables for this station", 
		// "Error", JOptionPane.ERROR_MESSAGE);
		// }
		// final JComboBox jcbVariables = new JComboBox(vars);
		final JButton jbPlot = new JButton("New Plot");
		jbPlot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					newPlot();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (WCTException e) {
					e.printStackTrace();
				} catch (PlotException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(jbPlot, e.getMessage());
				}
			}
		});
		final JButton jbAddPlot = new JButton("Add to Plot");
		jbAddPlot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					addToPlot();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (WCTException e) {
					e.printStackTrace();
				} catch (PlotException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					JOptionPane.showMessageDialog(jbPlot, e.getMessage());
				}
			}
		});



		JCheckBox jcbShowMissingColumns = new JCheckBox("Show Missing Fields?");
		JCheckBox jcbLabelOnMap = new JCheckBox("Label on Map?");
		JButton jbColor = new JButton("   ");

//		this.add(jcbShowMissingColumns, "br");
//		this.add(jcbLabelOnMap);
//		this.add(new JLabel("Color: "));
//		this.add(jbColor);






		this.add("p", new JLabel("Plot Timeseries:"));
		this.add(jcomboVariables);
		this.add(jbPlot);
		this.add(jbAddPlot);


		//		JComboBox<String> jcomboUniqueDates = new JComboBox<String>();
		JButton exportDate = new JButton("Export Data");
		exportDate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {	
				try {
					exportData();
				} catch (NoSuchElementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (StreamingProcessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		});
		
		
//		this.add("p", new JLabel("Symbolize Date on Map: "));
//		//		this.add(jcomboUniqueDates);
//		this.add(exportDate);
//		this.add(new JLabel(" (all stations for selected date)"));
//
//		JButton exportTimeSeries = new JButton("Export Time Series");
//		this.add("p", exportTimeSeries);
//		this.add(new JLabel("  [selected station]  "));

		JButton exportButton = new JButton("Export Data (CSV, JSON, SHP)");
		exportButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {

                try {
                    
                    WCTExportDialog wizard = new WCTExportDialog("Data Export Wizard", viewer);
                    wizard.pack();
                    wizard.setLocationRelativeTo(finalThis);
                    wizard.setVisible(true);                    
                
                    viewer.getDataSelector().checkCacheStatus();
                    System.out.println("data export wizard done");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(finalThis, ex.getMessage());
                }
				
			}
		});
		this.add("p", exportButton);






		JButton jbCopy = new JButton("Copy");
		jbCopy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextUtils.getInstance().copyToClipboard(textArea.getText());
			}
		});

		JButton jbPrint = new JButton("Print");
		jbPrint.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//                try {
				//                    TextUtils.getInstance().print("Identification Results", textArea.getText());
				//                } catch (JetException e1) {
				//                    e1.printStackTrace();
				//                }
				try {
					TextUtils.getInstance().print(textArea);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		JButton jbSave = new JButton("Save");
		jbSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					TextUtils.getInstance().save(getContentPane(), textArea.getText(), "txt", "Text File");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});


		JXHyperlink jxlinkLaunchToolsUIView = new JXHyperlink();
		jxlinkLaunchToolsUIView.setText("View in ToolsUI Point Obs Window");
		jxlinkLaunchToolsUIView.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {

				try {

					String prefStore = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
					XMLStore store = ucar.util.prefs.XMLStore.createFromFile(prefStore, null);
					PreferencesExt prefs = store.getPreferences();


					PointFeatureDatasetViewer dv = new PointFeatureDatasetViewer(prefs, new JPanel());
					//    			dv.setDataset((PointObsDataset)tdataset);
					dv.setDataset((FeatureDatasetPoint)fd);


					JDialog dialog = new JDialog(viewer, "Point Obs Panel");
					dialog.add(dv);
					dialog.pack();
					dialog.setVisible(true);

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});



		this.add("hfill right", new JPanel());
		this.add("right", jxlinkLaunchToolsUIView);

//		this.add("br center", jbCopy);
//		this.add(jbPrint);
//		this.add(jbSave);
//		this.add(jbCancel);

		textPanel.add("br center", jbCopy);
		textPanel.add(jbPrint);
		textPanel.add(jbSave);
//		textPanel.add(jbCancel);

		

	}



	private void loadCurrentlySelectedStation() {
		final JDialog finalThis = this;
		
		foxtrot.Worker.post(new foxtrot.Job() {
			public Object run() {
				Station s = null;
				try {
					if (jcomboStationIds.getSelectedItem() != null) {
						String id = jcomboStationIds.getSelectedItem().toString().split("\\(")[0].trim();								
						s = stsfc.getStation(id);

						readStationData(s);								
						setViewerExtentIfNotVisible(viewer.getCurrentExtent(), s);
						setViewerSelectedStation(s);

					}
				} catch (WCTException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (NoDataException e) {
					if (s != null) {
						JOptionPane.showMessageDialog(finalThis, "No observations are present for this station: "+
								s.getName()+" ("+s.getDescription()+")");
					}
				}
				return "DONE";
			}
		});
	}


	
	public void exportData() throws NoSuchElementException, StreamingProcessException, IOException {

		// Set up File Chooser
		JFileChooser fc = new JFileChooser(WCTProperties.getWCTProperty("jne_export_dir"));

//		fc.setAcceptAllFileFilterUsed(true);
		OpenFileFilter shpFilter = new OpenFileFilter("shp", true, "'.shp' ESRI Shapefile");
//		OpenFileFilter gmlFilter = new OpenFileFilter("gml", true, "'.gml' GML (Geographic Markup Language)");
		OpenFileFilter wktFilter = new OpenFileFilter("txt", true, "'.txt' WKT (Well Known Text)");
		OpenFileFilter csvFilter = new OpenFileFilter("csv", true, "'.csv' CSV (Comma Delimited)");
		fc.addChoosableFileFilter(csvFilter);
//		fc.addChoosableFileFilter(gmlFilter);
		fc.addChoosableFileFilter(wktFilter);
		fc.addChoosableFileFilter(shpFilter);
		fc.setFileFilter(shpFilter);

		String extension;
		ExportFormat exportFormat;

		int returnVal = fc.showSaveDialog(this);
		File file = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			int choice = JOptionPane.YES_OPTION;
			// intialize to YES!
			file = fc.getSelectedFile();

			WCTProperties.setWCTProperty("jne_export_dir", file.toString());
			
			// Check the typed or selected file for an extension
			if (fc.getFileFilter() == shpFilter || file.toString().endsWith(".shp")) {
				extension = ".shp";
				exportFormat = ExportFormat.SHAPEFILE;
			}
			else if (fc.getFileFilter() == wktFilter || file.toString().endsWith(".txt")) {
				extension = ".txt";
				exportFormat = ExportFormat.WKT;
			}
			else if (fc.getFileFilter() == csvFilter || file.toString().endsWith(".csv")) {
				extension = ".csv";
				exportFormat = ExportFormat.CSV;
			}
			else {
				JOptionPane.showMessageDialog(null, (Object) ("The Export File must be a shapefile (.shp), " +
						"well-known text (.txt) or comma-separated text (.csv):\n"+file),
						"Data Export Problem", JOptionPane.ERROR_MESSAGE);
				return;
			}
			

			String fstr = file.toString();
			if (! fstr.endsWith(extension)) {
				file = new File(fstr+extension);
			}			

			// Check for existing file
			if (file.exists()) {
				String message = "The Export File \n" +
						"<html><font color=red>" + file + "</font></html>\n" +
						"already exists.\n\n" +
						"Do you want to proceed and OVERWRITE?";
				choice = JOptionPane.showConfirmDialog(this, (Object) message,
						"OVERWRITE PROJECT FILE", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
				fstr = file.toString();
				file = new File(fstr.substring(0, (int) fstr.length() - 4));
			}

			if (choice == JOptionPane.YES_OPTION) {
				exportData(file, exportFormat);
				

				JOptionPane.showMessageDialog(this, (Object) ("Data Export Complete:\n"+file),
						"Data Export Complete", JOptionPane.INFORMATION_MESSAGE);
			}
			// END if(choice == YES_OPTION)
		}
	}

	private void exportData(File outFile, ExportFormat exportFormat) throws NoSuchElementException, StreamingProcessException, IOException {


		StreamingProcess exportProcess = null;
		if (exportFormat == ExportFormat.CSV) {
			exportProcess = new StreamingCsvExport(outFile);
		}
		else if (exportFormat == ExportFormat.SHAPEFILE) {
			exportProcess = new StreamingShapefileExport(outFile);
		}
		else if (exportFormat == ExportFormat.WKT) {
			exportProcess = new StreamingWKTExport(outFile);
		}
		

		FeatureCollection fc = viewer.getStationPointFeatures();
		FeatureIterator iter = fc.features();
		while (iter.hasNext()) {
			exportProcess.addFeature(iter.next());
		}
		
		exportProcess.close();
	}







	public void newPlot() throws IOException, WCTException, PlotException {

		Station station = stationList.get(jcomboStationIds.getSelectedIndex());
		// this.currentVariableUnits = getStationUnits(station);
		PlotRequestInfo plotRequestInfo = new PlotRequestInfo();
		plotRequestInfo.setVariables(new String[] { jcomboVariables.getSelectedItem().toString() });
		plotRequestInfo.setUnits(getStationUnits(station, plotRequestInfo.getVariables()));
		PlotPanel plotPanel = new PlotPanel();
		plotPanel.setPlot(stsfc, station, plotRequestInfo);
		for (int n=0; n<tabPane.getTabCount(); n++) {
			if (tabPane.getTitleAt(n).equalsIgnoreCase("Plot")) {
				tabPane.removeTabAt(n);	
			}
		}
		tabPane.addTab("Plot", plotPanel);
		tabPane.setSelectedComponent(plotPanel);
	}

	public void addToPlot() throws IOException, WCTException, PlotException {

		PlotRequestInfo plotRequestInfo = new PlotRequestInfo();
		Station station = stationList.get(jcomboStationIds.getSelectedIndex());
		// this.currentVariableUnits = getStationUnits(station);
		if (tabPane.getSelectedIndex() == 0) {
			newPlot();
			return;
		}

		// PlotPanel plotPanel = (PlotPanel)(tabPane.getTabComponentAt(tabPane.getSelectedIndex()));
		PlotPanel plotPanel = (PlotPanel)(tabPane.getSelectedComponent());

		System.out.println(tabPane.getTabCount());
		System.out.println(tabPane.getSelectedIndex());
		System.out.println(plotPanel.getClass());
		System.out.println(plotPanel.getPlotRequestInfo());
		String[] vars = plotPanel.getPlotRequestInfo().getVariables();
		ArrayList<String> varList = new ArrayList<String>(Arrays.asList(vars));
		String newVar = jcomboVariables.getSelectedItem().toString();
		if (! varList.contains(newVar)) {
			varList.add(newVar);
		}
		plotRequestInfo.setVariables(varList.toArray(new String[varList.size()]));
		// plotRequestInfo.setUnits(currentVariableUnits);
		plotRequestInfo.setUnits(getStationUnits(station, plotRequestInfo.getVariables()));
		System.out.println("previous variables to plot: "+Arrays.toString(vars));
		System.out.println(" current variables to plot: "+varList);
		plotPanel.setPlot(stsfc, station, plotRequestInfo);
		for (int n=0; n<tabPane.getTabCount(); n++) {
			if (tabPane.getTitleAt(n).equalsIgnoreCase("Plot")) {
				tabPane.removeTabAt(n);	
			}
		}
		tabPane.addTab("Plot", plotPanel);
		tabPane.setSelectedComponent(plotPanel);
	}

	/**
	 * Load a file/URL as a Station dataset.
	 * @param dataURL
	 * @throws IOException
	 * @throws WCTException
	 * @throws NoSharedTimeDimensionException 
	 */
	public void process(URL dataURL) throws IOException, WCTException, NoSharedTimeDimensionException {
		textArea.setText("");
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		textArea.setForeground(Color.GRAY);
		textArea.setCaretPosition(0);

		Formatter fmter = new Formatter();
		FeatureDataset fd = FeatureDatasetFactoryManager.open(null, dataURL.toString(), WCTUtils.getSharedCancelTask(), fmter);
		if (fd != null && fd.getFeatureType().isPointFeatureType()) {

		}
		else {
			System.out.println(fmter.toString());
			throw new IOException("Data does not appear to be of CDM type 'FeatureDatasetPoint' - "+fmter.toString());
		}
		process(fd);
	}

	public void process(FeatureDataset fd) throws IOException, WCTException, NoSharedTimeDimensionException {
		this.fd = fd;

		FeatureDatasetPoint fdp = null;
		if (fd != null && fd.getFeatureType().isPointFeatureType()) {
			fdp = (FeatureDatasetPoint)fd;
		}
		else {
			throw new IOException("Data does not appear to be of CDM type 'FeatureDatasetPoint'  ");
		}


		System.out.println("STATIONS: "+fdp.getPointFeatureCollectionList().size());

		List<ucar.nc2.ft.FeatureCollection> pfcList = fdp.getPointFeatureCollectionList();
		this.stsfc = (StationTimeSeriesFeatureCollection)(pfcList.get(0));
		stationList = stsfc.getStations();

		jcomboStationIds.removeAllItems();

		String[] stationListingArray = new String[stationList.size()];
		for (int n=0; n<stationList.size(); n++) {
			// StationTimeSeriesFeature sf = stsfc.getStationFeature(station);

			//			 station.getName() == attribute of 'station_id', 
			//			 station.getDescription() == attribute of 'station_description'


			//			System.out.println("Station: "+stationList.get(n).toString());
			//			jcomboStationIds.addItem(getStationListingString(stationList.get(n)));
			stationListingArray[n] = getStationListingString(stationList.get(n));

		}
		jcomboStationIds.setModel(new DefaultComboBoxModel<String>(stationListingArray));
		// init variables
		jcomboVariables.setModel(new DefaultComboBoxModel<String>(getStationPlottableVariables(stationList.get(0))));
		// this.currentVariableUnits = getStationUnits(stationList.get(0));





		//        AttributeTypeFactory.newAttributeType("geom", Geometry.class),
		//        AttributeTypeFactory.newAttributeType("station_id", String.class),
		//        AttributeTypeFactory.newAttributeType("station_desc", String.class),
		//        AttributeTypeFactory.newAttributeType("lat", Double.class),
		//        AttributeTypeFactory.newAttributeType("lon", Double.class),
		//        AttributeTypeFactory.newAttributeType("alt", Double.class)


		// update on map
		FeatureCollection viewerStationPointFeatures = viewer.getStationPointFeatures();
		viewerStationPointFeatures.clear();
		viewer.clearData();

		viewer.getStationPointMapLayer().setStyle(getDefaultStyle());
		
		ArrayList<Feature> tmpList = new ArrayList<Feature>(stationList.size());
		tmpList.ensureCapacity(stationList.size());

		System.out.println("tmpList.size(): "+tmpList.size()+" stationList.size():"+stationList.size());

		int geoIndex = 0;
		for (Station station : stationList) {

			try {

				Feature feature = schema.create(new Object[] {
						geoFactory.createPoint(new Coordinate(station.getLongitude(), station.getLatitude())),
						station.getName(), 
						station.getDescription(), 
						new Double(station.getLatitude()), new Double(station.getLongitude()),
						new Double(station.getAltitude()), new Double(-1.0), ""
				}, new Integer(geoIndex++).toString());

				//			viewerStationPointFeatures.add(feature);
				tmpList.add(feature);

				//			System.out.println(feature);

			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error adding station: "+station.getName()+" ("+station.getDescription()+")");
				return;
			}
		}

		viewerStationPointFeatures.addAll(tmpList);




		//		get unique date list		
		try {
			Variable currentVariable = fdp.getNetcdfFile().findVariable(jcomboVariables.getSelectedItem().toString());
			List<String> uniqueDateList = getUniqueTimeList(((FeatureDatasetPoint)fd), currentVariable);
			System.out.println(currentVariable);
			jcomboUniqueDates.setModel(new DefaultComboBoxModel<String>(uniqueDateList.toArray(new String[uniqueDateList.size()])));
			jcomboUniqueDates.setEnabled(true);

		} catch (NoSharedTimeDimensionException e) {
			e.printStackTrace();
			jcomboUniqueDates.setModel(new DefaultComboBoxModel<String>(new String[] { "Not Supported (Requires Shared Time Dimension)" } ));
			jcomboUniqueDates.setEnabled(false);
		}


		if (stationList.size() == 1) {
			loadCurrentlySelectedStation();
		}
	}


	private void getTimeSlice(Variable var, int timeIndex) {
		//		var.getDi
	}



	/**
	 * 
	 * @param fdp
	 * @param var
	 * @return
	 * @throws NoSharedTimeDimensionException
	 * @throws IOException 
	 */
	public static List<String> getUniqueTimeList(FeatureDatasetPoint fdp, Variable var) throws NoSharedTimeDimensionException, IOException {
		Attribute coordAtt = var.findAttribute("coordinates");
		if (coordAtt == null) {
			throw new NoSharedTimeDimensionException(
					"The coordinates attribute was not found for the data variable "+var.getShortName()+"  \n" +
							"Only station data structures with the one-dimensional time dimension can be quickly \n" +
							"sliced at a specific time across all stations.  Please refer to the Type #1 of \n" +
					"Discrete Sampling Geometries in the CF-documentation.");
		}
		System.out.println(coordAtt.getStringValue());
		String[] coordinateVariableArray = coordAtt.getStringValue().split(" ");


		// check:
		// 1) for each coordinate variable listed, which one has standard_name='time'?
		// 2) for time coordinate variable, is it one-dimensional?
		// 3) perhaps also check that the same dimension is used in the data variable and coord variable (probably named 'time')

		for (String coordVarName : coordinateVariableArray) {
			Variable coordVariable = fdp.getNetcdfFile().findVariable(coordVarName);
			if (coordVariable.findAttribute("standard_name").getStringValue().equals("time")) {
				System.out.println("time coord. variable is: "+coordVariable.getFullName());
				if (coordVariable.getRank() != 1) {
					throw new NoSharedTimeDimensionException(
							"The time coordinate variable was found, but is not one-dimensional.  \n\n" +
									"Only station data structures with the one-dimensional time dimension can be quickly \n" +
									"sliced at a specific time across all stations.  Please refer to the Type #1 of \n" +
							"Discrete Sampling Geometries in the CF-documentation.");
				}
				System.out.println("  and it is only-one dimensional!  proceed...");

				if (var.findDimensionIndex(coordVariable.getDimension(0).getShortName()) < 0) {
					throw new NoSharedTimeDimensionException(
							"The time coordinate variable was found, the dimensions of the data variable and time \n" +
									"coordinate variable do not match.  \n\n" +
									"Only station data structures with the one-dimensional time dimension can be quickly \n" +
									"sliced at a specific time across all stations.  Please refer to the Type #1 of \n" +
							"Discrete Sampling Geometries in the CF-documentation.");
				}

				System.out.println("found matching dimension too!");

				CalendarDateUnit cdu = CalendarDateUnit.of(null, coordVariable.findAttribute("units").getStringValue());
				double[] data = (double[])coordVariable.read().get1DJavaArray(Double.class);
				ArrayList<String> uniqueDates = new ArrayList<String>();
				uniqueDates.ensureCapacity(data.length);

				for (double val : data) {
					//					System.out.println(val);
					CalendarDate cd = cdu.makeCalendarDate(val);
					//					System.out.println(cd);
					uniqueDates.add(cd.toString());
				}				

				return uniqueDates;
			}
		}

		// if we get to this, throw exception
		throw new NoSharedTimeDimensionException("No shared time dimension could be found.  \n" +
				"Only station data structures with the shared time dimension can be quickly \n" +
				"sliced at a specific time across all stations.  Please refer to the Type #1 of \n" +
				"Discrete Sampling Geometries in the CF-documentation.");

	}

	static class NoSharedTimeDimensionException extends Exception {	
		public NoSharedTimeDimensionException(String message) {
			super(message);
		}
	}
	class NoDataException extends Exception {
		public NoDataException(String message) {
			super(message);
		}
	}



	private String getStationListingString(Station station) {
		return station.getName() + " ("+station.getDescription()+")";
	}

	public String[] getStationVariables(String id) throws WCTException, IOException {
		return getStationVariables(stsfc.getStation(id));
	}
	public String[] getStationVariables(Station station) throws WCTException, IOException {
		if (this.stsfc == null) {
			throw new WCTException("No data file has been loaded");
		}
		StationTimeSeriesFeature sf = stsfc.getStationFeature(station);
		PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
		if (pfIter.hasNext()) {
			PointFeature pf = pfIter.next();
			StructureData sdata = pf.getData();
			StructureMembers smembers = sdata.getStructureMembers();
			List<String> nameList = smembers.getMemberNames();
			return nameList.toArray(new String[nameList.size()]);

			//			String[] varArray = nameList.toArray(new String[nameList.size()]);
			//			for (int n=0; n<varArray.length; n++) {
			//				varArray[n] = varArray[n].concat(" "+smembers.getMember(n).getDescription());
			//			}
			//			
			//			return varArray;
		}
		else {
			throw new WCTException("No variables found for this station");
		}
	}
	
	public String[] getStationPlottableVariables(Station station) throws WCTException, IOException {
		if (this.stsfc == null) {
			throw new WCTException("No data file has been loaded");
		}
		StationTimeSeriesFeature sf = stsfc.getStationFeature(station);
		PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
		if (pfIter.hasNext()) {
			PointFeature pf = pfIter.next();
			StructureData sdata = pf.getData();
			StructureMembers smembers = sdata.getStructureMembers();
			List<String> nameList = smembers.getMemberNames();
			
			ArrayList<String> plottableList = new ArrayList<String>();
			for (int n=0; n<nameList.size(); n++) {

				if ((smembers.getMember(n).getDataType() == DataType.BYTE ||
						smembers.getMember(n).getDataType() == DataType.DOUBLE ||
						smembers.getMember(n).getDataType() == DataType.FLOAT ||
						smembers.getMember(n).getDataType() == DataType.INT ||
						smembers.getMember(n).getDataType() == DataType.LONG ||
						smembers.getMember(n).getDataType() == DataType.SHORT) ) {
					
					plottableList.add(nameList.get(n));
				}
			}
			return plottableList.toArray(new String[plottableList.size()]);

			//			String[] varArray = nameList.toArray(new String[nameList.size()]);
			//			for (int n=0; n<varArray.length; n++) {
			//				varArray[n] = varArray[n].concat(" "+smembers.getMember(n).getDescription());
			//			}
			//			
			//			return varArray;
		}
		else {
			throw new WCTException("No variables found for this station");
		}
	}
	
	
	public String[] getStationUnits(String id, String[] vars) throws WCTException, IOException {
		return getStationUnits(stsfc.getStation(id), vars);
	}
	public String[] getStationUnits(Station station, String[] vars) throws WCTException, IOException {
		if (this.stsfc == null) {
			throw new WCTException("No data file has been loaded");
		}
		StationTimeSeriesFeature sf = stsfc.getStationFeature(station);
		PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
		if (pfIter.hasNext()) {
			PointFeature pf = pfIter.next();
			StructureData sdata = pf.getData();
			StructureMembers smembers = sdata.getStructureMembers();
			List<Member> memberList = smembers.getMembers();
			System.out.println(memberList);
			System.out.println(smembers.getMemberNames());
			String[] unitsArray = new String[vars.length];
			for (int i=0; i<vars.length; i++) {
				for (int n=0; n<memberList.size(); n++) {
					if (memberList.get(n).getName().equals(vars[i])) {
						unitsArray[i] = memberList.get(n).getUnitsString();
						if (unitsArray[i] == null) {
							unitsArray[i] = "No Units";
						}
					}
				}
			}
			return unitsArray;
		}
		else {
			throw new WCTException("No variable units found for this station");
		}
	}





	public void setViewerExtentIfNotVisible(Rectangle2D currentExtent, Station s) {
		double lat = s.getLatitude();
		double lon = s.getLongitude();
		
		if (! currentExtent.contains(lon, lat)) {
			setViewerExtent(s);
		}
	}
	
	
	
	public void setViewerExtent(Station s) {
		double lat = s.getLatitude();
		double lon = s.getLongitude();
		double extentBuffer = 2.0;
		viewer.setCurrentExtent(new Rectangle.Double(lon-extentBuffer, lat-extentBuffer, extentBuffer*2, extentBuffer*2));
	}

	public void setViewerSelectedStation(Station station) {

		try {

			viewer.getStationPointSelectedFeatures().clear();

			Feature feature = schema.create(new Object[] {
					geoFactory.createPoint(new Coordinate(station.getLongitude(), station.getLatitude())),
					station.getName(), 
					station.getDescription(), 
					new Double(station.getLatitude()), new Double(station.getLongitude()),
					new Double(station.getAltitude()), new Double(-1), 
					station.getName() + " ("+station.getDescription()+")"
			}, new Integer(0).toString());

			viewer.getStationPointSelectedFeatures().add(feature);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error adding station selection: "+station.getName()+" ("+station.getDescription()+")");
			return;
		}

	}

	/**
	 * Limited to 2 deg search box around selected location
	 * @param fromLocation
	 */
	public void selectClosestStation(Point2D fromLocation) {
		try {

			double searchRange = 1.0;
			LatLonRect latlonRect = new LatLonRect(
					new LatLonPointImpl(fromLocation.getY()-searchRange, fromLocation.getX()-searchRange),
					new LatLonPointImpl(fromLocation.getY()+searchRange, fromLocation.getX()+searchRange));

			System.out.print("before search based station subset...");
			List<Station> stations = this.stsfc.getStations(latlonRect);
			System.out.println("   done.");

			Station closestStation = null;
			double closestDistance = 10000000;
			int index = 0;
			for (Station s : stations) {
				double dist = fromLocation.distance(s.getLongitude(), s.getLatitude());
				if (dist < closestDistance) {
					closestStation = s;
					closestDistance = dist;
				}
				index++;
			}

			//			int stationIndex = this.stsfc.getStations().indexOf(closestID);
			//			Station selectedStation = this.stsfc.getStation(closestID);

			//			jcomboStationIds.setSelectedIndex(closestIndex);

			System.out.println("fromLocation: "+fromLocation);
			//			System.out.println("stationList: "+stationList);
			System.out.println("closestStation: "+closestStation);
			//			System.out.println("stationIndex: "+stationIndex);
			//			System.out.println("selectedStation: "+selectedStation);

			jcomboStationIds.setSelectedItem(getStationListingString(closestStation));


		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error selecting closest station: "+fromLocation);
			return;
		}
	}



	public static Style getDefaultStyle() {
		StyleBuilder sb = new StyleBuilder();
        Mark plmark = sb.createMark(StyleBuilder.MARK_CIRCLE, Color.GREEN.darker().darker().darker(), Color.GREEN, .5);
        Graphic plgr = sb.createGraphic(null, plmark, null);
        PointSymbolizer plps = sb.createPointSymbolizer(plgr);
        
        return sb.createStyle(plps);
	}

	public void readUniqueTime(String time) throws IOException, NoDataException {

		PointFeatureCollection pfc = this.stsfc.flatten(null, new CalendarDateRange(
				CalendarDate.parseISOformat(null, jcomboUniqueDates.getSelectedItem().toString()), 100));

		PointFeatureIterator pfIter = pfc.getPointFeatureIterator(1*1024*1024);
		processFeatureIterator(pfIter);
	}

	public void readUniqueTime(int timeIndex) throws IOException, NoDataException, InvalidRangeException, NoSuchElementException, IllegalAttributeException {

		viewer.getStationPointSelectedFeatures().clear();

		List<Variable> varList = ((FeatureDatasetPoint)fd).getNetcdfFile().getVariables();

		// 1. find coord. variable that has standard_name='time'
		// 2. then find the dimension  used in that coord. variable
		// 3. then select the variables that use this dimension
		// 4. subset those variables based on the time index
		List<String> dimNameList = new ArrayList<String>();
		for (Variable var : varList) {
			Attribute att = var.findAttribute("standard_name");
			if (att != null && att.getStringValue().equals("time")) {
				System.out.println("found a time variable named: "+var.getShortName());
				if (var.getRank() != 1) {
					throw new NoDataException("time coord. variable is not 1-D");
				}
				dimNameList.add(var.getDimension(0).getShortName());
			}
		}

		if (dimNameList.size() == 0) {
			throw new NoDataException("no time dimension found");
		}
		// just get variables with time
		ArrayList<Variable> varListWithTime = new ArrayList<Variable>();
		varListWithTime.ensureCapacity(varList.size());
		for (Variable var : varList) {
			for (String dimName : dimNameList) {
				if (var.findDimensionIndex(dimName) >= 0) {
					varListWithTime.add(var);
				}
			}
		}

		// get that data slice!
		//		for (Variable var : varListWithTime) {
		//			int dimIdx = var.findDimensionIndex(dimName);
		//			double[] stationValues = (double[])(var.slice(dimIdx, timeIndex).read().get1DJavaArray(Double.class));
		//			System.out.println(var.getShortName()+"  size="+stationValues.length);
		//		}
		
		System.out.println(dimNameList.toString());
		System.out.println(varListWithTime.toString());
		
		

		System.out.println("reading data slice...");
		int dimIdx = -1;
		Variable var = ((FeatureDatasetPoint)fd).getNetcdfFile().findVariable(jcomboVariables.getSelectedItem().toString());
		for (String dimName : dimNameList) {
			if (var.findDimensionIndex(dimName) >= 0) {
				dimIdx = var.findDimensionIndex(dimName);
			}
		}

		double[] stationValues = (double[])(var.slice(dimIdx, timeIndex).read().get1DJavaArray(Double.class));

		Attribute missingValueAtt = var.findAttribute("missing_value");
		Attribute fillValueAtt = var.findAttribute("_FillValue");

		System.out.println("setting feature attribute with value...");
		FeatureCollection viewerStationPointFeatures = viewer.getStationPointFeatures();
		FeatureIterator iter = viewerStationPointFeatures.features();
		int idx = 0;
		while (iter.hasNext()) {
			Feature f = iter.next();
			f.setAttribute("value", stationValues[idx]);

			if (Double.isNaN(stationValues[idx])) {
				f.setAttribute("label", "");
				idx++;
				continue;				
			}

			if (missingValueAtt != null && stationValues[idx] == missingValueAtt.getNumericValue().doubleValue()) {
				f.setAttribute("label", "");
				idx++;
				continue;
			}

			if (fillValueAtt != null && stationValues[idx] == fillValueAtt.getNumericValue().doubleValue()) {
				f.setAttribute("label", "");
				idx++;
				continue;
			}

			f.setAttribute("label", WCTUtils.DECFMT_0D0.format(stationValues[idx++]));
		}

		System.out.println("setting map layer style");
		double minVal = Double.MAX_VALUE;
		double maxVal = Double.MIN_VALUE;
		for (double v : stationValues) {
			if (v > -9999) {
				minVal = Math.min(minVal, v);
				maxVal = Math.max(maxVal, v);
			}
		}

		viewer.getStationPointMapLayer().setStyle(getStyle(minVal, maxVal));

		System.out.println("done");
	}

	public void readStationData(String id) throws WCTException, IOException, NoDataException {
		readStationData(stsfc.getStation(id));
	}
	public void readStationData(Station station) throws WCTException, IOException, NoDataException {
		if (this.stsfc == null) {
			throw new WCTException("No data file has been loaded");
		}

		// for (Station station : stationList) {
		StationTimeSeriesFeature sf = stsfc.getStationFeature(station);

		System.out.println("Station: "+station.toString());
		System.out.println("Location: "+sf.getLatLon());
		textArea.append("## Station: "+station.toString()+" -- "+sf.getLatLon() +" \n");

		PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
		processFeatureIterator(pfIter);
	}



	private void processFeatureIterator(PointFeatureIterator pfIter) throws NoDataException, IOException {
		JeksTableModel jeksTableModel = getTableModel();
		int c = 1;

		cancel = false;

		int row = 0;
		String[] columnNames = null;
		String[] columnInfo = null;
		boolean firstTime = true;
		StringBuilder sb = new StringBuilder();

		textArea.setText("");
		
		// iterate through data for each station
		while (pfIter.hasNext()) {
			PointFeature pf = pfIter.next();

			// System.out.println( pf.getObservationTimeAsDate() + " -- " + pf.getLocation().toString());
			StructureData sdata = pf.getDataAll();
			StructureMembers smembers = sdata.getStructureMembers();
			// System.out.println( smembers.getMemberNames().toString() );
			

			if (firstTime) {
				List<String> nameList = smembers.getMemberNames();
				columnNames = new String[nameList.size()+1];
				columnInfo = new String[nameList.size()+1];
				Class[] colTypes = new Class[nameList.size()];
				//                2010-03-23T12:34:00Z
				textArea.append("#datetime           ");
				columnNames[0] = "Date/Time";
				columnInfo[0] = "<html>Date/Time<br>Time Zone: UTC</html>";
				for (int n=0; n<nameList.size(); n++) {
					columnNames[n+1] = nameList.get(n);
					Member member = smembers.getMember(n);
					textArea.append(", "+member.getName());
					columnInfo[n+1] = "<html>"+member.getName()+"<br>"+
							member.getDescription()+"<br>"+
							member.getUnitsString()+"</html>";
					DataType dt = member.getDataType();
					System.out.println(dt);
				}
				textArea.append("\n");
				// jeksTable.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("Date");
				// Force the header to resize and repaint itself
				// jeksTable.getTableHeader().resizeAndRepaint();

				firstTime = false;
			}

			

			
//			textArea.append(stsfc.getTimeUnit().makeStandardDateString( pf.getObservationTime() ));
			sb.append(stsfc.getTimeUnit().makeStandardDateString( pf.getObservationTime() ));
			jeksTableModel.setValueAt(stsfc.getTimeUnit().makeStandardDateString( pf.getObservationTime() ), row, 0);
			c = 1;
			for (String col : smembers.getMemberNames()) {
				String data = sdata.getScalarObject(col).toString();
				jeksTableModel.setValueAt(data, row, c++);
				//				 System.out.print(col+"["+c+"]="+data+" ");
				// ---- textArea.append(","+data + getPad(col, data));
				
				

//				textArea.append(", "+data);
				sb.append(", "+data);
			}
//			textArea.append("\n");
			sb.append("\n");
			
			
			if (cancel) {
				return;
			}


			row++;

			if (row % 100 == 0) {
				System.out.println(".... processed "+row+" rows");
				textArea.append(sb.toString());
				sb.setLength(0);
			}
		}

		// add remaining text
		textArea.append(sb.toString());
		sb.setLength(0);

		// only format if there is not a excess of data
		if (row < 10000) {
			formatTextArea();
		}
		textArea.setForeground(Color.BLACK);

		
		
		
		if (columnNames == null) {
			throw new NoDataException("No Data Found");
		}
//		textArea.append(sb.toString());

		// System.out.println("before tablemodel copy");
		JeksTableModel jeksTableModel2 = new JeksTableModel(row, c, columnNames);
		for (int i=0; i<c; i++) {
			for (int j=0; j<row; j++) {
				jeksTableModel2.setValueAt(jeksTableModel.getValueAt(j, i), j, i);
			}
		}
		// System.out.println("after tablemodel copy");

		jeksTable.setModel(jeksTableModel2);
		jeksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// System.out.println("after setTableModel");
		autoFitColumns(jeksTable, columnNames);

		// textArea.append("\nDONE PROCESSING: "+station.getName());
		ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
		for (int i = 0; i < jeksTable.getColumnCount(); i++) {
			TableColumn col = jeksTable.getColumnModel().getColumn(i);
			tips.setToolTip(col, columnInfo[i]);
			col.setHeaderValue(columnNames[i]);
		}
		jeksTable.getTableHeader().addMouseMotionListener(tips);
	}


	private static void autoFitColumns(JTable jeksTable, String[] columnNames) {

		if (columnNames == null) {
			return;
		}

		int rowSampleSize = 6;
		int rows = jeksTable.getRowCount();
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D)image.getGraphics();
		FontRenderContext fc = g.getFontRenderContext();

		for (int c=0; c<jeksTable.getColumnCount(); c++) {
			int maxWidth = -1;
			rowSampleSize = Math.min(rowSampleSize, rows);

			for (int r=0; r<rowSampleSize; r++) {
				int row = (int)(Math.random()*rows);
//				System.out.println("checking random row: "+row+" for column "+c);
				try {
					String value = jeksTable.getValueAt(r, c).toString();
					int width = (int) jeksTable.getFont().
							createGlyphVector(fc, value).getVisualBounds().getWidth();
					maxWidth = Math.max(width, maxWidth);
				} catch (Exception e) {
					System.err.println("row: "+row+" of "+rows+" , col: "+c+" of "+jeksTable.getColumnCount()+" :: error: "+e.getMessage());
					//					r--;
				}
			}
			int width = (int) jeksTable.getFont().
					createGlyphVector(fc, columnNames[c]).getVisualBounds().getWidth();
			maxWidth = Math.max(width, maxWidth);

			jeksTable.getColumnModel().getColumn(c).setPreferredWidth(maxWidth+15);
		}
	}
	private String getPad(String col, String data) {
		if (col.length() < 10) {
			StringBuilder sb = new StringBuilder();
			sb.append(col);
			for (int n=col.length(); n<10; n++) {
				sb.append(" ");
			}
			col = sb.toString();
		}
		StringBuilder sb = new StringBuilder();
		for (int n=data.length(); n<col.length()+1; n++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	private String getPad(String str, int padToLength) {
		StringBuilder sb = new StringBuilder();
		sb.append(str);
		for (int n=str.length(); n<padToLength; n++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	private void formatTextArea() {
		int[] colLengths = null;
		String[] lines = textArea.getText().split("\n");
		for (int n=0; n<lines.length; n++) {
			if (lines[n].startsWith("##")) {
				continue;
			}
			// System.out.println(lines[n]);

			String[] cols = lines[n].split(",");
			if (colLengths == null) {
				colLengths = new int[cols.length];
			}
			if (colLengths.length != cols.length) {
				colLengths = new int[cols.length];
			}
			for (int i=0; i<cols.length; i++) {
				colLengths[i] = Math.max(colLengths[i], cols[i].length());
			}
		}
		// System.out.println(Arrays.toString(colLengths));

		StringBuilder sb = new StringBuilder();
		for (int n=0; n<lines.length; n++) {
			if (lines[n].startsWith("##")) {
				sb.append(lines[n]).append("\n");
				continue;
			}
			String[] cols = lines[n].split(",");
			for (int i=0; i<cols.length; i++) {
				sb.append(getPad(cols[i], colLengths[i]));
				if (i < cols.length-1) {
					sb.append(",");
				}
			}
			sb.append("\n");
		}
		textArea.setText(sb.toString());
	}
	private JeksTableModel getTableModel() {
		JeksTableModel jeksTableModel = new JeksTableModel() {
		};
		return jeksTableModel;
	}



	public Style getStyle(double minVal, double maxVal) {
		StyleBuilder sb = new StyleBuilder();
		Color[] color = new Color[] {
				new Color(  50, 50, 50, 100),
				Color.BLUE,
				Color.CYAN,
				Color.GREEN.brighter(),
				Color.GREEN,
				Color.GREEN.darker(),
				Color.YELLOW,
				Color.ORANGE,
				Color.RED,
				Color.RED.darker().darker(),
				new Color(155, 50, 50)
		};
		int alpha = 100;
		double interval = (maxVal-minVal)/color.length;

		Rule rules[] = new Rule[color.length];

		Rule labelRules[] = new Rule[color.length];

		Style style = sb.createStyle();
		try {
			BetweenFilter filters[] = new BetweenFilter[color.length];
			FilterFactory ffi = FilterFactory.createFilterFactory();

			for (int i = 0; i < color.length; i++) {

				filters[i] = ffi.createBetweenFilter();

				Mark plmark = sb.createMark(StyleBuilder.MARK_CIRCLE, color[i], color[i].darker().darker().darker(), .5);
				plmark.getFill().setOpacity(sb.literalExpression(color[i].getAlpha()/255.0));
				plmark.getStroke().setOpacity(sb.literalExpression(color[i].getAlpha()/255.0));

				Graphic plgr = sb.createGraphic(null, plmark, null);
				PointSymbolizer plps = sb.createPointSymbolizer(plgr);

				rules[i] = sb.createRule(plps);


				filters[i].addLeftValue(sb.literalExpression(minVal+(i*interval) ));
				filters[i].addRightValue(sb.literalExpression(minVal+((i+1)*interval) ));
				filters[i].addMiddleValue(ffi.createAttributeExpression(schema, "value"));

				System.out.println(filters[i]);

				rules[i].setFilter(filters[i]);

				style.addFeatureTypeStyle(sb.createFeatureTypeStyle(null, rules[i]));





				//              org.geotools.styling.Font gtFont = BaseMapManager.GT_FONT_ARRAY[0];
				//              DeclutterType declutterType = DeclutterType.FULL;
				//              double minScaleDenom = 150.0;
				//              double maxScaleDenom = BaseMapStyleInfo.NO_MAX_SCALE;
				//              
				//          	String attName = "label";
				//        	String[] attNameArray = attName.split(",");
				//
				//
				//        	
				//        	double rotationAsDeclutterFlag = 0;
				//        	if (declutterType == DeclutterType.NONE) {
				//        		rotationAsDeclutterFlag = 720;
				//        	}
				//        	else if (declutterType == DeclutterType.SIMPLE) {
				//        		rotationAsDeclutterFlag = 360;
				//        	}
				//        	
				//    		double yDisplacment = -5;
				//    		Symbolizer[] symbArray = new Symbolizer[attNameArray.length];
				//        	for (int n=0; n<attNameArray.length; n++) {
				//        		TextSymbolizer ts = sb.createTextSymbolizer(color[i], gtFont, attNameArray[n]);
				//        		ts.setHalo(sb.createHalo(color[0], .7, 2.2));
				//        		ts.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 5.0, yDisplacment, rotationAsDeclutterFlag));
				//        		yDisplacment += 10;    		
				//        		symbArray[n] = ts;
				//        	}
				//        	
				//
				//			labelRules[i] = sb.createRule(symbArray);	            
				//            labelRules[i].setFilter(filters[i]);
				//            
				//            FeatureTypeStyle fts = sb.createFeatureTypeStyle(symbArray, minScaleDenom, maxScaleDenom);
				//            fts.addRule(labelRules[i]);
				//    		style.addFeatureTypeStyle(fts);


			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return style;      
	}

	/*
	   public Style getNexradPolygonStyle(NexradHeader header) {
	         // Create Filters and Style for NEXRAD Polygons!
	         Color[] color = NexradColorFactory.getColors(header.getProductCode());

	         Rule rules[] = new Rule[color.length];
	         Style nexradStyle = sb.createStyle();
	         try {
	            BetweenFilter filters[] = new BetweenFilter[color.length];
	            FilterFactory ffi = FilterFactory.createFilterFactory();

	            for (int i = 0; i < color.length; i++) {

	               filters[i] = ffi.createBetweenFilter();
	               PolygonSymbolizer polysymb = sb.createPolygonSymbolizer(color[i], color[i], 1);
	               polysymb.getFill().setOpacity(sb.literalExpression(nexradAlphaChannelValue/255.0));
	               polysymb.getStroke().setOpacity(sb.literalExpression(nexradAlphaChannelValue/255.0));
	               rules[i] = sb.createRule(polysymb);

	               filters[i].addLeftValue(sb.literalExpression(i));
	               filters[i].addRightValue(sb.literalExpression(i + 1));
	               filters[i].addMiddleValue(ffi.createAttributeExpression(nexradSchema, "colorIndex"));
	               rules[i].setFilter(filters[i]);

	               nexradStyle.addFeatureTypeStyle(sb.createFeatureTypeStyle(null, rules[i]));
	            }
	         } catch (Exception e) {
	            e.printStackTrace();
	         }

	         return nexradStyle;      
	   }

	 */























	class ColumnHeaderToolTips extends MouseMotionAdapter {
		TableColumn curCol;
		Map<TableColumn, String> tips = new HashMap<TableColumn, String>();
		public void setToolTip(TableColumn col, String tooltip) {
			if (tooltip == null) {
				tips.remove(col);
			} else {
				tips.put(col, tooltip);
			}
		}
		public void mouseMoved(MouseEvent evt) {
			JTableHeader header = (JTableHeader) evt.getSource();
			JTable table = header.getTable();
			TableColumnModel colModel = table.getColumnModel();
			int vColIndex = colModel.getColumnIndexAtX(evt.getX());
			TableColumn col = null;
			if (vColIndex >= 0) {
				col = colModel.getColumn(vColIndex);
			}
			if (col != curCol) {
				header.setToolTipText((String) tips.get(col));
				curCol = col;
			}
		}
	}




	private static class PlotRequestInfo {
		enum PlotType { LINE, BAR, AREA };
		private String[] variables;
		private String[] units;
		private PlotType[] plotTypes;
		private Color[] plotColors;
		public void setVariables(String[] variables) {
			this.variables = variables;
		}
		public String[] getVariables() {
			return variables;
		}
		public void setPlotTypes(PlotType[] plotTypes) {
			this.plotTypes = plotTypes;
		}
		public PlotType[] getPlotTypes() {
			return plotTypes;
		}
		public void setPlotColors(Color[] plotColors) {
			this.plotColors = plotColors;
		}
		public Color[] getPlotColors() {
			return plotColors;
		}
		public void setUnits(String[] units) {
			this.units = units;
		}
		public String[] getUnits() {
			return units;
		}
	}
	class PlotPanel extends JPanel {
		private PlotRequestInfo plotRequestInfo;

		public void setPlot(StationTimeSeriesFeatureCollection stsfc, 
				Station station, PlotRequestInfo plotRequestInfo) 
						throws PlotException, IOException {


			this.plotRequestInfo = plotRequestInfo;
			ArrayList<String> uniqueUnitsList = new ArrayList<String>();
			for (String u : plotRequestInfo.getUnits()) {
				if (u != null && ! uniqueUnitsList.contains(u)) {
					uniqueUnitsList.add(u);
				}
			}

			String[] variables = plotRequestInfo.getVariables();
			String[] units = plotRequestInfo.getUnits();
			TimeSeriesCollection[] dataset = new TimeSeriesCollection[variables.length];
			for (int n=0; n<dataset.length; n++) {
				dataset[n] = new TimeSeriesCollection(TimeZone.getTimeZone("GMT"));
			}
			//			for (int n=0; n<variables.length; n++) {
			//				if (units[n].equals(uniqueUnitsList.get(0))) {
			//					TimeSeries varSeries = new TimeSeries(variables[n], FixedMillisecond.class);
			//
			//					String[] plotInfo = addToTimeSeries(
			//							station, 
			//							variables[n], varSeries);
			//
			//					dataset[0].addSeries(varSeries);
			//				}
			//			}
			{
				TimeSeries varSeries = new TimeSeries(variables[0], FixedMillisecond.class);
				String[] plotInfo = addToTimeSeries(
						station, 
						variables[0], varSeries);
				dataset[0].addSeries(varSeries);
			}


			String graphTitle = "Plot";
			//    String graphTitle = plotInfo[0];
			String domainTitle = "Date/Time (GMT)";


			JFreeChart chart = ChartFactory.createTimeSeriesChart(graphTitle,// chart title
					domainTitle, // x axis label
					uniqueUnitsList.get(0), // y axis label
					dataset[0], // data
					// PlotOrientation.VERTICAL,
					true, // include legend
					true, // tooltips
					false // urls
					);

			// Set the time zone for the date axis labels
			((DateAxis)chart.getXYPlot().getDomainAxis(0)).setTimeZone(TimeZone.getTimeZone("GMT"));

			// chart.setBackgroundImage(javax.imageio.ImageIO.read(new
			// URL("http://mesohigh/img/noaaseagullbkg.jpg")));

			// NOW DO SOME OPTIONAL CUSTOMIZATION OF THE CHART...
			chart.setBackgroundPaint(Color.white);

			//        LegendTitle legend = (LegendTitle) chart.getLegend();
			//        legend.setDisplaySeriesShapes(true);

			// get a reference to the plot for further customization...
			XYPlot plot = chart.getXYPlot();
			//			for (int n=1; n<uniqueUnitsList.size(); n++) {
			//
			//				for (int i=0; i<variables.length; i++) {
			//
			//					if (units[i].equals(uniqueUnitsList.get(n))) {
			//						TimeSeries varSeries = new TimeSeries(variables[i], FixedMillisecond.class);
			//
			//						String[] plotInfo = addToTimeSeries(
			//								station, 
			//								variables[n], varSeries);
			//
			//						dataset[n].addSeries(varSeries);
			//
			//						// AXIS 2
			//						NumberAxis axis2 = new NumberAxis(uniqueUnitsList.get(n));
			//						axis2.setFixedDimension(10.0);
			//						axis2.setAutoRangeIncludesZero(false);
			//						axis2.setLabelPaint(PLOT_COLORS[n-1]);
			//						axis2.setTickLabelPaint(PLOT_COLORS[n-1]);
			//						plot.setRangeAxis(n, axis2);
			//						plot.setRangeAxisLocation(n, AxisLocation.BOTTOM_OR_LEFT);
			//
			//						plot.setDataset(n, dataset[n]);
			//						plot.mapDatasetToRangeAxis(n, n);
			//
			//						XYItemRenderer renderer2 = new StandardXYItemRenderer();
			//						//        XYBarRenderer renderer2 = new XYBarRenderer();
			//						renderer2.setSeriesPaint(0, PLOT_COLORS[n-1]);
			//						plot.setRenderer(n, renderer2);
			//
			//
			//					}
			//				}



			for (int n=0; n<uniqueUnitsList.size(); n++) {

				if (n > 0) {
					NumberAxis axis2 = new NumberAxis(uniqueUnitsList.get(n));
					axis2.setFixedDimension(10.0);
					axis2.setAutoRangeIncludesZero(false);
					axis2.setLabelPaint(PLOT_COLORS[n]);
					axis2.setTickLabelPaint(PLOT_COLORS[n]);
					plot.setRangeAxis(n, axis2);
					plot.setRangeAxisLocation(n, AxisLocation.BOTTOM_OR_LEFT);
				}

				for (int i=1; i<variables.length; i++) {

					if (units[i].equals(uniqueUnitsList.get(n))) {
						TimeSeries varSeries = new TimeSeries(variables[i], FixedMillisecond.class);

						String[] plotInfo = addToTimeSeries(
								station, 
								variables[i], varSeries);

						dataset[i].addSeries(varSeries);

					}
					plot.setDataset(i, dataset[i]);
					plot.mapDatasetToRangeAxis(i, n);

					XYItemRenderer renderer2 = new StandardXYItemRenderer();
					//        XYBarRenderer renderer2 = new XYBarRenderer();
					renderer2.setSeriesPaint(0, PLOT_COLORS[n]);
					plot.setRenderer(i, renderer2);

				}


			}



			ChartPanel chartPanel = new ChartPanel(chart);
			// JPanel plotPanel = new JPanel(new RiverLayout());
			// plotPanel.add(chartPanel, "hfill");
			this.removeAll();
			this.setLayout(new BorderLayout());
			this.add(chartPanel, BorderLayout.CENTER);
		}
		/**
		 * 
		 * @param station
		 * @param variable
		 * @param varSeries
		 * @return [0]=station name and description if available
		 *   [1]=units
		 * @throws IOException
		 */
		public String[] addToTimeSeries(Station station, String variable, TimeSeries varSeries) throws IOException, PlotException {
			// Station station = stsfc.getStation(stationName);
			String[] plotInfo = new String[3];
			plotInfo[0] = station.getName();
			if (station.getDescription() != null &&
					station.getDescription().trim().length() > 0) {
				plotInfo[0] += " ("+station.getDescription()+")";
			}
			StationTimeSeriesFeature sf = stsfc.getStationFeature(station);

			System.out.println("Station: "+station.toString());
			System.out.println("Location: "+sf.getLatLon());
			textArea.append("## Station: "+station.toString()+" -- "+sf.getLatLon() +" \n");

			boolean firstTime = true;
			PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
			// iterate through data for each station
			while (pfIter.hasNext()) {
				PointFeature pf = pfIter.next();

				// System.out.println( pf.getObservationTimeAsDate() + " -- " + pf.getLocation().toString());
				StructureData sdata = pf.getData();
				StructureMembers smembers = sdata.getStructureMembers();
				// System.out.println( smembers.getMemberNames().toString() );
				if (firstTime) {
					plotInfo[1] = smembers.getMember(smembers.getMemberNames().indexOf(variable)).getUnitsString();
					firstTime = false;
				}

				try {

					Date date = pf.getObservationTimeAsDate();
					double val = Double.parseDouble(sdata.getScalarObject(variable).toString());
					if (val < -100) {
						val = Double.NaN;
					}
					varSeries.addOrUpdate(new FixedMillisecond(date.getTime()), val);

				} catch (NumberFormatException nfe) {
					throw new PlotException("Plot Error: Only numeric fields are supported.");
				}

			}
			return plotInfo;
		}
		public PlotRequestInfo getPlotRequestInfo() {
			return plotRequestInfo;
		}
	}

	class PlotException extends Exception {
		public PlotException(String msg) {
			super(msg);
		}
	}
}

