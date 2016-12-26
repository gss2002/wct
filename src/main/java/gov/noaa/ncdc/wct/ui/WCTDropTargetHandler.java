package gov.noaa.ncdc.wct.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import gov.noaa.ncdc.wct.io.DirectoryScanner;
import gov.noaa.ncdc.wct.io.ScanResults;
import gov.noaa.ncdc.wct.io.WCTDataSourceDB;


class WCTDropTargetHandler implements DropTargetListener {
    
    private static WCTDropTargetHandler singleton = null;    
    private WCTViewer viewer = null;
    
    private WCTDropTargetHandler() {
    }
    
    public static WCTDropTargetHandler getInstance() {
        if (singleton == null) {
            singleton = new WCTDropTargetHandler();
        }
        return singleton;
    }
    
    public void registerViewer(WCTViewer viewer) {
        this.viewer = viewer;
    }
    
    @Override
    public void drop(DropTargetDropEvent event) {
        
        Transferable transferable = event.getTransferable();
        
        if (viewer == null) {
            System.err.println("No viewer is registered");            
        }
        
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            event.acceptDrop(DnDConstants.ACTION_COPY);
            try {
                String s = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                System.out.println(s);
                
                
                if (s.startsWith("http://") || s.startsWith("ftp://")) {
                    s = s.split("\n")[0];
                }
                
                if (s.startsWith("http://www.ncdc.noaa.gov/cgi-bin/good-bye.pl?src=")) {
                    s = s.replaceAll("http://www.ncdc.noaa.gov/cgi-bin/good-bye.pl\\?src=", "");
                }
                
                if (s.contains(".html") && ! s.contains("/thredds/dodsC/")) {
                    s = s.replaceAll(".html", ".xml");
                }
                
                if (s.startsWith("HAS") && s.length() == 12) {
                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.NCDC_HAS_FTP);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.NCDC_HAS_FTP, s);
                    viewer.getDataSelector().submitListFiles();
                }
                else if (s.startsWith("http://noaa-nexrad-level2.s3.amazonaws.com") ||
                		s.startsWith("https://noaa-nexrad-level2.s3.amazonaws.com")) {
                	// http://noaa-nexrad-level2.s3.amazonaws.com/2009/05/31/KGSP/KGSP20090531_001905_V03.gz
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.NOAA_BDP_AWS);
                    String[] folders = s.split("/");
                    String yyyymmdd = folders[3]+folders[4]+folders[5];
                    String siteid=folders[6];
                    
                    NexradBDPAccessPanel bdpAccessPanel = viewer.getDataSelector().getDataSourcePanel().getNexradBDPAccessPanel();
                    bdpAccessPanel.setDate(yyyymmdd);
                    bdpAccessPanel.setSite(siteid);

                    viewer.getDataSelector().submitListFiles();
                    
                    
                    ScanResults[] scanResults = viewer.getDataSelector().getScanResults();
                    for (int n=0; n<scanResults.length; n++) {
                    	if (scanResults[n].getFileName().equals(folders[7])) {
                    		viewer.getDataSelector().getResultsList().setSelectedIndex(n);
                            viewer.getDataSelector().getResultsList().ensureIndexIsVisible(n);
                    	}
                    }         
                    viewer.getDataSelector().loadData();
                	
                }
                else if (s.startsWith("http://") && s.contains("/thredds/dodsC/") && s.endsWith(".html")) {
                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.SINGLE_FILE);
                    String opendapLocation = s.substring(0, s.length()-5);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.SINGLE_FILE, opendapLocation);
                    viewer.getDataSelector().loadData();
                }
                else if (s.contains("catalog.xml") || (s.contains("/thredds/") && s.endsWith("xml"))) {
                	if (! (s.startsWith("http://") || s.startsWith("https://")) ) {
                		s = "http://"+s;
                	}
                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.THREDDS);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.THREDDS, s);
                    viewer.getDataSelector().submitListFiles();
                }
                else if (s.startsWith(DirectoryScanner.NCDC_HAS_HTTP_SERVER+"/"+DirectoryScanner.NCDC_HAS_HTTP_DIRECTORY) ||
                		s.startsWith("ftp://"+DirectoryScanner.NCDC_HAS_FTP_SERVER+"/"+DirectoryScanner.NCDC_HAS_FTP_DIRECTORY) ) {
                	
                	String order = null;
                	if (s.startsWith("ftp")) {
                		int startIndex = s.indexOf(DirectoryScanner.NCDC_HAS_FTP_DIRECTORY) + DirectoryScanner.NCDC_HAS_FTP_DIRECTORY.length() + 1;
                		int endIndex = startIndex + 12; // 12 = HAS123456789
                		order = s.substring(startIndex, endIndex);
                	}
                	else {
                		int startIndex = s.indexOf(DirectoryScanner.NCDC_HAS_HTTP_DIRECTORY) + DirectoryScanner.NCDC_HAS_HTTP_DIRECTORY.length() + 1;
                		int endIndex = startIndex + 12; // 12 = HAS123456789
                		order = s.substring(startIndex, endIndex);
                	}
                	
                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.NCDC_HAS_FTP);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.NCDC_HAS_FTP, order);
                    viewer.getDataSelector().submitListFiles();
                }
                else if (s.startsWith("http://") || s.startsWith("ftp://")) {
                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.URL_DIRECTORY);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.URL_DIRECTORY, s);
                    viewer.getDataSelector().submitListFiles();
                }
                
                
                
                event.dropComplete(true);
            } catch (UnsupportedFlavorException flavorException) {
                flavorException.printStackTrace();
                event.dropComplete(false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                event.dropComplete(false);
            } catch (Exception e) {
                e.printStackTrace();
                event.dropComplete(false);
            }
        }
        else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            event.acceptDrop(DnDConstants.ACTION_COPY);
            try {
                List fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                Iterator iterator = fileList.iterator();
                // take first entry
                if (iterator.hasNext()) {
//                    System.out.println("javaFileListFlavor: "+iterator.next().toString());
                    File file = (File) iterator.next();
                    if (! file.isDirectory()) {
                        file = file.getParentFile();
                    }

                    viewer.getDataSelector().setVisible(true);
                    viewer.getDataSelector().getDataSourcePanel().setDataType(WCTDataSourceDB.LOCAL_DISK);
                    viewer.getDataSelector().getDataSourcePanel().setDataLocation(WCTDataSourceDB.LOCAL_DISK, file.toString());
                    viewer.getDataSelector().submitListFiles();
                        
                    
                    System.out.println(file);
                }
                event.dropComplete(true);
            } catch (UnsupportedFlavorException flavorException) {
                flavorException.printStackTrace();
                event.dropComplete(false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                event.dropComplete(false);
            } catch (Exception e) {
                e.printStackTrace();
                event.dropComplete(false);
            }
        }
        else {
            event.rejectDrop();
        }
        
    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
        if (viewer != null && (
                event.isDataFlavorSupported(DataFlavor.stringFlavor) || 
                event.isDataFlavorSupported(DataFlavor.javaFileListFlavor) )) {
            
            event.acceptDrag(DnDConstants.ACTION_COPY);
        }
        else {
            event.rejectDrag();
        }
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }

}
