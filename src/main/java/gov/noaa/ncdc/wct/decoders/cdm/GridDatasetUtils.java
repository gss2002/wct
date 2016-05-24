package gov.noaa.ncdc.wct.decoders.cdm;

import gov.noaa.ncdc.wct.WCTException;
import gov.noaa.ncdc.wct.WCTUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import opendap.dap.DAP2Exception;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

public class GridDatasetUtils {
	
	
	private static boolean globalCaching = true;
	

	private static HashMap<String, GridDataset> gdCache = new HashMap<String, GridDataset>();
	

	/**
	 * Open a GridDataset.  May return a dataset with no grids.  IOException thrown if the dataset
	 * cannot be opened.
	 * @param gridDatasetURL
	 * @param errlog
	 * @return
	 * @throws IOException
	 */
	public static GridDataset openGridDataset(String source, StringBuilder errlog) throws DAP2Exception, IOException {
		
        GridDataset gds = null;     
        
        
//        NetcdfDataset.initNetcdfFileCache(0, 2, 2, 60*10);
        
    	// check if this GridDataset has been closed
    	GridDataset gd = gdCache.get(source);
    	if (gd != null && gd.getNetcdfFile() == null) {        	
    		gdCache.remove(source);
    	}
    	
        // get the dataset if cached
        if (isGlobalCaching() && gdCache.containsKey(source)) {
        	return gdCache.get(source);
        }
        
        
        Formatter fmter = new Formatter();
        
		// 1. Try to open the 'standard' way
        try {
        	gds = (GridDataset) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.GRID, source, WCTUtils.getSharedCancelTask(), fmter);
        } catch (Exception e) {
        	e.printStackTrace();
        	System.err.println("Grid Dataset open attempt: error message = "+e.getMessage()+",  formatted message: "+fmter.toString());
        	System.out.println("Standard Grid File Opening Failed....  Trying to manually open via OPeNDAP");
        
        	// 2. Try to open manually via OPeNDAP
        	if (source.startsWith("http:")) {
        		source = source.replace("http:", "dods:");
        		gds = (GridDataset) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.GRID, source, WCTUtils.getSharedCancelTask(), fmter);
        	}
        }

        errlog.append(fmter.toString());
        
        if (gds == null) { 
            throw new IOException("Can't open GRID at location= "+source+"; error message= "+errlog);
        }
        
        
        
        if (isGlobalCaching()) {
        	for (String key : gdCache.keySet()) {
        		gdCache.get(key).close();
        	}
        	
        	gdCache.clear();
        	gdCache.put(source, gds);
        }
        
        
        
        return gds;
	}



	/**
	 * copy from NcssRequestUtils in thredds
	 * @param gap
	 * @param dates
	 * @param timeWindow
	 * @return
	 * @throws TimeOutOfWindowException
	 * @throws OutOfBoundariesException
	 */
	public static List<CalendarDate> wantedDates(GridAsPointDataset gap, CalendarDateRange dates, long timeWindow) throws WCTException{

        CalendarDate start = dates.getStart();
        CalendarDate end = dates.getEnd();
                        
        
        List<CalendarDate> gdsDates = gap.getDates();

        if (  start.isAfter(gdsDates.get(gdsDates.size()-1))  || end.isBefore(gdsDates.get(0))  )
                throw new WCTException("Requested time range does not intersect the Data Time Range = " + gdsDates.get(0) + " to " + gdsDates.get(gdsDates.size()-1) );
        
        List<CalendarDate> wantDates = new ArrayList<CalendarDate>();
        
        if(dates.isPoint()){
      int best_index = 0;
      long best_diff = Long.MAX_VALUE;
      for (int i = 0; i < gdsDates.size(); i++) {
        CalendarDate date =  gdsDates.get(i);
        long diff = Math.abs( date.getDifferenceInMsecs( start) );
        if (diff < best_diff) {
          best_index = i;
          best_diff = diff;
        }
      }
      if( timeWindow > 0 && best_diff > timeWindow) //Best time is out of our acceptable timeWindow
              throw new WCTException("There is not time within the provided time window"); 
              
              
      wantDates.add(gdsDates.get(best_index));                
        }else{                                
                for (CalendarDate date : gdsDates) {
                        if (date.isBefore(start) || date.isAfter(end))
                                continue;
                        wantDates.add(date);
                }
        }
        return wantDates;
}

	
	
	
	

	public static void setGlobalCaching(boolean globalCaching) {
		GridDatasetUtils.globalCaching = globalCaching;
	}


	public static boolean isGlobalCaching() {
		return globalCaching;
	}








	public static void scheduleToClose(GridDataset gds) {
		// previous file is closed when a new file is requested
	}
	
	
}
