package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.filter.IllegalFilterException;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapLayer;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import gov.noaa.ncdc.wct.WCTUtils;

public class SpcStormReports {
	
	public static final String SPC_ROOT = "http://www.spc.noaa.gov/climo/reports";
	public static enum Type { HAIL, TORNADO, WIND };
	
	private FeatureType schema = null;
	private GeometryFactory geoFactory = new GeometryFactory();
	
	private FeatureCollection fcHail = FeatureCollections.newCollection();
	private FeatureCollection fcWind = FeatureCollections.newCollection();
	private FeatureCollection fcTorn = FeatureCollections.newCollection();
	
	private boolean labelEnabled = true;
	
    private final static Font[] fontArray = new Font[]{
        new Font("Arial", Font.PLAIN, 10),
        new Font("Arial", Font.PLAIN, 11),
        new Font("Arial", Font.PLAIN, 12),
        new Font("Arial", Font.PLAIN, 14),
        new Font("Arial", Font.PLAIN, 16)
    };
    
	public static final AttributeType[] ATTRIBUTES = {
		AttributeTypeFactory.newAttributeType("location", String.class, true, 20),
		AttributeTypeFactory.newAttributeType("time", String.class, true, 20),
		AttributeTypeFactory.newAttributeType("size", String.class, true, 10),
		AttributeTypeFactory.newAttributeType("narrative", String.class, true, 200),
		AttributeTypeFactory.newAttributeType("geom", Geometry.class)
	};
	
	
	
	

	public SpcStormReports() throws FactoryConfigurationError, SchemaException {
		init();
	}
	
	private void init() throws FactoryConfigurationError, SchemaException {
		this.schema = FeatureTypeFactory.newFeatureType(
			ATTRIBUTES, "SPC Storm Reports Attributes");
	}

	public void loadReports(String yyyymmdd) 
		throws IOException, NumberFormatException, IllegalAttributeException, IllegalFilterException {
		
		String yymmdd = yyyymmdd.substring(2);
//		http://www.spc.noaa.gov/climo/reports/151224_rpts.html
		URL url = new URL(SPC_ROOT + "/" + yymmdd + "_rpts.csv");
		
//		File file = new File("C:\\work\\spc\\120229_rpts_filtered_hail.csv");

		//	    	Time,Size,Location,County,State,Lat,Lon,Comments
		//	    	1220,125,SHANNON,RANDOLPH,AR,36.21,-90.96,(MEG)
		//	    	1245,100,5 N HANSON,HOPKINS,KY,37.49,-87.47,QUARTER SIZE HAIL REPORTED. (PAH)
		//	    	1250,150,1 NNW STRAWBERRY,LAWRENCE,AR,35.98,-91.33,(MEG)

		getFcHail().clear();
		getFcWind().clear();
		getFcTorn().clear();

		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		String str;
		String[] headerCols;
		int latIndex = 0;
		int lonIndex = 0;
		int geoIndex = 0;

		Type type = null;

		while ((str = br.readLine()) != null) {
			System.out.println(str);
			
			String[] cols = str.split(",");

			if (str.startsWith("Time")) {
//				System.out.println("HEADER: "+str);
				
				headerCols = str.split(",");
				List headerList = Arrays.asList(cols);
				latIndex = headerList.indexOf("Lat");
				lonIndex = headerList.indexOf("Lon");
				
				if (str.startsWith("Time,F_Scale")) {
					type = Type.TORNADO;
				}
				else if (str.startsWith("Time,Speed")) {
					type = Type.WIND;
				}
				else if (str.startsWith("Time,Size")) {
					type = Type.HAIL;
				}
				continue;
			}
//			System.out.println(cols[latIndex]+" , "+cols[lonIndex]);
			
			try {
				
				if (type == Type.TORNADO) {
					Feature feature = schema.create(
							new Object[] { 
									cols[2], 
									cols[0]+" GMT", 
									cols[1], 
									cols[7],
									geoFactory.createPoint(new Coordinate(
											Double.parseDouble(cols[lonIndex]), 
											Double.parseDouble(cols[latIndex])))
									},
							new Integer(geoIndex++).toString()
							);
					getFcTorn().add(feature);
				}
				else if (type == Type.HAIL) {
					Feature feature = schema.create(
							new Object[] { 
									cols[2], 
									cols[0]+" GMT",
									WCTUtils.DECFMT_0D00.format(Double.parseDouble(cols[1])/100.0), 
									cols[7],
									geoFactory.createPoint(new Coordinate(
											Double.parseDouble(cols[lonIndex]), 
											Double.parseDouble(cols[latIndex])))
									},
							new Integer(geoIndex++).toString()
							);
					getFcHail().add(feature);
				}
				else if (type == Type.WIND) {
					Feature feature = schema.create(
							new Object[] {
									cols[2], 
									cols[0]+" GMT",
									cols[1], 
									cols[7],
									geoFactory.createPoint(new Coordinate(
											Double.parseDouble(cols[lonIndex]), 
											Double.parseDouble(cols[latIndex])))
							},
							new Integer(geoIndex++).toString()
							);
					getFcWind().add(feature);
				}
				else {
					System.err.println("Unrecogized event type data: "+str);
				}

			} catch (Exception e) {
				System.err.println(e.getMessage() + " .... failed to parse correctly: "+str);
			}
		}
		br.close();

	}


	public MapLayer getHailMapLayer() throws IllegalFilterException {
		return new DefaultMapLayer(getFcHail(), getDefaultStyle(Type.HAIL));
	}
	public MapLayer getWindMapLayer() throws IllegalFilterException {
		return new DefaultMapLayer(getFcWind(), getDefaultStyle(Type.WIND));
	}
	public MapLayer getTornadoMapLayer() throws IllegalFilterException {
		return new DefaultMapLayer(getFcTorn(), getDefaultStyle(Type.TORNADO));
	}
	
	
	
	
	public Style getDefaultStyle(Type type) throws IllegalFilterException {
        StyleBuilder sb = new StyleBuilder();
        Style style = sb.createStyle();
        Color defaultColor = Color.WHITE;
        if (type == Type.WIND) {
        	defaultColor = Color.BLUE;
        }
        else if (type == Type.HAIL) {
        	defaultColor = Color.GREEN;
        }
        else if (type == Type.TORNADO) {
        	defaultColor = Color.RED;
        }

        Mark mark1 = sb.createMark(StyleBuilder.MARK_CIRCLE, Color.BLACK, Color.BLACK, 7);
        Graphic gr1 = sb.createGraphic(null, mark1, null);
        PointSymbolizer pntSymbolizer1 = sb.createPointSymbolizer(gr1);

        Mark mark2 = sb.createMark(StyleBuilder.MARK_CIRCLE, defaultColor, defaultColor, 5);
        Graphic gr2 = sb.createGraphic(null, mark2, null);
        PointSymbolizer pntSymbolizer2 = sb.createPointSymbolizer(gr2);

        Mark mark3 = sb.createMark(StyleBuilder.MARK_CIRCLE, Color.WHITE, Color.WHITE, 1);
        Graphic gr3 = sb.createGraphic(null, mark3, null);
        PointSymbolizer pntSymbolizer3 = sb.createPointSymbolizer(gr3);

        // 0 = full declutter, 360 = declutter without repositioning, 720 = no declutter
        double rotation = 0;
        org.geotools.styling.Font font = sb.createFont(fontArray[0]);
        TextSymbolizer textSymbolizer1 = sb.createTextSymbolizer(Color.white, font, "location");
        textSymbolizer1.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 10.0, -10.0, rotation));
        textSymbolizer1.setHalo(sb.createHalo(Color.BLACK, .7, 2.2));
        TextSymbolizer textSymbolizer2 = sb.createTextSymbolizer(Color.white, font, "time");
        textSymbolizer2.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 10.0, 0.0, rotation));
        textSymbolizer2.setHalo(sb.createHalo(Color.BLACK, .7, 2.2));
        TextSymbolizer textSymbolizer3 = sb.createTextSymbolizer(Color.white, font, "size");
        textSymbolizer3.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 10.0, 10.0, rotation));
        textSymbolizer3.setHalo(sb.createHalo(Color.BLACK, .7, 2.2));
        TextSymbolizer textSymbolizer4 = sb.createTextSymbolizer(Color.white, font, "narrative");
        textSymbolizer4.setLabelPlacement(sb.createPointPlacement(0.0, 0.0, 10.0, 20.0, rotation));
        textSymbolizer4.setHalo(sb.createHalo(Color.BLACK, .7, 2.2));

        style.addFeatureTypeStyle(sb.createFeatureTypeStyle("SPC Storm Reports Attributes", 
                new Symbolizer[] { pntSymbolizer1, pntSymbolizer2, pntSymbolizer3 }));
        
        if (isLabelEnabled()) {
        	style.addFeatureTypeStyle(sb.createFeatureTypeStyle("SPC Storm Reports Attributes", 
        			new Symbolizer[] { textSymbolizer1 }, 30, 5000000));
        	style.addFeatureTypeStyle(sb.createFeatureTypeStyle("SPC Storm Reports Attributes", 
        			new Symbolizer[] { textSymbolizer2 }, 400, 5000000));
        	style.addFeatureTypeStyle(sb.createFeatureTypeStyle("SPC Storm Reports Attributes", 
        			new Symbolizer[] { textSymbolizer3 }, 400, 5000000));
        	style.addFeatureTypeStyle(sb.createFeatureTypeStyle("SPC Storm Reports Attributes", 
        			new Symbolizer[] { textSymbolizer4 }, 1000, 5000000));
        }
        
        return style;         
    }

	public boolean isLabelEnabled() {
		return labelEnabled;
	}

	public void setLabelEnabled(boolean labelEnabled) {
		this.labelEnabled = labelEnabled;
	}

	public FeatureCollection getFcHail() {
		return fcHail;
	}

	public FeatureCollection getFcWind() {
		return fcWind;
	}

	public FeatureCollection getFcTorn() {
		return fcTorn;
	}

	public String getInfo() {
		
		return "The NOAA/SPC reports are preliminary and are plotted and listed "
				+ "\"as is\" from NWS Local Storm Reports usually sent in realtime. \n"
				+ "Consult the NOAA Storm Events Database to obtain official documentation of "
				+ "verified reports of severe weather. ";
	}

}
