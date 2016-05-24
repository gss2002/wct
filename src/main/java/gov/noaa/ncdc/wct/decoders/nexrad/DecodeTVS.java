/**
 * NOAA's National Climatic Data Center
 * NOAA/NESDIS/NCDC
 * 151 Patton Ave, Asheville, NC  28801
 * 
 * THIS SOFTWARE AND ITS DOCUMENTATION ARE CONSIDERED TO BE IN THE 
 * PUBLIC DOMAIN AND THUS ARE AVAILABLE FOR UNRESTRICTED PUBLIC USE.  
 * THEY ARE FURNISHED "AS IS." THE AUTHORS, THE UNITED STATES GOVERNMENT, ITS
 * INSTRUMENTALITIES, OFFICERS, EMPLOYEES, AND AGENTS MAKE NO WARRANTY,
 * EXPRESS OR IMPLIED, AS TO THE USEFULNESS OF THE SOFTWARE AND
 * DOCUMENTATION FOR ANY PURPOSE. THEY ASSUME NO RESPONSIBILITY (1)
 * FOR THE USE OF THE SOFTWARE AND DOCUMENTATION; OR (2) TO PROVIDE
 * TECHNICAL SUPPORT TO USERS.
 */

package gov.noaa.ncdc.wct.decoders.nexrad;

import gov.noaa.ncdc.wct.decoders.DecodeException;
import gov.noaa.ncdc.wct.decoders.DecodeHintNotSupportedException;
import gov.noaa.ncdc.wct.decoders.MaxGeographicExtent;
import gov.noaa.ncdc.wct.decoders.StreamingProcess;
import gov.noaa.ncdc.wct.decoders.StreamingProcessException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geotools.ct.MathTransform;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.pt.CoordinatePoint;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


/**
 *  Decodes NTV NEXRAD Level-III Tornado Vortex Signature alphanumeric product.  
 *  
 *  From 2620003J.pdf 21.1: 
 *  
 *  "This product shall provide information regarding the existence and location 
 *  of an identified Tornado Vortex Signature (TVS). This product shall be 
 *  produced from the output of the Tornado Detection Algorithm. The product 
 *  shall produce an alphanumeric tabular display and a graphic overlay of the 
 *  algorithm output data for each identified TVS (and Elevated TVS (ETVS)) 
 *  signature information when such is identified. This product shall be updated 
 *  once per volume scan time. This product shall include annotations for the 
 *  product name, radar ID, time and date of volume scan, radar position, radar 
 *  elevation above MSL, and radar operational mode. Upon user request, all site 
 *  adaptable parameters identified as inputs to the algorithm(s) used to generate 
 *  data for this product shall be available at the alphanumeric display."
 *  
 */
public class DecodeTVS implements DecodeL3Alpha {

    private static final Logger logger = Logger.getLogger(DecodeTVS.class.getName());

    private String[] metaLabelString = new String[3];
    private FeatureCollection features = FeatureCollections.newCollection();
    private FeatureType schema = null;
    private GeometryFactory geoFactory = new GeometryFactory();
    private java.awt.geom.Rectangle2D.Double wsrBounds;

    private HashMap<String, Object> decodeHints = new HashMap<String, Object>();

    private DecodeL3Header header;
    private String[] supplementalData = new String[2];

    private WCTProjections nexradProjection = new WCTProjections();
    private String datetime;


    /** 
     * Constructor
     * @param url Input Raw DPA URL
     * Will produce a PolygonLayer in Lat/Lon projection
     * @throws IOException 
     */
    public DecodeTVS (DecodeL3Header header) throws DecodeException, IOException {
        this.header = header;
        this.wsrBounds = getExtent();
        decodeData();
    }

    /**
     * Returns the feature types used for these features
     * 
     * @return The featureType value
     */
    public FeatureType[] getFeatureTypes() {
        return new FeatureType[] {schema};
    }

    /**
     * Returns the LineFeatureType -- Always null in this case
     *
     * @return    Always null for this alphanumeric product
     */
    public FeatureType getLineFeatureType() {
        return null;
    }


    /**
     * Returns Rectangle.Double Bounds for the NEXRAD Site calculated during decode. (unique to product)
     * Could be 248, 124 or 32 nmi.
     *
     * @return    The nexradExtent value
     */
    public java.awt.geom.Rectangle2D.Double getNexradExtent() {
        return wsrBounds;
    }

    /**
     * Returns default display symbol type
     *
     * @return    The symbol type
     */
    public String getDefaultSymbol() {
        return org.geotools.styling.StyleBuilder.MARK_CIRCLE;
    }


    /** 
     * Returns the specified supplemental text data (index=1 == Block2, index2 == Block3, etc...)  
     */
    public String getSupplementalData(int index) {
        if (supplementalData[index] != null) {           
            return supplementalData[index];
        }
        else {
            return new String("NO DATA");
        }
    }

    /** 
     * Returns the supplemental text data array  
     */
    public String[] getSupplementalDataArray() {
        return supplementalData;
    }


    public String getMetaLabel(int index) {
        if (metaLabelString[index] == null) {
            return "";
        }
        else {
            return metaLabelString[index];
        }
    }

    private void makeMetaLabelStrings() {

        FeatureIterator fi = features.features();
        if (fi.hasNext()) { // only use first and thus strongest reading
            Feature f = fi.next();
            metaLabelString[0] = "MAX ID: " + f.getAttribute("id").toString().trim();
            metaLabelString[1] = "BASE / TOP: " + f.getAttribute("base").toString().trim() + "/" +
            f.getAttribute("top").toString().trim() + " (kft)";
            metaLabelString[2] = "MAX SHEAR: " + f.getAttribute("mxshr").toString().trim() + " (E-3/s)";
        }
        else {
            metaLabelString[0] = "NO TVS PRESENT";
        }

    }


//    @Override
    public Map<String, Object> getDecodeHints() {
        return decodeHints;
    }

//    @Override
    public void setDecodeHint(String hintKey, Object hintValue)
    throws DecodeHintNotSupportedException {
        throw new DecodeHintNotSupportedException("DecodeTVS", hintKey, decodeHints);

    }


    /**
     * Decodes data and stores with in-memory FeatureCollection
     * @return
     * @throws DecodeException
     */
//    @Override
    public void decodeData() throws DecodeException, IOException {
        features.clear();

        StreamingProcess process = new StreamingProcess() {
            public void addFeature(Feature feature)
            throws StreamingProcessException {
                features.add(feature);
            }
            public void close() throws StreamingProcessException {
                logger.info("STREAMING PROCESS close() ::: features.size() = "+features.size());
            }           
        };

        decodeData(new StreamingProcess[] { process } );    
    }


//    @Override
    public void decodeData(StreamingProcess[] processArray)
    throws DecodeException, IOException {

        decodeData(processArray, true);

    }





//    @Override
    public void decodeData(StreamingProcess[] processArray, boolean autoClose)
    throws DecodeException, IOException {


        //logger.info("DECODING TVS DATA TO LATLON PROJECTION");
        // New GeoTools based projection transformations
        MathTransform nexradTransform;
        try {
            // Use Geotools Proj4 implementation to get MathTransform object
            nexradTransform = nexradProjection.getRadarTransform(header);
        } catch (Exception e) {
            throw new DecodeException("PROJECTION TRANSFORM ERROR", header.getDataURL());
        }


        datetime = header.getDate()+header.getHourString()+header.getMinuteString()+header.getSecondString();


        // Set up attribute table
        try {
            AttributeType geom = AttributeTypeFactory.newAttributeType("geom", Point.class);
            AttributeType wsrid = AttributeTypeFactory.newAttributeType("wsrid", String.class, true, 5);
            AttributeType datetime = AttributeTypeFactory.newAttributeType("datetime", String.class, true, 15);
            AttributeType lat = AttributeTypeFactory.newAttributeType("lat", Double.class, true, 10);
            AttributeType lon = AttributeTypeFactory.newAttributeType("lon", Double.class, true, 10);
            AttributeType id = AttributeTypeFactory.newAttributeType("id", String.class, true, 3);
            AttributeType type = AttributeTypeFactory.newAttributeType("type", String.class, true, 5);
            AttributeType range = AttributeTypeFactory.newAttributeType("range", Double.class, true, 7);
            AttributeType azim = AttributeTypeFactory.newAttributeType("azim", Double.class, true, 7);
            AttributeType avgdv = AttributeTypeFactory.newAttributeType("avgdv", String.class, true, 5);
            AttributeType lldv = AttributeTypeFactory.newAttributeType("lldv", String.class, true, 5);
            AttributeType mxdv = AttributeTypeFactory.newAttributeType("mxdv", String.class, true, 5);
            AttributeType mxdvhgt = AttributeTypeFactory.newAttributeType("mxdvhgt", String.class, true, 5);
            AttributeType depth = AttributeTypeFactory.newAttributeType("depth", String.class, true, 5);
            AttributeType base = AttributeTypeFactory.newAttributeType("base", String.class, true, 5);
            AttributeType top = AttributeTypeFactory.newAttributeType("top", String.class, true, 5);
            AttributeType mxshr = AttributeTypeFactory.newAttributeType("mxshr", String.class, true, 5);
            AttributeType mxshrhgt = AttributeTypeFactory.newAttributeType("mxshrhgt", String.class, true, 5);
            AttributeType[] attTypes = {geom, wsrid, datetime, lat, lon, id, type, range, azim, avgdv, lldv, mxdv, mxdvhgt, depth, base, top, mxshr, mxshrhgt};
            schema = FeatureTypeFactory.newFeatureType(attTypes, "Tornado Vortex Signature Data");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reset feature index counter
        int geoIndex = 0;


        try {

            // Decode the text blocks (block 2 and 3)
            DecodeL3AlphaGeneric decoder = new DecodeL3AlphaGeneric();
            decoder.decode(header);

            logger.info("----------- VERSION: "+header.getVersion()+" ------------ \n");
            logger.info("----------- BLOCK 2 ----------- \n"+decoder.getBlock2Text());
            logger.info("----------- BLOCK 3 ----------- \n"+decoder.getBlock3Text());


            // Build text for block 2 data           
            StringBuffer sb = new StringBuffer();

            // Lets make a custom legend for block 2
            sb.append("  TVS SUPPLEMENTAL DATA 1\n\n");                  
            sb.append("  ABBREVIATIONS:\n");                  
            sb.append("  AZ    = Azimuth Angle From Radar \n");
            sb.append("          (In Degrees where 0 deg = North, 90 = East, 180 = South, etc...)\n");                  
            sb.append("  RAN   = Range (Distance) From Radar (In Nautical Miles (nmi))\n");                  
            sb.append("  BASE  = Elevation of TVS Base (kft)\n");                  
            sb.append("  TOP   = Elevation of TVS Top (kft)\n");                  
            sb.append("  RAD   = Radius of TVS (nmi)\n");                  
            sb.append("  AZDIA = Radius of TVS (nmi)\n\n");                  

            sb.append(decoder.getBlock2Text());
            supplementalData[0] = sb.toString();
            sb.append("\n\n");

            // Build text for block 3 data
            sb = new StringBuffer();
            sb.append("  TVS SUPPLEMENTAL DATA 2\n\n");                  
            sb.append(decoder.getBlock3Text());
            sb.append("\n\n");
            supplementalData[1] = sb.toString();


            String block3Text = decoder.getBlock3Text();
            String[] lines = block3Text.split("\n");

            if (lines.length == 0) {
                metaLabelString[0] = "NO TVS PRESENT";
                return;      
            }


            if (header.getVersion() > 1.0) {
                throw new DecodeException("UNKNOWN NEXRAD TVS FILE VERSION: " + header.getVersion(), header
                        .getDataURL());
            }

            for (int n=0; n<lines.length; n++) {

                String str = lines[n]; 
                // advance past empty lines
                if (str.trim().length() == 0) {
                    continue;
                }


//              VERSION 0              
//              0         1         2         3         4         5         6         7 
//              012345678901234567890123456789012345678901234567890123456789012345678901234567
//              TVS  MESO  STORM  BASE HGT  AZRAN   MAX SHEAR HGT  AZRAN    SHEAR   ORI   ROT  
//              ID   ID     ID     (KFT)  (DEG-NM)     (KFT)     (DEG-NM) (E-3/S) (DEG) (RAD) 

//              1    1     28      5.7     80/ 65      5.7        80/ 65    35   36.65 .017                
//              012345678901234567890123456789012345678901234567890123456789012345678901234567
//              0         1         2         3         4         5         6         7 

                if (header.getVersion() == 0) {

                    if (str.charAt(32) == '/' && str.charAt(55) == '/') {
                        //logger.info(hitCount+" "+new String(data));

                        double azim = Double.parseDouble(str.substring(29, 32));
                        double range = Double.parseDouble(str.substring(33, 36));

                        if (azim == 0.0 || azim == 180.0 || azim == 360.0) {
                            azim+=0.000001;
                        }
                        // Convert from nautical mi to meters
                        double[] geoXY = (nexradTransform.transform(
                                new CoordinatePoint(
                                        range*Math.sin(Math.toRadians(azim))*1852.0, 
                                        range*Math.cos(Math.toRadians(azim))*1852.0
                                ), null)).getCoordinates();


                        try {
                            // create the feature
                            //{geom, wsrid, datetime, lat, lon, id, type, range, azim, avgdv, lldv, mxdv, mxdvhgt, depth, base, top, mxshr, mxshrhgt};
                            Feature feature = schema.create(
                                    new Object[]{
                                            geoFactory.createPoint(new Coordinate(geoXY[0], geoXY[1])), // geom
                                            header.getICAO(), // wsrid
                                            datetime, 
                                            new Double(geoXY[1]),
                                            new Double(geoXY[0]),
                                            str.substring(2, 4).trim(), // id
                                            str.substring(7, 9).trim(), // type
                                            new Double(range),
                                            new Double(azim),                           
                                            "N/A", // avgdv not defined in version 0 of this product
                                            "N/A", // lldv not defined in version 0 of this product
                                            "N/A", // mxdv not defined in version 0 of this product
                                            "N/A", // mxdvhgt not defined in version 0 of this product
                                            "N/A", // depth not defined in version 0 of this product
                                            str.substring(21, 25).trim(), // base
                                            "N/A", // top not defined in version 0 of this product
                                            str.substring(61, 65).trim(), // mxshr
                                            str.substring(41, 45).trim() // mxshrhgt
                                    },
                                    new Integer(geoIndex++).toString());
                            // add to collection
                            features.add(feature);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }                  

                }
                else {


//                  VERSION 1              
//                  0         1         2         3         4         5         6         7 
//                  012345678901234567890123456789012345678901234567890123456789012345678901234567
//                  Feat  Storm   AZ/RAN  AVGDV  LLDV  MXDV/Hgt   Depth    Base/Top   MXSHR/Hgt    
//                  Type    ID   (deg,nm)  (kt)  (kt)  (kt,kft)   (kft)     (kft)     (E-3/s,kft) 
//                  TVS    J5   202/ 32    37    56    60/ 5.7   > 5.9  < 2.3/  8.2    31/ 5.7


                    if (str.charAt(17) == '/' && str.charAt(39) == '/' &&  
                            str.charAt(59) == '/' && str.charAt(71) == '/') {
                        //logger.info(hitCount+" "+new String(data));

                        double azim = Double.parseDouble(str.substring(14, 17));
                        double range = Double.parseDouble(str.substring(18, 21));

                        if (azim == 0.0 || azim == 180.0 || azim == 360.0) {
                            azim+=0.000001;
                        }
                        // Convert from nautical mi to meters
                        double[] geoXY = (nexradTransform.transform(
                                new CoordinatePoint(
                                        range*Math.sin(Math.toRadians(azim))*1852.0, 
                                        range*Math.cos(Math.toRadians(azim))*1852.0
                                ), null)).getCoordinates();


                        try {
                            // create the feature
                            //{geom, wsrid, datetime, lat, lon, id, type, range, azim, avgdv, lldv, mxdv, mxdvhgt, depth, base, top, mxshr, mxshrhgt};
                            Feature feature = schema.create(
                                    new Object[]{
                                            geoFactory.createPoint(new Coordinate(geoXY[0], geoXY[1])), // geom
                                            header.getICAO(), // wsrid
                                            datetime, // datetime
                                            new Double(geoXY[1]), // lat
                                            new Double(geoXY[0]), // lon
                                            str.substring(9, 11).trim(), // id
                                            str.substring(1, 7).trim(),  // type
                                            new Double(range),
                                            new Double(azim),                           
                                            str.substring(22, 28).trim(),// avgdv
                                            str.substring(29, 34).trim(),// lldv
                                            str.substring(35, 39).trim(),// mxdv
                                            str.substring(40, 45).trim(),// mxdvhgt
                                            str.substring(46, 53).trim(),// depth
                                            str.substring(54, 59).trim(),// base
                                            str.substring(60, 65).trim(),// top
                                            str.substring(66, 71).trim(),// mxshr
                                            str.substring(72, str.length()).trim() // mxshrhgt
                                    },
                                    new Integer(geoIndex++).toString());
                            // add to collection
                            features.add(feature);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }                  
                }             
            }
            makeMetaLabelStrings();
        } // END try
        catch (Exception e) {
            e.printStackTrace();
            throw new DecodeException("DECODE EXCEPTION IN TVS FILE", header.getDataURL());
        }
    } // END METHOD decodeData



    /**
     *  Gets the features attribute of the DecodeHail object
     *
     * @return    The features value
     */
    public FeatureCollection getFeatures() {
        return features;
    }


    /**
     *  Gets the line features attribute of the DecodeTVS object
     *
     * @return    The features value
     */
    public FeatureCollection getLineFeatures() {
        return null;
    }

    /**
     * Implementation of NexradDecoder
     */
    public void setFeatures(FeatureCollection features) {
        this.features = features;
    }


    private java.awt.geom.Rectangle2D.Double getExtent() {
        return (MaxGeographicExtent.getNexradExtent(header.getLat(), header.getLon()));
    }


    public DecodeL3Header getHeader() {
    	return header;
    }
} // END CLASS 
