package steve.test;

import java.io.IOException;
import java.net.URL;

import ftp.FtpException;
import gov.noaa.ncdc.wct.io.DirectoryScanner;
import gov.noaa.ncdc.wct.io.DirectoryScanner.ListResults;

public class DataCollectionBuilder {

	public static void main(String[] args) {
		try {
			buildFromHttpDir();
			
		} catch (FtpException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void buildFromHttpDir() throws FtpException, IOException {
		
//		URL url = new URL("http://mrms.ncep.noaa.gov/data/2D/");
//		URL url = new URL("http://mrms.ncep.noaa.gov/data/2D/ALASKA");
//		URL url = new URL("http://mrms.ncep.noaa.gov/data/2D/CARIB");
//		URL url = new URL("http://mrms.ncep.noaa.gov/data/2D/GUAM");
//		URL url = new URL("http://mrms.ncep.noaa.gov/data/2D/HAWAII");
		URL url = new URL("http://mrms.ncep.noaa.gov/data/3DReflPlus/");
			
		DirectoryScanner scanner = new DirectoryScanner();
		ListResults results = scanner.listURL(url);
		System.out.println(results.getCount() + " results found...");
		for (URL u : results.getUrlList()) {

//			<entry name="BrightBandBottomHeight" type="DIRECT_URL"
//					location="http://mrms.ncep.noaa.gov/data/2D/BrightBandBottomHeight/" />

			String name = u.getFile().split("/")[u.getFile().split("/").length-1];
			
			System.out.println("            <entry name=\""+name+"\" type=\"REMOTE_DIR\" location=\""+u+"\" />");
		}
	}
}
