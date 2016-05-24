package steve.test;

import gov.noaa.ncdc.wct.ResourceUtils;
import gov.noaa.ncdc.wct.WCTConstants;
import gov.noaa.ncdc.wct.export.vector.StreamingShapefileExport;

import java.io.File;
import java.net.URL;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class AlterFeatureGeometry {

    public static void main(String[] args) {
//        new AlterFeatureGeometry().processCanada();
//        new AlterFeatureGeometry().processCountries();
//        new AlterFeatureGeometry().processStates();
    	
    	
    	File inputShpfile = new File("D:\\work\\naturalearth\\final\\states-natearth.shp");
//    	File inputShpfile = new File("D:\\work\\naturalearth\\final\\countries-nousa.shp");
//    	File inputShpfile = new File("D:\\work\\naturalearth\\final\\countries-usa.shp");
    	File outdir = new File("D:\\work\\naturalearth\\final");
    	new AlterFeatureGeometry().append360(inputShpfile, outdir);
    }

    
    
    public void processCanada() {
        
        try {
            

//            URL url = ResourceUtils.getInstance().getJarResource(mapDataURL, ResourceUtils.RESOURCE_CACHE_DIR, "/shapefiles/countries-USA-360.shp", null);
            URL url = new File("E:\\work\\shapefiles\\fromNWS\\countries-noUSA-360.shp").toURI().toURL();
            ShapefileDataStore ds = new ShapefileDataStore(url);
            FeatureSource fs = ds.getFeatureSource("countries-noUSA-360");
            FeatureCollection fc = fs.getFeatures().collection();
            
            StreamingShapefileExport shpExport = new StreamingShapefileExport(new File("E:\\work\\shapefiles\\fromNWS\\countries-noUSACanada-360.shp"));            
            FeatureIterator iter = fc.features();
            FeatureType featureType = null;
            int geoIndex = 0;
            while (iter.hasNext()) {
                Feature f = iter.next();
                
                if (geoIndex == 0) {
                	featureType = f.getFeatureType();
                }
//                System.out.println(f.getFeatureType());
                
                Feature f1 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
//                System.out.println(f1.toString().substring(0, 80));
//                System.out.println(f1.getAttribute("CNTRY_NAME"));
                
                if (! f1.getAttribute("CNTRY_NAME").equals("Canada")) {
                	shpExport.addFeature(f1);
                }
                else {
                	System.out.println("found canada, and excluding");
                }
                

            }
            
            
            
            
            
            
            System.out.println(featureType);
            
            URL url2 = new File("E:\\work\\shapefiles\\fromNWS\\province.shp").toURI().toURL();
            ShapefileDataStore ds2 = new ShapefileDataStore(url2);
            FeatureSource fs2 = ds2.getFeatureSource("province");
            FeatureCollection fc2 = fs2.getFeatures().collection();
            FeatureIterator iter2 = fc2.features();
            while (iter2.hasNext()) {
                Feature f = iter2.next();       

//                System.out.println(f.getFeatureType());
//                System.out.println(f.getAttribute("NAME") + " " + f.getAttribute("the_geom"));
                
                Feature canFeature = featureType.create(new Object[] {
                		f.getAttribute("the_geom"), "", "", f.getAttribute("NAME")
                }, String.valueOf(geoIndex++));
                
                shpExport.addFeature(canFeature);
                
                
                
                Geometry geom = canFeature.getDefaultGeometry();
                Coordinate[] coords = geom.getCoordinates();
                for (Coordinate c : coords) {
                    c.x += 360;
                }
                shpExport.addFeature(canFeature);
                
            }
            
            shpExport.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    
    
    
    public void processCountriesUSA() {
        
        try {
            
            URL mapDataURL = new URL(WCTConstants.MAP_DATA_JAR_URL);

//            URL url = ResourceUtils.getInstance().getJarResource(mapDataURL, ResourceUtils.RESOURCE_CACHE_DIR, "/shapefiles/countries-USA-360.shp", null);
            URL url = new File("E:\\work\\shapefiles\\countries-USA.shp").toURI().toURL();
            ShapefileDataStore ds = new ShapefileDataStore(url);
            FeatureSource fs = ds.getFeatureSource("countries-USA");
            FeatureCollection fc = fs.getFeatures().collection();
            
            StreamingShapefileExport shpExport = new StreamingShapefileExport(new File("E:\\work\\shapefiles\\countries-USA-360.shp"));            
            FeatureIterator iter = fc.features();
            int geoIndex = 0;
            while (iter.hasNext()) {
                Feature f = iter.next();
                
//                System.out.println(f.getFeatureType());
                
                Feature f1 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f1.toString().substring(0, 80));
                shpExport.addFeature(f1);
                
                Geometry geom = f.getDefaultGeometry();
                Coordinate[] coords = geom.getCoordinates();
                for (Coordinate c : coords) {
                    c.x += 360;
                }
                
                Feature f2 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f2.toString().substring(0, 80));
                shpExport.addFeature(f2);

            }
            shpExport.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    

    
    
    public void processCountries() {
        
        try {
            
            URL mapDataURL = new URL(WCTConstants.MAP_DATA_JAR_URL);

            URL url = ResourceUtils.getInstance().getJarResource(mapDataURL, ResourceUtils.RESOURCE_CACHE_DIR, "/shapefiles/countries-noUSA.shp", null);
            ShapefileDataStore ds = new ShapefileDataStore(url);
            FeatureSource fs = ds.getFeatureSource("countries-noUSA");
            FeatureCollection fc = fs.getFeatures().collection();
            
            StreamingShapefileExport shpExport = new StreamingShapefileExport(new File("E:\\work\\shapefiles\\countries-noUSA-360.shp"));            
            FeatureIterator iter = fc.features();
            int geoIndex = 0;
            while (iter.hasNext()) {
                Feature f = iter.next();
                
//                System.out.println(f.getFeatureType());
                
                Feature f1 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f1.toString().substring(0, 80));
                shpExport.addFeature(f1);
                
                Geometry geom = f.getDefaultGeometry();
                Coordinate[] coords = geom.getCoordinates();
                for (Coordinate c : coords) {
                    c.x += 360;
                }
                
                Feature f2 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f2.toString().substring(0, 80));
                shpExport.addFeature(f2);

            }
            shpExport.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    
    
    
    
    
    
    
    public void processStates() {
        
        try {
            
            URL mapDataURL = new URL(WCTConstants.MAP_DATA_JAR_URL);

            URL url = ResourceUtils.getInstance().getJarResource(mapDataURL, ResourceUtils.RESOURCE_CACHE_DIR, "/shapefiles/states.shp", null);
            ShapefileDataStore ds = new ShapefileDataStore(url);
            FeatureSource fs = ds.getFeatureSource("states");
            FeatureCollection fc = fs.getFeatures().collection();
            
            StreamingShapefileExport shpExport = new StreamingShapefileExport(new File("E:\\work\\shapefiles\\states-360.shp"));            
            FeatureIterator iter = fc.features();
            int geoIndex = 0;
            while (iter.hasNext()) {
                Feature f = iter.next();
                
//                System.out.println(f.getFeatureType());
                
                Feature f1 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f1.toString().substring(0, 80));
                shpExport.addFeature(f1);
                
                Geometry geom = f.getDefaultGeometry();
                Coordinate[] coords = geom.getCoordinates();
                for (Coordinate c : coords) {
                    c.x += 360;
                }
                
                Feature f2 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f2.toString().substring(0, 80));
                shpExport.addFeature(f2);

            }
            shpExport.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    
    
    
    
    
    
    public void append360(File inputShpfile, File outdir) {
        
        try {
            
        	URL url = inputShpfile.toURI().toURL();
        	String basename = inputShpfile.getName();
        	basename = basename.substring(0, basename.length()-4);

            ShapefileDataStore ds = new ShapefileDataStore(url);
            FeatureSource fs = ds.getFeatureSource(basename);
            FeatureCollection fc = fs.getFeatures().collection();
            
            StreamingShapefileExport shpExport = new StreamingShapefileExport(new File(outdir.toString()+File.separator+basename+"-360.shp"));            
            FeatureIterator iter = fc.features();
            int geoIndex = 0;
            while (iter.hasNext()) {
                Feature f = iter.next();
                
//                System.out.println(f.getFeatureType());
                
                Feature f1 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f1.toString().substring(0, 80));
                shpExport.addFeature(f1);
                
                Geometry geom = f.getDefaultGeometry();
                Coordinate[] coords = geom.getCoordinates();
                for (Coordinate c : coords) {
                    c.x += 360;
                }
                
                Feature f2 = f.getFeatureType().create(f.getAttributes(null), String.valueOf(geoIndex++));
                System.out.println(f2.toString().substring(0, 80));
                shpExport.addFeature(f2);

            }
            shpExport.close();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
