package gov.noaa.ncdc.wct.ui;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.SchemaException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CDOServicesSupport {

	public final static String TOKEN = "qxRPadRTvthzHgpKBOhRRRXHPfQtODKi";	
	public final static String BASE_URL = "http://www.ncdc.noaa.gov/cdo-web/api/v2/";
	public final static int LIMIT = 100;
	
	public final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private JSONParser jsonParser = new JSONParser();
	private FeatureType schema = null;
	private GeometryFactory geoFactory = new GeometryFactory();
	
	public CDOServicesSupport() throws FactoryConfigurationError, SchemaException {
		init();
	}
	
	private void init() throws FactoryConfigurationError, SchemaException {
		schema = FeatureTypeFactory.newFeatureType(StationPointProperties.POINT_ATTRIBUTES, "Station/Point Attributes");
	}

	
	/**
	 * /stations?extent=47.0204,-122.3047,47.9139,-122.0065&startdate=2012-03-01&enddate=2013-03-01&limit=300
	 * 
	 * {
     * "results": [
     *    {
     *        "id": "COOP:454169",
     *        "elevation": 9.1,
     *        "name": "KENT, WA US",
     *        "elevationUnit": "METERS",
     *        "datacoverage": 0.9193,
     *        "longitude": -122.2433,
     *        "mindate": "1919-01-01",
     *        "latitude": 47.4172,
     *        "maxdate": "2012-11-01"
     *    },
     *    {"results":[
     *      {
     *        "id":"GHCND:US1WAPR0020",
	 *        "name":"PUYALLUP 2.1 ESE, WA US",
	 *        "elevation":125.3,
	 *        "elevationUnit":"METERS",
	 *        "longitude":-122.244,
	 *        "datacoverage":1,
	 *        "latitude":47.1683,
	 *        "mindate":"2008-06-01",
	 *        "maxdate":"2013-10-23"
	 *      },
	 *      {
	 *        "id":"GHCND:US1WAPR0034",
	 *        "name":"EDGEWOOD 2.5 SE, WA US",
	 *        "elevation":108.5,
	 *        "elevationUnit":"METERS",
	 *        "longitude":-122.256,
	 *        "datacoverage":1,
	 *        "latitude":47.211,
	 *        "mindate":"2008-09-01",
	 *        "maxdate":"2013-10-23"
	 *      },
	 *      {
	 *        "id":"GHCND:USC00455224",
	 *        "name":"MC MILLIN RESERVOIR, WA US",
	 *        "elevation":176.5,
	 *        "elevationUnit":"METERS",
	 *        "longitude":-122.2558,
	 *        "datacoverage":0.9977,
	 *        "latitude":47.1358,
	 *        "mindate":"1941-03-01",
	 *        "maxdate":"2013-10-15"
	 *      }
	 *    ],
	 *    "metadata":
	 *      {
	 *        "resultset":
	 *          {
	 *            "limit":100,
	 *            "count":3,
	 *            "offset":1
	 *          }
	 *      }
	 *  }
     *   
	 * @param fc
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public void getStationFeatures(FeatureCollection fc, Rectangle2D extent, Date date) throws IOException, ParseException {
		
		String dateString = "startdate="+sdf.format(date)+"&enddate="+sdf.format(date);
		String extentString = "extent="+extent.getMinY()+","+extent.getMinX()+
				","+extent.getMaxY()+","+extent.getMaxX();
		String dataset = "datasetid=GHCND";
		
		String queryString = "?"+dataset+"&"+extentString+"&"+dateString+"&limit="+LIMIT;
		
		URL url = new URL(BASE_URL+"/stations"+queryString);
		URLConnection conn = url.openConnection();
		conn.addRequestProperty("token", TOKEN);
		InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		Object obj = jsonParser.parse(new BufferedReader(reader));
		reader.close();
		
		System.out.println(obj.toString());
//		System.out.println(obj.getClass());
		JSONObject jobj = (JSONObject)obj;		

		JSONArray resultsJson = (JSONArray)jobj.get("results");
		System.out.println(resultsJson.toJSONString());
		System.out.println(resultsJson.getClass());
		
		JSONObject metadataJson = (JSONObject)jobj.get("metadata");
		System.out.println(metadataJson.toJSONString());
		System.out.println(metadataJson.getClass());

		
		
		ArrayList<Feature> tmpList = new ArrayList<Feature>(resultsJson.size());
		tmpList.ensureCapacity(resultsJson.size());

		System.out.println("tmpList.size(): "+tmpList.size()+" stationList.size():"+resultsJson.size());

//		AttributeTypeFactory.newAttributeType("geom", Geometry.class),
//		AttributeTypeFactory.newAttributeType("station_id", String.class),
//		AttributeTypeFactory.newAttributeType("station_desc", String.class),
//		AttributeTypeFactory.newAttributeType("lat", Double.class),
//		AttributeTypeFactory.newAttributeType("lon", Double.class),
//		AttributeTypeFactory.newAttributeType("alt", Double.class),
//		AttributeTypeFactory.newAttributeType("value", Double.class),
//		AttributeTypeFactory.newAttributeType("label", String.class)
		
		int geoIndex = 0;
		for (int n=0; n<resultsJson.size(); n++) {

			try {
				
				JSONObject o = (JSONObject)resultsJson.get(n);
//				System.out.println(o.getClass() + " , "+o.toString());

				Feature feature = schema.create(new Object[] {
						geoFactory.createPoint(new Coordinate(Double.parseDouble(o.get("longitude").toString()), 
								Double.parseDouble(o.get("latitude").toString()))),
						o.get("id").toString(), 
						o.get("name").toString(), 
						new Double(Double.parseDouble(o.get("latitude").toString())),
						new Double(Double.parseDouble(o.get("longitude").toString())),
						new Double(Double.parseDouble(o.get("elevation").toString())), new Double(-1.0), ""
				}, new Integer(geoIndex++).toString());

				tmpList.add(feature);

//							System.out.println(feature);

			} catch (Exception e) {
				e.printStackTrace();
//				JOptionPane.showMessageDialog(this, "Error adding station: "+station.getName()+" ("+station.getDescription()+")");
				return;
			}
		}
		
		
		// sort the feature list
		Collections.sort(tmpList, new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				return f1.getAttribute("station_desc").toString().compareTo(f2.getAttribute("station_desc").toString());
			}
			
		});

		for (Feature f : tmpList) {
			System.out.println(f);
		}

		fc.addAll(tmpList);
				

	}
	
}
