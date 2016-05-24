package gov.noaa.ncdc.wct.decoders.cdm;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.SchemaException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import gov.noaa.ncdc.wct.WCTFilter;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.DecodeException;
import gov.noaa.ncdc.wct.decoders.DecodeHintNotSupportedException;
import gov.noaa.ncdc.wct.decoders.StreamingDecoder;
import gov.noaa.ncdc.wct.decoders.StreamingProcess;
import gov.noaa.ncdc.wct.decoders.StreamingProcessException;
import gov.noaa.ncdc.wct.event.DataDecodeEvent;
import gov.noaa.ncdc.wct.event.DataDecodeListener;
import ucar.ma2.Array;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.unidata.geoloc.Station;

public class StationPointDecoder implements StreamingDecoder {
	

    private static final Logger logger = Logger.getLogger(StationPointDecoder.class.getName());

    private Array dataCache = null;

    private String source;
    private java.awt.geom.Rectangle2D.Double bounds = null;

    private Vector<DataDecodeListener> listeners = new Vector<DataDecodeListener>();

    private FeatureType schema;
    private Map<String, Object> hintsMap;

    private final GeometryFactory geoFactory = new GeometryFactory();
	public final static AttributeType[] POINT_ATTRIBUTES = {
			AttributeTypeFactory.newAttributeType("geom", Geometry.class),
			AttributeTypeFactory.newAttributeType("station_id", String.class),
			AttributeTypeFactory.newAttributeType("station_desc", String.class),
			AttributeTypeFactory.newAttributeType("lat", Double.class),
			AttributeTypeFactory.newAttributeType("lon", Double.class),
			AttributeTypeFactory.newAttributeType("alt", Double.class),
			AttributeTypeFactory.newAttributeType("value", Double.class),
			AttributeTypeFactory.newAttributeType("label", String.class)
		};
    
	
    
    public StationPointDecoder() throws FactoryConfigurationError, SchemaException {
    	init();
    }
	
    public void addDataDecodeListener(DataDecodeListener l) {
    	listeners.add(l);
    }
    public void removeDataDecodeListener(DataDecodeListener l) {
    	listeners.remove(l);
    }
    

    private void init() throws FactoryConfigurationError, SchemaException {
    	
		schema = FeatureTypeFactory.newFeatureType(POINT_ATTRIBUTES, "Station/Point Attributes");
		
		
        hintsMap = new HashMap<String, Object>();

        // default WCTFilter for Grid data
        WCTFilter filter = new WCTFilter();
        hintsMap.put("gridFilter", filter);

    }


    /**
     * Set a decodeHint.  To get a list of supported hints and default values,
     * use 'getDecodeHints()'.  The currently supported hints are as follows: <br><br>
     * <ol>
     *  <li> <b>attributes (NOT CURRENTLY IMPLEMENTED)</b>: 
     *          AttributeType[] object that determines which set of attributes to produce.  
     *          Use the static arrays in this class - they are the only ones supported.
     *  <li> <b>gridFilter</b>: 
     *          WCTFilter object that defines filtering options on range, azimuth, 
     *          height and geographic bounds.
     * @param hintsMap
     */
    public void setDecodeHint(String hintKey, Object hintValue) throws DecodeHintNotSupportedException {
        if (! hintsMap.keySet().contains(hintKey)) {
            throw new DecodeHintNotSupportedException(this.getClass().toString(), hintKey, hintsMap);
        }

        hintsMap.put(hintKey, hintValue);
    }

    /**
     * Get the key-value pairs for the current decode hints.  
     * If no hints have been set, this will return the supported
     * hints with default values.
     * @return
     */
    public Map<String, Object> getDecodeHints() {
        return hintsMap;
    }

    public void setSource(String source) {
        this.source = source;
    }


    

	@Override
	public FeatureType[] getFeatureTypes() {
		return new FeatureType[] { schema };
	}

	@Override
	public void decodeData(StreamingProcess[] streamingProcessArray) throws DecodeException, IOException {
	
        DataDecodeEvent event = new DataDecodeEvent(this);
        try {

        	// Start decode
        	// --------------
        	for (int i = 0; i < listeners.size(); i++) {
        		event.setProgress(0);
        		listeners.get(i).decodeStarted(event);
        	}


        	Formatter fmter = new Formatter();
        	FeatureDataset fd = FeatureDatasetFactoryManager.open(null, source, WCTUtils.getSharedCancelTask(), fmter);
        	FeatureDatasetPoint fdp = null;
        	if (fd != null && fd.getFeatureType().isPointFeatureType()) {
        		fdp = (FeatureDatasetPoint)fd;
        	}
        	List<ucar.nc2.ft.FeatureCollection> pfcList = fdp.getPointFeatureCollectionList();
        	StationTimeSeriesFeatureCollection stsfc = (StationTimeSeriesFeatureCollection)(pfcList.get(0));
        	List<Station> stationList = stsfc.getStations();

        	int geoIndex = 0;
        	for (Station station : stationList) {

        		try {

        			Feature feature = schema.create(new Object[] {
        					geoFactory.createPoint(new Coordinate(station.getLongitude(), station.getLatitude())),
        					station.getName(), 
        					station.getDescription(), 
        					new Double(station.getLatitude()), new Double(station.getLongitude()),
        					new Double(station.getAltitude()), new Double(-1.0), ""
        			}, new Integer(geoIndex++).toString());

        			for (StreamingProcess process : streamingProcessArray) {
        				process.addFeature(feature);
        			}

        			//			System.out.println(feature);

        		} catch (Exception e) {
        			e.printStackTrace();
        			System.err.println("Error adding station: "+station.getName()+" ("+station.getDescription()+")");
        			return;
        		}
        		
                for (int i = 0; i < listeners.size(); i++) {
                    event.setProgress((int)(100*geoIndex/(double)stationList.size()));
                    listeners.get(i).decodeProgress(event);
                }
        	}

        	fd.close();
        	
        	for (int n=0; n<streamingProcessArray.length; n++) {
        		streamingProcessArray[n].close();
        	}
        
        } catch (StreamingProcessException spe) {
        	spe.printStackTrace();
        } finally {
            for (int i = 0; i < listeners.size(); i++) {
                event.setProgress(0);
                listeners.get(i).decodeEnded(event);
            }
        }
		
	}

}
