package steve.test;

import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDateRange;

public class CFClimatologyParser {
	
	public static void main(String[] args) {
		
		try {
			
			GridDataset gd = GridDataset.open("C:\\work\\cdr\\AMSU_CH9_RO_CAL_BT_V01R00_CLIM_2002_2010.nc");
			
			System.out.println(gd.getNetcdfDataset().toString());
			
			System.out.println(gd.getCalendarDateStart());
			System.out.println(gd.getCalendarDateEnd());
			System.out.println(gd.getCalendarDateRange());
			
			CalendarDateRange cdr = gd.getCalendarDateRange();
			
			
			GridDatatype grid = gd.findGridDatatype("bt_climatology");
			Attribute climAtt = grid.getCoordinateSystem().getTimeAxis().findAttribute("climatology");
			
			System.out.println(climAtt);
			
//			isClimatology()
//			getClimatologyDates()
			
//			class ClimatologyDate
//			enum HOUR, DAY, MONTH, OTHER
//			getType()
//			
			
			
			
			
			
			
			gd.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
