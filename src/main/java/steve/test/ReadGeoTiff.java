package steve.test;

import gov.noaa.ncdc.wct.WCTIospManager;

import java.io.IOException;
import java.io.StringWriter;

import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;

public class ReadGeoTiff {

	public static void main(String[] args) {
		
		String errors = "";
		try {
            WCTIospManager.getInstance().registerIosp(ucar.nc2.iosp.geotiff.GeoTiffIOServiceProvider.class);
        } catch (Exception e) {
            e.printStackTrace();
            errors += "Error registering ucar.nc2.iosp.geotiff.GeoTiffIOServiceProvider.class\n";
        }
        
		try {
			String file = "D:\\work\\export\\goes\\GOES13_BAND_01_20130720_005518.tif";
			
			NetcdfFile ncfile = NetcdfFile.open(file);
			
			StringWriter sw = new StringWriter();
			NCdumpW.print(file, sw);
			System.out.println(sw);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
