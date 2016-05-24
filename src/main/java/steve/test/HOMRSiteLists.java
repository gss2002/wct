package steve.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import gov.noaa.ncdc.wct.decoders.StreamingProcessException;
import gov.noaa.ncdc.wct.decoders.nexrad.RadarHashtables;
import gov.noaa.ncdc.wct.export.vector.StreamingShapefileExport;

public class HOMRSiteLists {

	public static void main(String[] args) {
		
		try {

			processNEXRAD();


		} catch (IOException | FactoryConfigurationError | SchemaException | IllegalAttributeException | StreamingProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void processNEXRAD() throws MalformedURLException, IOException, 
		FactoryConfigurationError, SchemaException, IllegalAttributeException, StreamingProcessException {
		
		

	    AttributeType[] nexradAttributeTypeArray = {
	        AttributeTypeFactory.newAttributeType("geom", Geometry.class),
	        AttributeTypeFactory.newAttributeType("icao", String.class),
	        AttributeTypeFactory.newAttributeType("latitude", Double.class),
	        AttributeTypeFactory.newAttributeType("longitude", Double.class),
	        AttributeTypeFactory.newAttributeType("elev", Integer.class),
	        AttributeTypeFactory.newAttributeType("name", String.class),
	        AttributeTypeFactory.newAttributeType("state", String.class),
	        AttributeTypeFactory.newAttributeType("country", String.class),
	        AttributeTypeFactory.newAttributeType("wban", String.class),
	        AttributeTypeFactory.newAttributeType("ncei_id", Integer.class),
	        AttributeTypeFactory.newAttributeType("county", String.class),
	        AttributeTypeFactory.newAttributeType("timezone", String.class)
	    };
		
		FeatureType nexradSitesSchema = FeatureTypeFactory.newFeatureType(nexradAttributeTypeArray, "Nexrad Site Attributes");
		GeometryFactory geoFactory = new GeometryFactory();
		int geoIndex = 0;
		
		List<String> lines = IOUtils.readLines(new URL("http://www.ncdc.noaa.gov/homr/file/nexrad-stations.txt").openStream());

		StreamingShapefileExport shpExport = new StreamingShapefileExport(new File("C:\\work\\wct\\overlays\\wsr.shp"));
		for (String line : lines) {
			System.out.println(line);
			if (line.startsWith("NCDCID") || line.startsWith("-")) {
				continue;
			}
			
//			0         1         2         3         4         5         6         7         8         9         0          1        2         3         4
//			012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//			30001794 KABR 14929 ABERDEEN                       UNITED STATES        SD BROWN                          45.45583  -98.41306  1302   -6    NEXRAD     
//			NCDCID   ICAO WBAN  NAME                           COUNTRY              ST COUNTY                         LAT       LON        ELEV   UTC   STNTYPE 
			
			Integer nceiID = Integer.parseInt(line.substring(0, 9).trim());
			String icao = line.substring(9, 14).trim();
			String wban = line.substring(14, 20).trim();
			String name = line.substring(20, 51).trim();
			String country = line.substring(51, 72).trim();
			String state = line.substring(72, 75).trim();
			String county = line.substring(75, 106).trim();
			Double lat = Double.parseDouble(line.substring(106, 116).trim());
			Double lon = Double.parseDouble(line.substring(116, 127).trim());
			Integer elev = Integer.parseInt(line.substring(127, 134).trim());
			String timezone = line.substring(134, 140).trim();
			String stationType = line.substring(140).trim();


//	        AttributeTypeFactory.newAttributeType("geom", Geometry.class),
//	        AttributeTypeFactory.newAttributeType("icao", String.class),
//	        AttributeTypeFactory.newAttributeType("latitude", Double.class),
//	        AttributeTypeFactory.newAttributeType("longitude", Double.class),
//	        AttributeTypeFactory.newAttributeType("elev", Integer.class),
//	        AttributeTypeFactory.newAttributeType("name", String.class),
//	        AttributeTypeFactory.newAttributeType("state", String.class),
//	        AttributeTypeFactory.newAttributeType("country", String.class),
//	        AttributeTypeFactory.newAttributeType("wban", String.class),
//	        AttributeTypeFactory.newAttributeType("ncei_id", Integer.class),
//	        AttributeTypeFactory.newAttributeType("county", String.class),
//	        AttributeTypeFactory.newAttributeType("timezone", String.class)
			
			
            Coordinate coord = new Coordinate(lon, lat);
            Feature feature = nexradSitesSchema.create(new Object[] {
                    geoFactory.createPoint(coord),
                    icao, lat, lon, elev, name, state, country, wban, nceiID, county, timezone
            }, new Integer(geoIndex++).toString());

			shpExport.addFeature(feature);
	
			
			// compare to existing entry in wsr.dbf
			double tolerance = 0.00001; // degrees ~ 1m
			double oldLat = RadarHashtables.getSharedInstance().getLat(icao);
			double oldLon = RadarHashtables.getSharedInstance().getLon(icao);
			if (Math.abs(lat-oldLat) > tolerance) {
				System.err.println("Site location change! ::: "+icao+": old lat = "+oldLat+" current HOMR lat = "+lat);
				System.err.println("Site location change! ::: "+icao+": old lon = "+oldLon+" current HOMR lon = "+lon);
			}
			
		}
		shpExport.close();
	}
}
