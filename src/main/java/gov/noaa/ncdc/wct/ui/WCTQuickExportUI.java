package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.event.GeneralProgressEvent;
import gov.noaa.ncdc.wct.event.GeneralProgressListener;
import gov.noaa.ncdc.wct.export.WCTExport.ExportFormat;
import gov.noaa.ncdc.wct.export.WCTExportException;
import gov.noaa.ncdc.wct.export.WCTExportNoDataException;
import gov.noaa.ncdc.wct.export.raster.WCTRaster;
import gov.noaa.ncdc.wct.export.raster.WCTRasterExport;
import gov.noaa.ncdc.wct.export.raster.WCTRasterExport.GeoTiffType;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import ucar.ma2.InvalidRangeException;

import com.jidesoft.hints.FileIntelliHints;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.SelectAllUtils;

public class WCTQuickExportUI {
	
	private WCTViewer viewer;
	private WCTRaster raster;
	private String historyPropertyKey_Dir;
	private String historyPropertyKey_File;
	
	public WCTQuickExportUI(WCTViewer viewer, WCTRaster raster, 
			String historyPropertyKey_Dir, String historyPropertyKey_File) {
		
		this.viewer = viewer;
		this.raster = raster;
		this.historyPropertyKey_Dir = historyPropertyKey_Dir;
		this.historyPropertyKey_File = historyPropertyKey_File;
	}


	public void showExportDialog() {

		final JTextField jtfOutDir = new JTextField(25);
		final JTextField jtfOutFile = new JTextField(10);

        String dir = WCTProperties.getWCTProperty(historyPropertyKey_Dir);
        if (dir == null) {
            dir = "";
        }
        String file = WCTProperties.getWCTProperty(historyPropertyKey_File);
        if (file == null) {
        	file = "";
        }
        jtfOutDir.setText(dir);
        jtfOutFile.setText(file);
        
        // JIDE stuff
        jtfOutDir.setName("File IntelliHint");
        SelectAllUtils.install(jtfOutDir);
        new FileIntelliHints(jtfOutDir).setFolderOnly(true);

        
        
		final JComboBox<ExportFormat> jcomboFormat = new JComboBox<ExportFormat>(ExportFormat.values());
		jcomboFormat.removeItem(ExportFormat.NATIVE);
		jcomboFormat.removeItem(ExportFormat.CSV);
		jcomboFormat.removeItem(ExportFormat.GEOJSON);
		jcomboFormat.removeItem(ExportFormat.KMZ);
		jcomboFormat.removeItem(ExportFormat.RAW_NETCDF);
		jcomboFormat.removeItem(ExportFormat.SHAPEFILE);
		jcomboFormat.removeItem(ExportFormat.VTK);
		jcomboFormat.removeItem(ExportFormat.WKT);
		jcomboFormat.removeItem(ExportFormat.WCT_RASTER_OBJECT_ONLY);
		
		final JButton jbBrowse = new JButton("Browse");
		final Component finalThis = viewer;
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
		final JDialog exportDialog = new JDialog(viewer.getMapSelector(), "Math Results Export", ModalityType.DOCUMENT_MODAL);
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
		jbCancel.setPreferredSize(new Dimension(60, (int)jbCancel.getPreferredSize().getHeight()));
		
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
		exportDialog.setLocationRelativeTo(viewer);
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
                        ex.printStackTrace();
                        reportException(ex);
                    }

                    return "DONE";
                }
            });

	}
	
	
	private void doExport(final String outDir, String outFile, final ExportFormat format) {

        WCTProperties.setWCTProperty(this.historyPropertyKey_Dir, outDir);
        WCTProperties.setWCTProperty(this.historyPropertyKey_File, outFile);

		
		final String finalOutFile = outFile;
    	WCTRasterExport rasterExport = new WCTRasterExport();
    	rasterExport.addGeneralProgressListener(new GeneralProgressListener() {
			@Override
			public void started(GeneralProgressEvent event) {
//				progressBar.setString(event.getStatus());
			}
			@Override
			public void ended(GeneralProgressEvent event) {
//				progressBar.setValue(0);
//				progressBar.setString("");
			}
			@Override
			public void progress(GeneralProgressEvent event) {
//				progressBar.setValue((int)event.getProgress());
//				progressBar.setString("Saving File: "+(int)event.getProgress()+"% complete");
			}    		
    	});
    	
    	try {
    		
    		
    		if (format == ExportFormat.ARCINFOASCII) {
    			if (! outFile.endsWith(".asc")) {
    				outFile = outFile + ".asc";
    			}
    			rasterExport.saveAsciiGrid(new File(outDir+File.separator+outFile), raster);
    		}
    		else if (format == ExportFormat.ARCINFOBINARY) {
    			if (! outFile.endsWith(".flt")) {
    				outFile = outFile + ".flt";
    			}
    			rasterExport.saveBinaryGrid(new File(outDir+File.separator+outFile), raster, true);
    		}
    		else if (format == ExportFormat.GRIDDED_NETCDF) {
    			if (! outFile.endsWith(".nc")) {
    				outFile = outFile + ".nc";
    			}
    			rasterExport.saveNetCDF(new File(outDir+File.separator+outFile), raster);
    		}
    		else if (format == ExportFormat.GEOTIFF_32BIT) {
    			if (! outFile.endsWith(".tif")) {
    				outFile = outFile + ".tif";
    			}
    			rasterExport.saveGeoTIFF(new File(outDir+File.separator+outFile), raster, GeoTiffType.TYPE_32_BIT);
    		}
    		else if (format == ExportFormat.GEOTIFF_GRAYSCALE_8BIT) {
    			if (! outFile.endsWith(".tif")) {
    				outFile = outFile + ".tif";
    			}
    			rasterExport.saveGeoTIFF(new File(outDir+File.separator+outFile), raster, GeoTiffType.TYPE_8_BIT);
    		}
		} catch (WCTExportNoDataException e1) {
			e1.printStackTrace();
		} catch (WCTExportException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}	
	}

	private void reportException(Exception ex) {
        JOptionPane.showMessageDialog(viewer, ex.getMessage());
	}
}
