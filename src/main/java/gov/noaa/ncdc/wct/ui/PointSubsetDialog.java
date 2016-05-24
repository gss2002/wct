package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.common.RoundedBorder;
import gov.noaa.ncdc.wct.WCTConstants;
import gov.noaa.ncdc.wct.WCTFilter;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.DecodeException;
import gov.noaa.ncdc.wct.decoders.DecodeHintNotSupportedException;
import gov.noaa.ncdc.wct.event.DataExportEvent;
import gov.noaa.ncdc.wct.event.DataExportListener;
import gov.noaa.ncdc.wct.export.WCTExport;
import gov.noaa.ncdc.wct.export.WCTExport.ExportFormat;
import gov.noaa.ncdc.wct.export.WCTExportException;
import gov.noaa.ncdc.wct.export.WCTExportNoDataException;
import gov.noaa.ncdc.wct.export.raster.FeatureRasterizerException;
import gov.noaa.ncdc.wct.export.raster.WCTMathOp;
import gov.noaa.ncdc.wct.export.raster.WCTRaster;
import gov.noaa.ncdc.wct.io.SupportedDataType;
import gov.noaa.ncdc.wct.io.WCTDataSourceDB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.jdesktop.swingx.JXDialog;
import org.jdesktop.swingx.border.DropShadowBorder;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.units.DateFormatter;

import com.jidesoft.hints.FileIntelliHints;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.SelectAllUtils;
import com.vividsolutions.jts.geom.Coordinate;


public class PointSubsetDialog extends JXDialog {

	private WCTViewer viewer;
	
	public final static File SUBSET_HISTORY_FILE = new File(WCTConstants.getInstance().getCacheLocation()+File.separator+"objdata"+File.separator+"point-subset-history.txt");
	
	private JButton exportButton, cancelButton, editLegendButton;

	private JTextPane legendTitleTextArea = new JTextPane();
	
	private JProgressBar progressBar = new JProgressBar();
	
	private int processedCountInLoop = 0;
	private boolean shouldCancel = false;
	private boolean isOperationAcrossDimensions = false;
	private SupportedDataType dataType;
	

	private JList<String> markerList = new JList<String>();
	private ArrayList<Feature> markerFeatureList = new ArrayList<Feature>();
//	private JTextArea resultsTextArea = new JTextArea(6, 40);
	WCTTextPanel wctTextPanel = new WCTTextPanel("", false);
	
	private WCTRaster processRaster = null;
	private DateFormatter df = new DateFormatter();
	

    private final WCTTextDialog errorLogDialog = new WCTTextDialog(this, "", "Export Error Log: ", false);
    
    
	/**
	 * Show the WCT Math Dialog
	 * @param viewer
	 */
	public PointSubsetDialog(WCTViewer viewer) {
		this(viewer, viewer);
	}
	
	public PointSubsetDialog(WCTViewer viewer, Component parent) {
		super(viewer, new JPanel());
		
		this.viewer = viewer;
		
        setModal(true);
        setTitle("Point Subset Tool (BETA)");
        
        createGUI();
        
        pack();
        setLocation(parent.getX()+45, parent.getY()+45);
        setVisible(true);
	}
	
	
	
	
	private void createGUI() {
		

		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		progressBar.setString("Processing Progress");
   
        
        
        


		try {


//			JPanel mainPanel = new JPanel(new RiverLayout());
			//        mainPanel.setBorder(WCTUiUtils.myTitledTopBorder("Math Tool (for Grid and Radial data)", 5, 5, 5, 5, TitledBorder.CENTER, TitledBorder.TOP));





//			System.out.println(Arrays.toString(viewer.getDataSelector().getSelectedURLs()));
//			System.out.println(Arrays.toString(viewer.getGridProps().getSelectedRunTimeIndices()));
//			System.out.println(Arrays.toString(viewer.getGridProps().getSelectedTimeIndices()));
//			System.out.println(Arrays.toString(viewer.getGridProps().getSelectedZIndices()));
			
			
			boolean dataAvailable = false;
			if (viewer.getDataSelector().getSelectedIndices().length > 0 &&
					viewer.getGridProps() != null && 
					viewer.getFileScanner().getLastScanResult().getDataType() == SupportedDataType.GRIDDED &&
					(viewer.getGridProps().getSelectedRunTimeIndices().length > 1 ||
							viewer.getGridProps().getSelectedTimeIndices().length > 1 ||
							viewer.getGridProps().getSelectedZIndices().length > 1)
			) {
				dataAvailable = true;
				isOperationAcrossDimensions = true;
			}
			else if (viewer.getDataSelector().getSelectedIndices().length > 0) {
				dataAvailable = true;
			}



			JPanel generalOptionsPanel = getGeneralOptionsPanel();
			if (dataAvailable) {
//				this.getContentPane().setLayout(new BorderLayout());
//				this.getContentPane().add(mainPanel, BorderLayout.CENTER);
//				this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				this.getContentPane().add(generalOptionsPanel);
			}
			else {
//				mainPanel.add(getNoFilesSelectedPanel(), "hfill");
				this.getContentPane().add(getNoFilesSelectedPanel());
			}

			
			




		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e, "General Error", JOptionPane.ERROR_MESSAGE);
		}

	}

	
	private JPanel getGeneralOptionsPanel() throws ClassNotFoundException, IllegalAttributeException, IOException, FactoryConfigurationError, SchemaException {
		
//		JPanel panel = new JPanel(new RiverLayout(0, 3));
		JPanel panel = new JPanel(new BorderLayout());
		
//		panel.setBorder(WCTUiUtils.myTitledTopBorder("Point Subset Tool Options", 5, 5, 5, 5, TitledBorder.CENTER, TitledBorder.TOP));

		
//		FeatureCollection markerFc = viewer.getMarkerFeatures();
		FeatureCollection markerFc = MarkerEditor.loadMarkerObjectData(
				MarkerEditor.MARKER_OBJECT_FILE, FeatureCollections.newCollection());
		
		DefaultListModel<String> markerInfoListModel = new DefaultListModel<String>();
		markerFeatureList.ensureCapacity(markerFc.size());
		FeatureIterator iter = markerFc.features();
		int cnt = 0;
		while (iter.hasNext()) {
			Feature f = iter.next();
			System.out.println(f);
			markerInfoListModel.addElement(f.getAttribute("label1").toString()+","+
					f.getAttribute("label2").toString()+","+
					f.getAttribute("label3").toString()
				);
			markerFeatureList.add(f);
		}
		markerList.setModel(markerInfoListModel);
		markerList.setVisibleRowCount(6);
		markerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (cnt > 0) {
			markerList.setSelectedIndex(0);
		}

		
		
		
		
		JButton goButton = new JButton("Start");
		goButton.setActionCommand("SUBMIT");
		goButton.setPreferredSize(new Dimension(60, (int)goButton.getPreferredSize().getHeight()));
		goButton.addActionListener(new SubmitListener(this));
		JButton clearButton = new JButton("Clear");
		clearButton.setPreferredSize(new Dimension(60, (int)clearButton.getPreferredSize().getHeight()));
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
//				resultsTextArea.setText("");
				wctTextPanel.getTextArea().setText("");
				try {
					FileUtils.writeStringToFile(SUBSET_HISTORY_FILE, wctTextPanel.getTextArea().getText());
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}				
		});
		exportButton = new JButton("Export");
		exportButton.setActionCommand("EXPORT");
		exportButton.setPreferredSize(new Dimension(60, (int)exportButton.getPreferredSize().getHeight()));
		exportButton.addActionListener(new SubmitListener(this));
		JButton copyButton = new JButton("Copy");
		copyButton.setActionCommand("COPY");
		copyButton.setPreferredSize(new Dimension(60, (int)exportButton.getPreferredSize().getHeight()));
		copyButton.addActionListener(new SubmitListener(this));
		cancelButton = new JButton("Close");
		cancelButton.setActionCommand("CLOSE");
		cancelButton.setPreferredSize(new Dimension(60, (int)cancelButton.getPreferredSize().getHeight()));
		cancelButton.addActionListener(new SubmitListener(this));
		
		JPanel buttonPanel = new JPanel(new RiverLayout());
		buttonPanel.add("p hfill", progressBar);
		buttonPanel.add("p center", goButton);
		buttonPanel.add(copyButton);
		buttonPanel.add(exportButton);
		buttonPanel.add(clearButton);
		buttonPanel.add(cancelButton);

		

        
//		panel.add("p", new JLabel("Select Marker Features: "));
//		panel.add("br hfill", new JScrollPane(markerList));
		
//		panel.add("p", new JLabel("Point Subset Results"));
//		panel.add(new JScrollPane(resultsTextArea), "br hfill vfill");
//		panel.add(wctTextPanel, "br hfill vfill");
		
		JScrollPane markerListScrollPane = new JScrollPane(markerList);
		markerListScrollPane.setBorder(WCTUiUtils.myTitledTopBorder("Select Marker Features: ", 22, 2, 2, 2, TitledBorder.LEFT, TitledBorder.BELOW_TOP));
		
		wctTextPanel.setPreferredSize(new Dimension(580, 300));
		wctTextPanel.setBorder(WCTUiUtils.myTitledTopBorder("Point Subset Results: ", 6, 2, 8, 2, TitledBorder.LEFT, TitledBorder.BELOW_TOP));	
		populateFromHistoryInBackground();
		
		JPanel markerPanel = new JPanel(new BorderLayout());
		markerPanel.add(new JLabel("<html><b>Select Marker Features:</b></html>"), BorderLayout.NORTH);
		markerPanel.add(markerListScrollPane, BorderLayout.CENTER);
		markerPanel.setBorder(WCTUiUtils.myBorder(5, 5, 5, 5));
		
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsPanel.add(new JLabel("<html><b>Point Subset Results:</b></html>"), BorderLayout.NORTH);
		resultsPanel.add(wctTextPanel, BorderLayout.CENTER);
		resultsPanel.setBorder(WCTUiUtils.myBorder(5, 5, 5, 5));
		
//		panel.add(markerListScrollPane, BorderLayout.NORTH);
//		panel.add(wctTextPanel, BorderLayout.CENTER);
		
//		panel.add(markerPanel, BorderLayout.NORTH);
//		panel.add(resultsPanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, markerPanel, resultsPanel);
		panel.add(splitPane, BorderLayout.CENTER);
		
		
		
		
		
//		textAreaPanel.setBorder(BorderFactory.createLineBorder(new Color(162, 181, 205)));
	

		DropShadowBorder dropShadowBorder = new DropShadowBorder(Color.BLACK, 8, 0.8f, 5, false, false, true, true);
		Border border2 = BorderFactory.createCompoundBorder(dropShadowBorder, BorderFactory.createEmptyBorder(2, 2, 0, 0));
		Border mainBorder = BorderFactory.createCompoundBorder(border2, new RoundedBorder(new Color(10, 36, 106, 150), 2, 2));
		markerListScrollPane.setBorder(mainBorder);
		wctTextPanel.setBorder(mainBorder);
		
		
//		textAreaPanel.setPreferredSize(new Dimension(legendTitleTextArea.getPreferredSize().width+50, 170));
//		panel.add(new JLabel("Legend Title:"), "p");
//		panel.add(textAreaPanel, "br hfill vfill");
		
		
		return panel;
	}
	
	
	
	private JPanel getNoFilesSelectedPanel() {
		
		JPanel panel = new JPanel(new RiverLayout());
		
		panel.setBorder(WCTUiUtils.myTitledTopBorder("General Math Tool Options", 5, 5, 5, 5, TitledBorder.CENTER, TitledBorder.TOP));

        
		panel.add("p", new JLabel("<html><center>The Math Tool is only available<br>" +
				"for multiple files or grid slices.</center></html>"));		
		
		
		return panel;
	}
	
	
	private void populateFromHistoryInBackground() {
		
		try {
			foxtrot.Worker.post(new foxtrot.Task() {
			    public Object run() {

					// save to cache
					try {
						if (SUBSET_HISTORY_FILE.exists()) {
							wctTextPanel.getTextArea().setText(FileUtils.readFileToString(SUBSET_HISTORY_FILE));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
			    	
			    	return "DONE";
			    }
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	private void process() throws WCTExportNoDataException, WCTExportException, DecodeException, FeatureRasterizerException, 
		IllegalAttributeException, InvalidRangeException, DecodeHintNotSupportedException, 
		URISyntaxException, ParseException, Exception {
		
		
		

		final int[] selectedIndices = viewer.getDataSelector().getSelectedIndices();
		URL[] selectedURLs = viewer.getDataSelector().getSelectedURLs();
		
		
		
		final WCTExport exporter = new WCTExport();
		
		exporter.setExportGridSize(2);
		
//		exporter.setForceResample(true);
		exporter.setOutputFormat(WCTExport.ExportFormat.WCT_RASTER_OBJECT_ONLY);
		if (markerFeatureList.size() == 0) {
			throw new Exception("-- No Marker Features Exist -- \n"
					+ "Please use the marker tool or editor to add locations for point extractions. ");
		}
		
		final int[] selectedMarkerIndices = markerList.getSelectedIndices();
		
		
		// Add progress listener on exporter
		exporter.addDataExportListener(new DataExportListener() {
			@Override
			public void exportStarted(DataExportEvent event) {
			}
			@Override
			public void exportEnded(DataExportEvent event) {
			}
			@Override
			public void exportProgress(DataExportEvent event) {
				int numToProcess;
					if (isOperationAcrossDimensions) {
						numToProcess = getDimensionNumSlicesToProcess()*selectedMarkerIndices.length;
					}
					else {
						numToProcess = selectedIndices.length*selectedMarkerIndices.length;
					}
					double progress = WCTUtils.progressCalculator(
							new int[] { getProcessedCount(), event.getProgress() }, 
							new int[] { numToProcess, 100 } );
					
		
				
				progressBar.setValue((int)(progress*100));
				if (shouldCancel) {
					progressBar.setString("Canceling...");
				}
				else {
					if (isOperationAcrossDimensions) {
						progressBar.setString("Processing Grid ("+getProcessedCount()+" of "+numToProcess+")");
					}
					else {
//						progressBar.setString(event.getStatus());
						progressBar.setString("Processing File ("+getProcessedCount()+" of "+numToProcess+")");
					}
				}
			}

			@Override
			public void exportStatus(DataExportEvent event) {
			}
			
		});
		
		
		
		
		
		
		shouldCancel = false;
		
		
		
		

		for (int selectedMarkerIndex : selectedMarkerIndices) {
			
			Feature f = markerFeatureList.get(selectedMarkerIndex);

			if (isOperationAcrossDimensions) {
				int gridIndex = viewer.getGridProps().getSelectedGridIndex();
				int[] rtIndices = viewer.getGridProps().getSelectedRunTimeIndices();
				int[] tIndices = viewer.getGridProps().getSelectedTimeIndices();
				int[] zIndices = viewer.getGridProps().getSelectedZIndices();

				System.out.println(Arrays.toString(rtIndices));
				System.out.println(Arrays.toString(tIndices));
				System.out.println(Arrays.toString(zIndices));

				if (rtIndices.length == 0) {
					rtIndices = new int[] { -1 };
				}
				if (tIndices.length == 0) {
					tIndices = new int[] { -1 };
				}
				if (zIndices.length == 0) {
					zIndices = new int[] { -1 };
				}


				exporter.setGridExportGridIndex(gridIndex);
				for (int rt=0; rt<rtIndices.length; rt++) {
					if (rtIndices[rt] >= 0) {
						exporter.setGridExportRuntimeIndex(rtIndices[rt]);
					}
					for (int t=0; t<tIndices.length; t++) {
						if (tIndices[t] >= 0) {
							exporter.setGridExportTimeIndex(tIndices[t]);
						}
						for (int z=0; z<zIndices.length; z++) {
							if (zIndices[z] >= 0) {
								exporter.setGridExportZIndex(zIndices[z]);
							}

							if (shouldCancel) {
								rt = rtIndices.length;
								t = tIndices.length;
								z = zIndices.length;
								continue;				
							}


							processRaster = processLayer(exporter, processRaster, selectedURLs[0], f, processedCountInLoop);
							processedCountInLoop++;

						}
					}
				}


			}
			else {
				for (int n=0; n<selectedIndices.length; n++) {

					if (shouldCancel) {
						n = selectedIndices.length;
						continue;				
					}

					// this may not apply if data isn't gridded, but it won't hurt
					if (viewer.getGridProps() != null) {
						exporter.setGridExportGridIndex(viewer.getGridProps().getSelectedGridIndex());
						exporter.setGridExportRuntimeIndex(viewer.getGridProps().getSelectedRuntimeIndex());
						exporter.setGridExportZIndex(viewer.getGridProps().getSelectedZIndex());
						exporter.setGridExportTimeIndex(viewer.getGridProps().getSelectedTimeIndex());
					}


					processRaster = processLayer(exporter, processRaster, selectedURLs[n], f, processedCountInLoop);
					processedCountInLoop++;
				}
			}
		
		}
		
		processedCountInLoop = 0;
		progressBar.setValue(0);
		progressBar.setString("");
		
		
		// save to cache
		FileUtils.writeStringToFile(SUBSET_HISTORY_FILE, wctTextPanel.getTextArea().getText());
        viewer.getDataSelector().checkCacheStatus();

		
//		GridCoverage gc = getGridCoverageFromProcessedRaster(exporter, processRaster);
////		System.out.println(gc.getEnvelope());
//		
//		viewer.fireRenderCompleteEvent();
//
//		viewer.getMapPaneZoomChange().setGridSatelliteActive(false);
//		viewer.getMapPaneZoomChange().setRadarActive(false);
//		if (dataType == SupportedDataType.NEXRAD_LEVEL3 ||
//				dataType == SupportedDataType.NEXRAD_LEVEL3_NWS ||
//				dataType == SupportedDataType.RADIAL) {
//			
//			viewer.setRadarGridCoverage(gc);
//			viewer.setRadarGridCoverageVisibility(true);
//		}
//		else {
//			viewer.getGridDatasetRaster().setWritableRaster(processRaster.getWritableRaster());
//			viewer.getGridDatasetRaster().setBounds(processRaster.getBounds());
//			viewer.getGridDatasetRaster().setDisplayMinValue(viewer.getGridDatasetRaster().getMinValue());
//			viewer.getGridDatasetRaster().setDisplayMaxValue(viewer.getGridDatasetRaster().getMaxValue());
//			viewer.setGridSatelliteGridCoverage(gc);
//			viewer.setGridSatelliteVisibility(true);
//			
//			
//			viewer.setGridSatelliteColorTable(viewer.getGridDatasetRaster().getColorTableAlias());
//		}
		
		


		
		// enable export button so results can be saved
		exportButton.setEnabled(true);
	}

	private int getDimensionNumSlicesToProcess() {
		int[] rtIndices = viewer.getGridProps().getSelectedRunTimeIndices();
		int[] tIndices = viewer.getGridProps().getSelectedTimeIndices();
		int[] zIndices = viewer.getGridProps().getSelectedZIndices();
		if (rtIndices.length == 0) {
			rtIndices = new int[] { -1 };
		}
		if (tIndices.length == 0) {
			tIndices = new int[] { -1 };
		}
		if (zIndices.length == 0) {
			zIndices = new int[] { -1 };
		}

		return rtIndices.length*tIndices.length*zIndices.length;			
	}
	
	

//	private boolean isDiffAvailable() {
//		URL[] selectedURLs = viewer.getDataSelector().getSelectedURLs();
//		String operation = operationTypeCombo.getSelectedItem().toString();
//		if (viewer.getCurrentDataType() == CurrentDataType.GRIDDED) {
//			
//			int[] rtIndices = viewer.getGridProps().getSelectedRunTimeIndices();
//			int[] tIndices = viewer.getGridProps().getSelectedTimeIndices();
//			int[] zIndices = viewer.getGridProps().getSelectedZIndices();
//			
//			int rtNumSelected = (rtIndices.length == 0) ? -1 : rtIndices.length;
//			int tNumSelected =  (tIndices.length == 0) ? -1 : tIndices.length;
//			int zNumSelected =  (zIndices.length == 0) ? -1 : zIndices.length;
//
//			
//			System.out.println(selectedURLs.length + " ,,,, "+Math.abs(rtNumSelected * tNumSelected * zNumSelected) );
//			
//			if (operation.equalsIgnoreCase("Diff") && 
//					(selectedURLs.length > 2 || 
//						(selectedURLs.length == 1 && Math.abs(rtNumSelected * tNumSelected * zNumSelected) != 2))) {
//				return false;
//			}
//			else {
//				return true;
//			}
//		}
//		else {
//			return (operation.equalsIgnoreCase("Diff") && selectedURLs.length == 2); 
//		}
//
//	}
	
	
	
	
	private WCTRaster processLayer(WCTExport exporter, WCTRaster processRaster, URL dataURL, Feature markerFeature, int overallIndex) {
		
		System.out.println("Processing: "+dataURL);
		try {
			
			Coordinate coord = markerFeature.getDefaultGeometry().getCoordinate();
			Rectangle2D.Double markerExtent = new Rectangle2D.Double(coord.x, coord.y, 0.0000001, 0.0000001);

			WCTFilter exportFilter = new WCTFilter();
			exportFilter.setExtentFilter(markerExtent);
			exporter.setExportL3Filter(exportFilter);
			exporter.setExportRadialFilter(exportFilter);
			exporter.setExportGridSatelliteFilter(exportFilter);

			System.out.println(markerFeature);

			

			// this handles datatype overrides from user
			SupportedDataType dataTypeOverride = null;
			try {
				if (viewer.getDataSelector().getSelectedDataType() != SupportedDataType.UNKNOWN) { 
					dataTypeOverride = viewer.getDataSelector().getSelectedDataType(); 
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	        boolean checkOpendap = viewer.getDataSelector().getDataSourcePanel().getDataType().equals(WCTDataSourceDB.SINGLE_FILE) ||
	                viewer.getDataSelector().getDataSourcePanel().getDataType().equals(WCTDataSourceDB.THREDDS) ||
	                viewer.getDataSelector().getDataSourcePanel().getDataType().equals(WCTDataSourceDB.FAVORITES);


	        double val;
	        String units = "";
	        long dateMillis = 0L;
	        String filename = "";
//			if (viewer.getDataSelector().getSelectedDataType() == SupportedDataType.GRIDDED) {
	        if (isOperationAcrossDimensions) {
	        	GridDatatype grid = viewer.getGridDatasetRaster().getLastProcessedGridDataset().getGrids().get(viewer.getGridProps().getSelectedGridIndex());
	        	int[] xy = new int[2];
//	        	System.out.println("grid point subset: "+exporter.getFilter().getExtentFilter().y+", "+exporter.getFilter().getExtentFilter().x);
//	        	grid.getCoordinateSystem().findXYindexFromLatLon(exporter.getFilter().getExtentFilter().y, exporter.getFilter().getExtentFilter().x, xy);
	        	grid.getCoordinateSystem().findXYindexFromLatLon(coord.y, coord.x, xy);
//				System.out.println("grid.readDataSlice("+exporter.getGridExportRuntimeIndex()+", -1, "+exporter.getGridExportTimeIndex()+", "+exporter.getGridExportZIndex()+", "+xy[0]+", "+xy[1]);
				val = ((double[])(grid.readDataSlice(
							exporter.getGridExportRuntimeIndex(), -1, 
							exporter.getGridExportTimeIndex(), exporter.getGridExportZIndex(), 
							xy[1], xy[0]).get1DJavaArray(Double.class))
					  )[0];
	
				// TODO: this could be further optimized by trying and timing successively larger time 
				// chunks to reduce 'chatty' calls for small or optimized datasets over OPeNDAP 
				
				
				units = grid.getUnitsString();
				dateMillis = grid.getCoordinateSystem().getCalendarDates().get(exporter.getGridExportTimeIndex()).getMillis();
				filename = viewer.getFileScanner().getLastScanResult().getFileName();
				
				int numToProcess = getDimensionNumSlicesToProcess()*markerList.getSelectedIndices().length;
				double progress = WCTUtils.progressCalculator(
						new int[] { getProcessedCount() }, 
						new int[] { numToProcess } );
				
				progressBar.setValue((int)(progress*100));
				if (shouldCancel) {
					progressBar.setString("Canceling...");
				}
				else {
					progressBar.setString("Processing Grid ("+getProcessedCount()+" of "+numToProcess+")");
				}
			}
	        else {
	        
	        	exporter.exportData(dataURL, File.createTempFile("wct", ".obj"), dataTypeOverride, checkOpendap);

	        	WCTRaster raster = exporter.getLastProcessedRaster();

	        	// Create initial raster and reset bounds
	        	if (processRaster == null) {
	        		DateFormatter df = new DateFormatter();
	        		// create copy of raster that has value of 0.0 for all data
	        		processRaster = WCTMathOp.createEmptyRasterCopy(raster);
	        		processRaster.setLongName("WCT Point Subset operation output 'fake' grid of: "+raster.getLongName()+
	        				" -- First timestep: "+df.toDateTimeStringISO( new Date(raster.getDateInMilliseconds()) ));
	        		processRaster.setVariableName(raster.getVariableName());
	        		processRaster.setStandardName(raster.getStandardName());
	        		processRaster.setUnits(raster.getUnits());
	        	}
	        	// use last processed date for date dimension
	        	processRaster.setDateInMilliseconds(raster.getDateInMilliseconds());

	        	val = raster.getWritableRaster().getSampleDouble(0, 0, 0);
	        	//			System.out.println("value="+val);

	        	units = raster.getUnits();
	        	dateMillis = raster.getDateInMilliseconds();
	        	filename = exporter.getFileScanner().getLastScanResult().getFileName();
	        }
	
			JTextArea resultsTextArea = wctTextPanel.getTextArea();
			if (resultsTextArea.getText().trim().length() == 0) {
				resultsTextArea.append("date_time, value, units, file_name, label1, latitude, longitude\n");
			}

			StringBuilder sb = new StringBuilder();
			sb.append(df.toDateTimeStringISO( new Date(dateMillis)));
			sb.append(", ");
			sb.append(Double.isNaN(val) ? "" : WCTUtils.DECFMT_pDpppp.format(val));
			sb.append(", ");
			sb.append(units);
			sb.append(", ");
			sb.append(filename);
			sb.append(", ");
			sb.append(markerFeature.getAttribute("label1").toString());
			sb.append(", ");
			sb.append(WCTUtils.DECFMT_0D0000.format(coord.y));
			sb.append(", ");
			sb.append(WCTUtils.DECFMT_0D0000.format(coord.x));
			sb.append("\n");

			resultsTextArea.append(sb.toString());
			resultsTextArea.setCaretPosition(resultsTextArea.getText().length());
			
			// check previous data type
			if (dataType != null && dataType != exporter.getLastProcessedDataType()) {
				JOptionPane.showMessageDialog(this, "WCT Point Export Tool Warning", 
						"Warning: New data type found: "+dataType, JOptionPane.WARNING_MESSAGE);
			}
			this.dataType = exporter.getLastProcessedDataType();

//			checkDatasetSpecificInfo(exporter);


			//				System.out.println("n="+n+"   "+scanResult.getDataType());
			System.out.println("n="+overallIndex+"   "+dataType);


		} catch (Exception e) {
//			e.printStackTrace();
			reportException(e, exporter);
		}

		return processRaster;
	}
	
	
	
	

	
	
	
	
	private final class SubmitListener implements ActionListener {
        private Dialog parent;
        public SubmitListener(Dialog parent) {
            this.parent = parent;
        }
        public void actionPerformed(ActionEvent e) {

            if (e.getActionCommand().equalsIgnoreCase("SUBMIT")) {
//            	saveProperties();
//                formSubmitted = true;
            	
            	((JButton)e.getSource()).setEnabled(false);
                try {
                    foxtrot.Worker.post(new foxtrot.Task() {
                        public Object run() {

                            try {

                            	cancelButton.setText("Cancel");
                                cancelButton.setActionCommand("CANCEL");
                                process();
                                cancelButton.setEnabled(true);
                            	cancelButton.setText("Close");
                                cancelButton.setActionCommand("CLOSE");

                        		progressBar.setValue(0);
                        		progressBar.setString("Processing Progress");

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                reportException(ex);
                            }

                            return "DONE";
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            	((JButton)e.getSource()).setEnabled(true);

            }
            else if (e.getActionCommand().equalsIgnoreCase("COPY")) {
            	String str = wctTextPanel.getTextArea().getSelectedText();
            	if (str == null || str.length() <= 0) {
            		str = wctTextPanel.getTextArea().getText();
            	}
            	TextUtils.getInstance().copyToClipboard(str);
            }
            else if (e.getActionCommand().equalsIgnoreCase("EXPORT")) {
            	showExportDialog();
            }
            else if (e.getActionCommand().equalsIgnoreCase("CANCEL")) {
            	setShouldCancel(true);
                cancelButton.setEnabled(false);
            	cancelButton.setText("Canceling...");
            }
            else if (e.getActionCommand().equalsIgnoreCase("EDIT_LEGEND")) {
            	LegendEditor legendEditor = LegendEditor.getInstance(viewer, viewer);
            	legendEditor.setVisible(true);
            }
            else {
            	parent.dispose();
            }
        }


    }

	
	private void reportException(Exception ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage());

	}
	
	private void reportException(Exception ex, WCTExport exporter) {
//        JOptionPane.showMessageDialog(this, ex.getMessage());
		
	       ex.printStackTrace();
           
           errorLogDialog.getTextArea().append(exporter.getFileScanner().getLastScanResult().getFileName()+
           		" ("+exporter.getFileScanner().getLastScanResult().getLongName() +
           		")  [Error Date: "+new Date()+"]\n\t "+ ex.getMessage()+"\n");
       	if (! errorLogDialog.isVisible()) {
       		errorLogDialog.setLocationRelativeTo(this);
               errorLogDialog.setVisible(true);
       	}

	}
	
	private int getProcessedCount() {
		return processedCountInLoop;
	}

	public void setShouldCancel(boolean shouldCancel) {
		this.shouldCancel = shouldCancel;
	}

	public boolean isShouldCancel() {
		return shouldCancel;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void showExportDialog() {

		final JTextField jtfOutDir = new JTextField(25);
		final JTextField jtfOutFile = new JTextField(10);

        String dir = WCTProperties.getWCTProperty("point_subset_export_dir");
        if (dir == null) {
            dir = "";
        }
        String file = WCTProperties.getWCTProperty("point_subset_export_file");
        if (file == null) {
        	file = "";
        }
        jtfOutDir.setText(dir);
        jtfOutFile.setText(file);
        
        // JIDE stuff
        jtfOutDir.setName("File IntelliHint");
        SelectAllUtils.install(jtfOutDir);
        new FileIntelliHints(jtfOutDir).setFolderOnly(true);

        
        
		final JComboBox<ExportFormat> jcomboFormat = new JComboBox<ExportFormat>();
		jcomboFormat.addItem(ExportFormat.CSV);
		
		final JButton jbBrowse = new JButton("Browse");
		final Component finalThis = this;
		final String lastDir = dir;
		final String lastFile = file;
		jbBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                
                
                FolderChooser folderChooser = new FolderChooser() {
                    protected JDialog createDialog(Component parent)
                    	throws HeadlessException {
                    	JDialog dlg = super.createDialog(parent);
                    	dlg.setLocation((int)jbBrowse.getLocationOnScreen().getX()+jbBrowse.getWidth()+4, 
                    			(int)jbBrowse.getLocationOnScreen().getY());
                    	return dlg;
                    }
                };
                folderChooser.setRecentListVisible(false);
                folderChooser.setAvailableButtons(FolderChooser.BUTTON_REFRESH | FolderChooser.BUTTON_DESKTOP 
                		| FolderChooser.BUTTON_MY_DOCUMENTS);
                if (lastDir.length() > 0) {
                    File lastFolderFile = folderChooser.getFileSystemView().createFileObject(lastDir);
                    folderChooser.setCurrentDirectory(lastFolderFile);
                }
                folderChooser.setFileHidingEnabled(true);
                folderChooser.setPreferredSize(new Dimension(320, 500));
                int result = folderChooser.showOpenDialog(finalThis);
                if (result == FolderChooser.APPROVE_OPTION) {
                    File selectedFile = folderChooser.getSelectedFile();
                    if (selectedFile != null) {
                    	jtfOutDir.setText(selectedFile.toString());
                    }
                    else {
                    	jtfOutDir.setText("");
                    }

                }
                
            }
        });
		final JDialog exportDialog = new JDialog(this, 
				"Point Subset Results Export", ModalityType.DOCUMENT_MODAL);
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(WCTUiUtils.myTitledTopBorder("Select Export Format and File", 0, 5, 5, 5, TitledBorder.CENTER, TitledBorder.TOP));

		mainPanel.setLayout(new RiverLayout());
		mainPanel.add("p", new JLabel("Output Location: "));
//		mainPanel.add(new JLabel(" "), "tab hfill");
//		mainPanel.add(jbBrowse, "right");
				
		mainPanel.add(jtfOutDir, "br left hfill");
		mainPanel.add(jbBrowse, "right");
		mainPanel.add(new JLabel("Format: "), "p left");
		mainPanel.add(jcomboFormat, "tab hfill");
		mainPanel.add(new JLabel("Filename: "), "p");
		mainPanel.add(jtfOutFile, "tab hfill");
		
		JButton jbExport = new JButton("Export");
		jbExport.setPreferredSize(new Dimension(60, (int)jbExport.getPreferredSize().getHeight()));		
		JButton jbCancel = new JButton("Cancel");
		jbCancel.setPreferredSize(new Dimension(60, (int)cancelButton.getPreferredSize().getHeight()));
		
		jbExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				exportDialog.dispose();
				try {
					doExportInBackground(jtfOutDir.getText(), jtfOutFile.getText(), (ExportFormat)jcomboFormat.getSelectedItem());
				} catch (Exception e) {
					reportException(e);
				}
			}			
		});		
		jbCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
//				process = false;
				exportDialog.dispose();
			}			
		});
		mainPanel.add(jbExport, "p center");
		mainPanel.add(jbCancel);
		
		exportDialog.add(mainPanel);
		
		exportDialog.pack();
		exportDialog.setLocationRelativeTo(this);
		exportDialog.setVisible(true);
		
		
		
		System.out.println(jtfOutDir);
		System.out.println(jtfOutFile);
		System.out.println(jcomboFormat.getSelectedItem());
	}
	
	
	private void doExportInBackground(final String outDir, final String outFile, final ExportFormat format) throws Exception {
		
            foxtrot.Worker.post(new foxtrot.Task() {
                public Object run() {

                    try {
                    	doExport(outDir, outFile, format);
                    } catch (Exception ex) {
//                        ex.printStackTrace();
                        reportException(ex);
                    }

                    return "DONE";
                }
            });

	}
	
	
	private void doExport(final String outDir, String outFile, final ExportFormat format) {

        WCTProperties.setWCTProperty("point_subset_export_dir", outDir);
        WCTProperties.setWCTProperty("point_subset_export_file", outFile);

		
		final String finalOutFile = outFile;
    	
    	try {
    		
    		if (format == ExportFormat.CSV) {
    			if (! outFile.endsWith(".csv")) {
    				outFile = outFile + ".csv";
    			}
				FileUtils.writeStringToFile(new File(outDir+File.separator+outFile), wctTextPanel.getTextArea().getText());
    		}
		} catch (IOException e1) {
			e1.printStackTrace();
		}	
	}
}
