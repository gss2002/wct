package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.decoders.ColorLutReaders;
import gov.noaa.ncdc.wct.decoders.ColorsAndValues;
import gov.noaa.ncdc.wct.decoders.SampleDimensionAndLabels;
import gov.noaa.ncdc.wct.event.GeneralProgressEvent;
import gov.noaa.ncdc.wct.event.GeneralProgressListener;
import gov.noaa.ncdc.wct.export.WCTExportException;
import gov.noaa.ncdc.wct.export.WCTExportNoDataException;
import gov.noaa.ncdc.wct.export.WCTExport.ExportFormat;
import gov.noaa.ncdc.wct.export.raster.WCTRasterExport;
import gov.noaa.ncdc.wct.export.raster.WCTRasterExport.GeoTiffType;
import gov.noaa.ncdc.wct.ui.event.MousePopupListener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.media.jai.WritableRenderedImageAdapter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.gc.GridCoverage;
import org.geotools.renderer.j2d.RenderedLayer;
import org.jdesktop.swingx.StackedBox;

import ucar.ma2.InvalidRangeException;

import com.jidesoft.hints.FileIntelliHints;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.SelectAllUtils;

public class SnapshotPanel extends JPanel {

    private WCTViewer viewer;
    private StackedBox dataLayersStack;
    private SnapshotLayer snapshotLayer;

    private static final Icon upIcon = new ImageIcon(WCTToolBar.class.getResource("/icons/go-up.png"));
    private static final Icon downIcon = new ImageIcon(WCTToolBar.class.getResource("/icons/go-down.png"));
    private static final Icon zoomToIcon = new ImageIcon(WCTToolBar.class.getResource("/icons/view-fullscreen.png"));
    private static final Icon infoIcon = new ImageIcon(WCTToolBar.class.getResource("/icons/question-mark.png"));


    final JComboBox<String> jcomboTransparency = new JComboBox<String>(new String[] {
            " Default", "  0 %", " 10 %", " 20 %", " 30 %", " 40 %", " 50 %", 
            " 60 %", " 70 %", " 80 %", " 90 %", "100 %"
    });      



    public SnapshotPanel(WCTViewer viewer, StackedBox dataLayersStack, SnapshotLayer snapshotLayer) {
        super(new RiverLayout());
        this.viewer = viewer;
        this.dataLayersStack = dataLayersStack;
        this.snapshotLayer = snapshotLayer;

        createUI();
    }

    private void createUI() {

        final JCheckBox jcbVisible = new JCheckBox("Visible", true);
        jcbVisible.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                snapshotLayer.getRenderedGridCoverage().setVisible(jcbVisible.isSelected());
                viewer.fireRenderCompleteEvent();
            }
        });
        this.add(jcbVisible);


        jcomboTransparency.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                snapshotLayer.setAlpha(getTransparencyAlpha());
                viewer.fireRenderCompleteEvent();
            }
        });
        jcomboTransparency.setEditable(true);




        //        JPanel transPanel = new JPanel();
        //        transPanel.setLayout(new RiverLayout());
        //        transPanel.add(new JLabel("Transparency"), "center");
        //        transPanel.add(jcomboTransparency, "br center");
        //        this.add(transPanel, "tab");
        this.add(new JLabel("Trans.: "), "tab");
        this.add(jcomboTransparency);

        snapshotLayer.getRenderedGridCoverage().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
//                System.out.println(evt.getPropertyName() + "  | "+evt.getOldValue() + " to "+evt.getNewValue());
                if (evt.getPropertyName().equals("visible")) {
                    jcbVisible.setSelected(Boolean.valueOf(evt.getNewValue().toString()));
                }
            }

        });


        final JButton jbIsolate = new JButton("Iso.");
//        final JButton jbIsolate = new JButton(new ImageIcon(WCTToolBar.class.getResource("/icons/iso2.png")));
        jbIsolate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (SnapshotLayer s : viewer.getSnapshotLayers()) {
                    s.getRenderedGridCoverage().setVisible(false);
                }
                snapshotLayer.getRenderedGridCoverage().setVisible(true);
                viewer.fireRenderCompleteEvent();
            }
        });
        this.add(jbIsolate, "tab");


        final JButton jbZoom = new JButton(zoomToIcon);
        jbZoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.setCurrentExtent(snapshotLayer.getRenderedGridCoverage().getGridCoverage().getEnvelope().toRectangle2D());
                viewer.fireRenderCompleteEvent();
            }            
        });
        this.add(jbZoom, "tab");


        final JButton jbMoveUp = new JButton(upIcon);
        jbMoveUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                RenderedLayer[] layers = viewer.getMapPane().getRenderer().getLayers();
                List<RenderedLayer> layerList = Arrays.asList(layers);

                int index = layerList.indexOf(snapshotLayer.getRenderedGridCoverage());
                int gridSatelliteIndex = layerList.indexOf(viewer.getGridSatelliteRenderedGridCoverage());
                if ( Math.abs(index-gridSatelliteIndex) == 1 ) {
                    System.out.println("next to grid/sat rgc - doing nothing");
                    return;
                }

                dataLayersStack.moveBoxUp(dataLayersStack.getIndexOf(snapshotLayer.getName()));

                for (RenderedLayer rl : viewer.getMapPane().getRenderer().getLayers()) {
                    System.out.println("before: "+rl.toString());
                }

                RenderedLayer switchLayer = layers[index+1];
                switchLayer.setZOrder(switchLayer.getZOrder()-0.001f);

                RenderedLayer mainLayer = snapshotLayer.getRenderedGridCoverage();
                mainLayer.setZOrder(mainLayer.getZOrder()+0.001f);

                layers[index] = switchLayer;
                layers[index+1] = mainLayer;

                for (RenderedLayer rl : viewer.getMapPane().getRenderer().getLayers()) {
                    System.out.println("after: "+rl.toString());
                }

                System.out.println("switching "+snapshotLayer.getRenderedGridCoverage()+"  with  "+switchLayer);
                
                viewer.fireRenderCompleteEvent();

            }
        });
        final JButton jbMoveDown = new JButton(downIcon);
        jbMoveDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                RenderedLayer[] layers = viewer.getMapPane().getRenderer().getLayers();
                List<RenderedLayer> layerList = Arrays.asList(layers);

                int index = layerList.indexOf(snapshotLayer.getRenderedGridCoverage());
                int gridSatelliteIndex = layerList.indexOf(viewer.getGridSatelliteRenderedGridCoverage());
                if ( Math.abs(index-gridSatelliteIndex) == viewer.getSnapshotLayers().size() ) {
                    System.out.println("at bottom- doing nothing");
                    return;
                }


                dataLayersStack.moveBoxDown(dataLayersStack.getIndexOf(snapshotLayer.getName()));

                //                for (RenderedLayer rl : viewer.getMapPane().getRenderer().getLayers()) {
                //                    System.out.println("before: "+rl.toString());
                //                }


                RenderedLayer switchLayer = layers[index-1];
                switchLayer.setZOrder(switchLayer.getZOrder()+0.001f);

                RenderedLayer mainLayer = snapshotLayer.getRenderedGridCoverage();
                mainLayer.setZOrder(mainLayer.getZOrder()-0.001f);

                layers[index] = switchLayer;
                layers[index-1] = mainLayer;

                //                for (RenderedLayer rl : viewer.getMapPane().getRenderer().getLayers()) {
                //                    System.out.println("after: "+rl.toString());
                //                }

                System.out.println("switching "+snapshotLayer.getRenderedGridCoverage()+"  with  "+switchLayer);

                viewer.fireRenderCompleteEvent();
            }
        });
        this.add(jbMoveUp, "tab");
        this.add(jbMoveDown);

        // now do 'options' menu


        JButton jbTools = new JButton("...");
        
        final JPopupMenu morePopupMenu = new JPopupMenu("Additional Operations");
        JMenuItem titleItem = new JMenuItem("<html><b>Additional Operations</b></html>");
        titleItem.setToolTipText("List of additional operations");
        titleItem.setEnabled(false);
        morePopupMenu.add(titleItem);
        morePopupMenu.addSeparator();

        JMenuItem itemExport = new JMenuItem("Export Data");
        itemExport.setToolTipText("Export to a supported output Raster/Grid format");
        itemExport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportData();
            }
        });
        
        // TODO: perhaps finish someday...
//        JMenuItem itemEditColorTable = new JMenuItem("Edit Color Table");
//        itemEditColorTable.setToolTipText("Edit the color table for this snapshot");
//        itemEditColorTable.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//            	
//            	try {
//				
//            		String test = 
//            				"#            R    G    B    A"+
//            						"Color:  75   235  235  235  255"+
//            						"Color:  35   255  255  0    255"+
//            						"Color:  0    187  255  255  255";
//
//            		ColorsAndValues[] cavArray = 
//            				ColorLutReaders.parseWCTPal(new BufferedReader(new StringReader(test)));
//
//            		SampleDimensionAndLabels sd = ColorLutReaders.convertToSampleDimensionAndLabels(
//            				cavArray[0], cavArray[1]);
//
//
//            		GridCoverage gc = snapshotLayer.getRenderedGridCoverage().getGridCoverage();
//            		WritableRenderedImageAdapter img = (WritableRenderedImageAdapter)(gc.getRenderedImage());
//            		WritableRaster data = (WritableRaster)img.getData();
//
//            		GridCoverage newGC = new GridCoverage(
//            				gc.getName(null), data, GeographicCoordinateSystem.WGS84, 
//            				null, gc.getEnvelope(), 
//            				new SampleDimension[] { sd.getSampleDimension() });
//
//            		snapshotLayer.getRenderedGridCoverage().setGridCoverage(newGC);
//
//        		
//				} catch (Exception e2) {
//					e2.printStackTrace();
//				}
//
//            }
//        });
//        JMenuItem itemEditLegend = new JMenuItem("Edit Legend");
//        itemEditLegend.setToolTipText("Edit the legend text and visibility on main map");
//        itemEditLegend.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                
//            }
//        });
//        
//        JMenuItem itemResnapshot = new JMenuItem("Re-snapshot");
//        itemResnapshot.setToolTipText("Create new snapshot from underlying parent data at current zoom extent");
//        itemResnapshot.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                
//            }
//        });
        
        JMenuItem itemLoadSnapshotParentData = new JMenuItem("Reload Parent Data");
        itemLoadSnapshotParentData.setToolTipText("Load the underlying parent dataset as the current active layer");
        itemLoadSnapshotParentData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewer.loadFile(snapshotLayer.getDataURL());
            }
        });
        
        morePopupMenu.add(itemExport);
//        morePopupMenu.add(itemEditColorTable);
//        morePopupMenu.add(itemEditLegend);
//        morePopupMenu.add(itemResnapshot);
        morePopupMenu.add(itemLoadSnapshotParentData);
        jbTools.addMouseListener(new MousePopupListener(jbTools, morePopupMenu));
        
        
        
        
        JButton jbLegend = new JButton("Legend");
        jbLegend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog legendDialog = new JDialog(viewer);
                legendDialog.getContentPane().setLayout(new RiverLayout());
                legendDialog.getContentPane().add(new JLabel(new ImageIcon(snapshotLayer.getLegendImage())));
                legendDialog.setTitle(snapshotLayer.getName());
                legendDialog.pack();
                legendDialog.setVisible(true);
            }
        });
        
        
        
        

        final JButton jbInfo = new JButton(infoIcon);
        
        
        this.add(jbTools, "tab");
        this.add(jbInfo, "tab");
                

    }

    public int getTransparencyAlpha() {
        if (jcomboTransparency.getSelectedItem().toString().trim().equalsIgnoreCase("Default")) {
            return -1;
        }
        else {
            int percent = Integer.parseInt(jcomboTransparency.getSelectedItem().toString().replaceAll("%", "").trim());
            int alpha = 255-(int)(percent*0.01*255);
            return alpha;
        }
    }
    
    
    
    private void exportData() {
    	WCTQuickExportUI exportUI = new WCTQuickExportUI(viewer, "snapshot_export_dir", "snapshot_export_file");
    	exportUI.showExportRasterDialog(snapshotLayer.getRaster());
    }
    

	
	
}
