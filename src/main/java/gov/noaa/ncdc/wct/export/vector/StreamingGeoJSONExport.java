package gov.noaa.ncdc.wct.export.vector;

import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.decoders.StreamingProcess;
import gov.noaa.ncdc.wct.decoders.StreamingProcessException;
import gov.noaa.ncdc.wct.decoders.nexrad.WCTProjections;
import gov.noaa.ncdc.wct.event.GeneralProgressListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;


/**
 *  Export features to geojson text one feature at a time for low memory consumption.
 *
 * @author    steve.ansari
 */
public class StreamingGeoJSONExport implements StreamingProcess {


    // The list of event listeners.
    private Vector<GeneralProgressListener> listeners = new Vector<GeneralProgressListener>();



    private File outfile;
    private BufferedWriter bw;
    private FeatureType featureType;
    private String prj = WCTProjections.NAD83_ESRI_PRJ;
    private boolean firstTime = true;


    /**
     * Constructor 
     *
     * @param  outfile          The destination file. (will create .prj and .csv files)
     * @param  prj              The ESRI projection string for the .prj file.
     * @param  featureType      The FeatureType (schema) to use
     * @exception  IOException  Error when writing file?
     */
    public StreamingGeoJSONExport(File outfile, String prj, FeatureType featureType) throws IOException {
        this.outfile = outfile;
        this.prj = prj;
        this.featureType = featureType;
    }

    /**
     * Constructor 
     *
     * @param  outfile          The destination file. (will create .prj and .csv files)
     * @param  prj              The ESRI projection string for the .prj file.
     * @exception  IOException  Error when writing file?
     */
    public StreamingGeoJSONExport(File outfile, String prj) throws IOException {
        this.outfile = outfile;
        this.prj = prj;
    }



    /**
     * Constructor for using default WGS84 lat/lon projection info.
     *
     * @param  outfile          The destination file. (will create .prj and .csv files)
     * @exception  IOException  Error when writing file?
     */
    public StreamingGeoJSONExport(File outfile) throws IOException {
        this.outfile = outfile;
        this.prj = WCTProjections.NAD83_ESRI_PRJ;
    }


    /**
     *  Writes the default WGS84 .prj file.
     *
     * @exception  IOException  Error writing?
     */
    private void writePrj() throws IOException {
        // Write .prj file  -- All exported Data is in WGS84 LatLon Projection
        // Check for .shp ending and remove it if necessary        
        String fileString = outfile.toString();
        File prjFile;
        if (! fileString.endsWith(".csv")) {
            outfile = new File(fileString+".csv");
            fileString = outfile.toString();
        }
        prjFile = new File(fileString.substring(0, fileString.length() - 4)+".prj");

        BufferedWriter bw = new BufferedWriter(new FileWriter(prjFile));
        bw.write(prj);
        bw.flush();
        bw.close();

    }

    private void init(Feature exampleFeature) throws IOException {

        if (! outfile.toString().endsWith(".json")) {
            outfile = new File(outfile.toString()+".json");
        }
        bw = new BufferedWriter(new FileWriter(outfile));
        for (int n=0; n<featureType.getAttributeCount(); n++) {
            if (! featureType.getAttributeType(n).isGeometry()) {
//                bw.write(featureType.getAttributeType(n).getName());
//                bw.write(",");
            }
        }
        
        if (exampleFeature.getDefaultGeometry().getGeometryType().contains("Point")) {
//        	bw.write("latitude,longitude");
        }
        else {
//        	bw.write(featureType.getDefaultGeometry().getName());
        }
//        bw.newLine();
        
        
        
        bw.write("{");
        bw.newLine();
        bw.write("  \"type\": \"FeatureCollection\",");
        bw.newLine();
        bw.write("  \"features\": [");
    }


    /**
     *  Implementation of StreamingProcess.  Writes out the Feature to the already open BufferedWriter.  If the
     *  feature's schema does not match the FeatureType of the first decoded feature or supplied FeatureType
     *  (if it is provided) it is <b>ignored</b>, 
     *  and <b> NO </b>exception is thrown.  This functionality allows for multiple types of Feature objects
     *  with different schemas to be decoded in a streaming process.
     *
     * @param  feature                   The feature to write.
     * @exception  StreamingProcessException  Error writing the feature?
     */
    public void addFeature(Feature feature) throws StreamingProcessException {
        try {

            if (firstTime) {
                // Extract featureType from first feature if no feature type is provided
                if (this.featureType == null) {
                    this.featureType = feature.getFeatureType();
                }

                init(feature);
                firstTime = false;
            }
            else {
            	bw.write(",");
            	bw.newLine();
            }

            // Ignore and return if this feature type does not match
            if (! feature.getFeatureType().equals(featureType)) {
                return;
            }
            

            bw.write("   {");
            bw.newLine();
            bw.write("    \"type\": \"Feature\",");
            bw.newLine();
            bw.write("    \"geometry\": {");

            if (feature.getDefaultGeometry().getGeometryType().contains("Point")) {
                bw.write("      \"type\": \"Point\",");
                bw.newLine();
                bw.write("      \"coordinates\": [");
            	bw.write(WCTUtils.DECFMT_0D0000.format(feature.getDefaultGeometry().getCoordinate().x));
            	bw.write(",");
            	bw.write(WCTUtils.DECFMT_0D0000.format(feature.getDefaultGeometry().getCoordinate().y));
            	bw.write(" ]");
            }
            else {
            	bw.write(feature.getDefaultGeometry().toString());
            }
            bw.newLine();
            bw.write("     },");
            bw.newLine();
            bw.write("     \"properties\": {");
            bw.newLine();

            int attNum = feature.getNumberOfAttributes();
            boolean firstAttribute = true;
            for (int n=0; n<attNum; n++) {
                if (! featureType.getAttributeType(n).isGeometry()) {

                    if (! firstAttribute) {
                    	bw.write(",");
                    	bw.newLine();
                    }
                    
                    String att = feature.getAttribute(n).toString();
                    bw.write("        \""+featureType.getAttributeType(n).getName()+"\": ");
                    bw.write("\""+att+"\"");
                    
                    firstAttribute = false;
                }
            }
            
            bw.newLine();
            bw.write("     }");
            bw.newLine();
            bw.write("   }");
            
        

        } catch (IOException ioe) {
            throw new StreamingProcessException("IOException: "+ioe.toString()+"\nFeature=" + feature.toString());
        }
    }



    /**
     * Close the JSON structure and file - THIS MUST BE DONE AFTER ALL FEATURES ARE PROCESSED!
     *
     * @exception  StreamingProcessException  Error closing the writer?
     */
    public void close() throws StreamingProcessException {
        try {
        	
        	bw.newLine();
        	bw.write("  ]");
        	bw.newLine();
        	bw.write("}");
        	bw.newLine();
        	
            if (bw != null) {
                bw.close();
                writePrj();
            }
        } catch (IOException ioe) {
            throw new StreamingProcessException("IOException: "+ioe.toString()+"\nERROR CLOSING FILE: " + outfile);
        }
    }


    /**
     * Just in case we forgot to close...
     */
    public void finalize() {
        try {
            close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }







    /**
     * Adds a GeneralProgressListener to the list.
     *
     * @param  listener  The feature to be added to the GeneralProgressListener attribute
     */
    public void addGeneralProgressListener(GeneralProgressListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


    /**
     * Removes a GeneralProgressListener from the list.
     *
     * @param  listener   GeneralProgressListener to remove.
     */
    public void removeGeneralProgressListener(GeneralProgressListener listener) {
        listeners.remove(listener);
    }

}


