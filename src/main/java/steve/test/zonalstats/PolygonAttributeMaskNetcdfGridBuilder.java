package steve.test.zonalstats;

import java.io.IOException;
import java.net.URL;
import java.util.Formatter;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;

import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonRect;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

public class PolygonAttributeMaskNetcdfGridBuilder {

	
 

    
    private void processNetcdfJoinAppend() throws Exception {
    	
    	String[][] configArray = new String[][] {
    			new String[] { "/shapefiles/states.shp", "states", "STATE_FIPS" },
    			new String[] { "/shapefiles/counties.shp", "counties", "FIPS" }
    	};
    	
    	
    	String infile = "D:\\work\\station-grids\\vose_5km_abbreviated.nc";
    	String gridName = "tmax";
    	String outfile = "mask2.nc";
    	
    	
    	
    	
    	
        // get bounds from NetCDF file
        Formatter fmter = new Formatter();
        CancelTask cancelTask = new CancelTask() {
			@Override
			public boolean isCancel() {
				return false;
			}
			@Override
			public void setError(String arg0) {
			}
			@Override
			public void setProgress(String arg0, int arg1) {
			}
        };
        GridDataset gds = (GridDataset) FeatureDatasetFactoryManager.open(
    			ucar.nc2.constants.FeatureType.GRID, infile, cancelTask, fmter);
        GridDatatype grid = gds.findGridDatatype(gridName);
        GridCoordSystem gcs = grid.getCoordinateSystem();
        if (! gcs.isLatLon()) {
        	throw new Exception("This grid is not lat/lon, only lat/lon gridded data are supported.");
        }
        LatLonRect llRect = gcs.getLatLonBoundingBox();
        float[] xAxisData = (float[])gcs.getXHorizAxis().read().copyTo1DJavaArray();
        double xCellSpacing = Math.abs(xAxisData[1] - xAxisData[0]);
        System.out.println("x cell spacing from netcdf: "+xCellSpacing);

        float[] yAxisData = (float[])gcs.getYHorizAxis().read().copyTo1DJavaArray();
        double yCellSpacing = Math.abs(yAxisData[1] - yAxisData[0]);
        System.out.println("y cell spacing from netcdf: "+yCellSpacing);
        
        // mask variables are just 2D, using coordinate dimensions
        String dataVarDimensionString = gcs.getYHorizAxis().getShortName()+" "+gcs.getXHorizAxis().getShortName();
//        for (Dimension dim : gcs.getDomain()) {
//        	dataVarDimensionString = dataVarDimensionString + dim.getShortName() + " ";
//        }
        
        
        // copy data from infile to outfile
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(Version.netcdf3, outfile);
        System.out.println("writer: "+writer.getNetcdfFile().toString());
        for (Dimension dim : gds.getNetcdfFile().getDimensions()) {
        	System.out.println("adding dimension: "+dim.getFullName()+", group: "+dim.getGroup());
        	writer.addDimension(dim.getGroup().isRoot() ? null : dim.getGroup(), dim.getFullName(), dim.getLength());
        }
        for (Attribute att : gds.getNetcdfFile().getGlobalAttributes()) {
        	writer.addGroupAttribute(att.getGroup(), att);
        }
        for (Variable var : gds.getNetcdfFile().getVariables()) {
        	writer.addVariable(var.getGroup().isRoot() ? null : var.getGroup(), 
        			var.getShortName(), var.getDataType(), var.getDimensionsString());
        	
        	Variable writerVar = writer.getNetcdfFile().findVariable(var.getShortName());
        	for (Attribute att : var.getAttributes()) {
        		writer.addVariableAttribute(writerVar, att);
        	}
        		
        }
        

        System.out.println("writer: "+writer.getNetcdfFile().toString());
        
        // shell out variables
    	for (int i=0; i<configArray.length; i++) {
    		String path = configArray[i][0];
    		String name = configArray[i][1];
    		String attName = configArray[i][2];
    		

    		String varName = "attribute_mask_"+name+"_"+attName;
        	writer.addVariable(null, varName, DataType.INT, dataVarDimensionString);	
        	Variable writerVar = writer.getNetcdfFile().findVariable(varName);
        	writer.addVariableAttribute(writerVar, 
        			new Attribute("long_name", "mask variable created from '"+name+"' features"));
        	writer.addVariableAttribute(writerVar, 
        			new Attribute("description", "mask variable created from '"+name+"' features, " +
        					"using '" + attName + "' attributes for mask values"));
        	
        	
    	}
        
        writer.create();

        for (Variable var : gds.getNetcdfFile().getVariables()) {
        	// writing variable data
        	System.out.println("writing variable: "+var.getShortName());
        	Variable writerVar = writer.getNetcdfFile().findVariable(var.getShortName());
        	writer.write(writerVar, var.read());
        }
        
        
        


    	GeometryFactory geoFactory = new GeometryFactory();

    	for (int i=0; i<configArray.length; i++) {
    		String path = configArray[i][0];
    		String name = configArray[i][1];
    		String attName = configArray[i][2];
    		

    		String varName = "attribute_mask_"+name+"_"+attName;
    		URL shapefileURL = PolygonAttributeMaskNetcdfGridBuilder.class.getResource(path); // get State shapefile

    		final STRtree spatialIndex = new STRtree();

    		FeatureSource featureSource = getShapefileFeatureSource(shapefileURL);
    		FeatureIterator iter = featureSource.getFeatures().collection().features();
    		while (iter.hasNext()) {
    			Feature f = iter.next();
    			System.out.println("adding to spatial index: "+f);
    			spatialIndex.insert(f.getBounds(), f);
    		}

    		int minVal = Integer.MAX_VALUE;
    		int maxVal = Integer.MIN_VALUE;
    		ArrayInt valueArray = new ArrayInt.D2(yAxisData.length, xAxisData.length);
    		Index ima = valueArray.getIndex();

    		for (int y=0; y<yAxisData.length; y++) {
    			for (int x=0; x<xAxisData.length; x++) {

    				valueArray.setDouble(ima.set(y, x), Double.NaN);

    				double lat = yAxisData[y];
    				double lon = xAxisData[x];
    				Point centroidPoint = geoFactory.createPoint(new Coordinate(lon, lat));

    				//        		System.out.println("querying for: "+lat+","+lon);
    				List results = spatialIndex.query(new Envelope(lon, lon, lat, lat));
    				for (int n=0; n<results.size(); n++) {

    					if (((Feature)results.get(n)).getDefaultGeometry().contains(centroidPoint)) {
    						//                    	System.out.println(centroidPoint + " is in: "+results.get(n).toString());
    						int val = Integer.parseInt( ((Feature)results.get(n)).getAttribute(attName).toString() );

    						valueArray.setInt(ima.set(y, x), val);
    						// break loop
    						n = results.size();


    						maxVal = (val > maxVal) ? val : maxVal;
    						minVal = (val < minVal) ? val : minVal;
    					}
    				}

    			}

    			// Progress
    			// --------------
    			//            for (int n = 0; n < listeners.size(); n++) {
    			//                event.setProgress( (int)( ( ((double)j) / latDim.getLength() ) * 100.0) );
    			//                listeners.get(n).progress(event);
    			//            }
    			System.out.println(varName+": row "+y+" of "+yAxisData.length);
    		}


    		System.out.println("min/max: "+minVal+" , "+maxVal);


    		
    		writer.write(writer.findVariable(varName), valueArray);
        
    	}

        writer.close();
        
    }
    

    
    
    
    
    
    
    
    

    public static FeatureSource getShapefileFeatureSource(URL shapefileURL) throws IOException {
       
        ShapefileDataStore ds = new ShapefileDataStore(shapefileURL);
        // For shapefiles, FeatureSource name is always the name of the shapefile without the '.shp'
        // Therefore, we must extract this from the entire URL
        String urlString = shapefileURL.toString();
        int idx = urlString.lastIndexOf("/");
        String name = urlString.substring(idx + 1, urlString.length() - 4);
        
        FeatureSource fs = ds.getFeatureSource(name);

        return fs;
    }
    
    
}
