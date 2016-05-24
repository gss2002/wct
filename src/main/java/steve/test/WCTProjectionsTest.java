package steve.test;

import gov.noaa.ncdc.wct.decoders.nexrad.WCTProjections;

import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CoordinateSystemFactory;
import org.geotools.cs.GeodeticCalculator;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.ct.MathTransform;
import org.geotools.pt.CoordinatePoint;
import org.opengis.referencing.FactoryException;

import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;

public class WCTProjectionsTest {



	// Factory to create coordinate systems from WKT strings
	private static final CoordinateSystemFactory csFactory = CoordinateSystemFactory.getDefault();

	// Factory to create transformations from a source and target CS 
	private static final CoordinateTransformationFactory ctFactory = CoordinateTransformationFactory.getDefault();

	public static CoordinateSystem getRadarCoordinateSystemAlbers(double lon, double lat) 
			throws FactoryException, CannotCreateTransformException {


		String wsrWKT = "PROJCS[\"Albers_Conic_Equal_Area\",GEOGCS[\"NAD83\","+
				"DATUM[\"NAD83\",SPHEROID[\"GRS_1980\",6378137.0,298.25722210100002],TOWGS84[0,0,0,0,0,0,0]],"+
				"PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.017453292519943295]],"+
				"PROJECTION[\"Albers_Conic_Equal_Area\"],"+
				"PARAMETER[\"false_easting\",0.0],PARAMETER[\"false_northing\",0.0],"+
				"PARAMETER[\"central_meridian\","+lon+"],"+
				"PARAMETER[\"standard_parallel_1\","+(lat-1.0)+"],"+
				"PARAMETER[\"standard_parallel_2\","+(lat+1.0)+"],"+
				"PARAMETER[\"latitude_of_origin\","+lat+"],UNIT[\"metre\",1.0]]";

		return csFactory.createFromWKT(wsrWKT);


	}
	
	public static CoordinateSystem getRadarCoordinateSystemAzimuthalEquidistant(double lon, double lat) 
			throws FactoryException, CannotCreateTransformException {


		String wsrWKT = "PROJCS[\"Azimuthal_Equidistant\",GEOGCS[\"NAD83\","+
				"DATUM[\"NAD83\",SPHEROID[\"GRS_1980\",6378137.0,298.25722210100002],TOWGS84[0,0,0,0,0,0,0]],"+
				"PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.017453292519943295]],"+
				"PROJECTION[\"Azimuthal_Equidistant\"],"+
				"PARAMETER[\"false_easting\",0.0],PARAMETER[\"false_northing\",0.0],"+
				"PARAMETER[\"central_meridian\","+lon+"],"+
				"PARAMETER[\"latitude_of_origin\","+lat+"],UNIT[\"metre\",1.0]]";

		return csFactory.createFromWKT(wsrWKT);


	}
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		try {
			
			double lat = 45.0;
			double lon = -90.0;
			double xInKM = 230;
			double yInKM = 0;
			
//			RadarMapProjection idvProj = new RadarMapProjection(lat, lon);
//			double[][] idvCoords = idvProj.toReference(new double[][] { new double[] { yInKM }, new double[] { xInKM } });
//			System.out.println(Arrays.deepToString(idvCoords));
//			
			GeodeticCalculator geocalc = new GeodeticCalculator();
//			
//	        FlatEarth trans = new FlatEarth(lat, lon);
//	        LatLonPointImpl llPointFlatEarth = trans.projToLatLon(xInKM, yInKM);
//	        
//	        System.out.println("CDM Flat Earth: "+llPointFlatEarth.getLongitude() + ", "+llPointFlatEarth.getLatitude());
//
//	        
//	        EquidistantAzimuthalProjection eaproj = new EquidistantAzimuthalProjection(lat, lon, 0, 0, new Earth());
//	        LatLonPointImpl llPointEAP = eaproj.projToLatLon(xInKM, yInKM);
//	        
//	        System.out.println("CDM EquidistantAzimuthal Spherical: "+llPointEAP.getLongitude() + ", "+llPointEAP.getLatitude());

	        

//Ellipsoid	Semimajor axis	Semiminor axis	Inverse flattening
//GRS 80	6,378,137 m	6,356,752.3141 m	298.257222101
	        

	        EquidistantAzimuthalProjection eaproj2 = new EquidistantAzimuthalProjection(lat, lon, 0, 0, new Earth(6378137, 6356752.3141, 298.257222101));
	        LatLonPointImpl llPointEAP_NAD83 = (LatLonPointImpl)eaproj2.projToLatLon(xInKM, yInKM);
	        
	        System.out.println("CDM EquidistantAzimuthal NAD83: "+llPointEAP_NAD83.getLongitude() + ", "+llPointEAP_NAD83.getLatitude());

	        
	        
			
			CoordinateSystem csAlbers = getRadarCoordinateSystemAlbers(lon, lat);
			CoordinateSystem csAzimuthal = getRadarCoordinateSystemAzimuthalEquidistant(lon, lat);
			
            CoordinateSystem outCS = csFactory.createFromWKT(WCTProjections.WGS84_WKT);      
			
            // Create transformation to convert from WSR Projection to WGS84      
            CoordinateTransformation transformationAlbers = ctFactory.createFromCoordinateSystems(csAlbers, outCS);
//            CoordinateTransformation transformationAzimuthal = ctFactory.createFromCoordinateSystems(csAzimuthal, outCS);
            
            MathTransform mtAlbers = transformationAlbers.getMathTransform();
//            MathTransform mtAzimuthal = transformationAzimuthal.getMathTransform();

            {
            	CoordinatePoint ptSrc = new CoordinatePoint(xInKM*1000, yInKM*1000);
            	CoordinatePoint ptDst = mtAlbers.transform(ptSrc, null);
            	System.out.println("Albers: "+ptDst);
            	
            	
//            	geocalc.setAnchorPoint(llPointFlatEarth.getLongitude(), llPointFlatEarth.getLatitude());
//            	geocalc.setDestinationPoint(ptDst.getOrdinate(0), ptDst.getOrdinate(1));
//            	System.out.println("\ndistance between albers and flat earth: "+geocalc.getOrthodromicDistance());
//            	
//            	geocalc.setDestinationPoint(llPointEAP.getLongitude(), llPointEAP.getLatitude());
//            	System.out.println("distance between azimuthal and flat earth: "+geocalc.getOrthodromicDistance());
//
//            	geocalc.setAnchorPoint(llPointEAP.getLongitude(), llPointEAP.getLatitude());
//            	geocalc.setDestinationPoint(ptDst.getOrdinate(0), ptDst.getOrdinate(1));
//            	System.out.println("distance between azimuthal and albers: "+geocalc.getOrthodromicDistance());

            	geocalc.setAnchorPoint(llPointEAP_NAD83.getLongitude(), llPointEAP_NAD83.getLatitude());
            	geocalc.setDestinationPoint(ptDst.getOrdinate(0), ptDst.getOrdinate(1));
            	System.out.println("distance between azimuthal nad83 and albers: "+geocalc.getOrthodromicDistance());

            }
            
            
//            {
//            	CoordinatePoint ptSrc = new CoordinatePoint(230000, 0);
//            	CoordinatePoint ptDst = mtAzimuthal.transform(ptSrc, null);
//            	System.out.println("Azimuthal: "+ptDst);
//            }
            
            

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}
}
