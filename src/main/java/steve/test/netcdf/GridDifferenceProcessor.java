package steve.test.netcdf;

import gov.noaa.ncdc.common.color.SimpleColorMap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Formatter;

import javax.imageio.ImageIO;

import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CancelTaskImpl;

public class GridDifferenceProcessor {

	// extra color map stuff
	public final static SimpleColorMap COLOR_MAP_NEG = new SimpleColorMap(-35, 0, Color.BLUE, Color.WHITE); 
	public final static SimpleColorMap COLOR_MAP_POS = new SimpleColorMap(0, 35, Color.WHITE, Color.RED); 
	public final static int TRANSPARENT_COLOR_RGB = new Color(0, 0, 0, 0).getRGB();
	
	
	public static void main(String[] args) {
		GridDifferenceProcessor gdp = new GridDifferenceProcessor();
		// this could be to ServletOutputStream , File is just for testing
		try {
			gdp.process(new FileOutputStream(new File("grid-difference-test.png")));
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void process(OutputStream outputStream) {
		
		// simple difference using time index 

		String dataLocation = "http://eclipse-d.ncdc.noaa.gov:8080/thredds/dodsC/data-in-development/ghcn-grids/new_vose_5km.nc";
		String varName = "prcp";
		int t1 = 6;
		int t2 = 5;
				
		
		GridDataset gds = null;
		Formatter cdmLog = new Formatter(new StringBuilder());
		CancelTask ct = new CancelTaskImpl();
		try {
			
			
			System.out.println("loading OPeNDAP resource");

			// build GridDataset and GridDatatype objects from reading file metadata and coordinate variables
			// no data read yet
			gds = (GridDataset) FeatureDatasetFactoryManager.open(
					ucar.nc2.constants.FeatureType.GRID, 
					dataLocation, ct, cdmLog);


			GridDatatype grid = gds.findGridDatatype(varName);
			
			System.out.println("reading 2 grid slices into memory...");
			// now read data into memory
			Array t1Data = grid.readDataSlice(t1, -1, -1, -1);
			Array t2Data = grid.readDataSlice(t2, -1, -1, -1);
			
			// get the underlying primitive array (hopefully not copy)
			float[] t1DataArray = (float[]) t1Data.get1DJavaArray(Float.class);
			float[] t2DataArray = (float[]) t2Data.get1DJavaArray(Float.class);
			
			System.out.println("doing difference...");
			// do difference and reuse the first array to save memory
			for (int n=0; n<t1DataArray.length; n++) {
				if (Float.isNaN(t1DataArray[n]) || Float.isNaN(t2DataArray[n])) {
//					t1DataArray[n] = 0;
					t1DataArray[n] = Float.NaN;
				}
				else {
					t1DataArray[n] = t2DataArray[n] - t1DataArray[n];
				}
			}

			System.out.println("generating image...");
			// create Image object
			BufferedImage bimage = new BufferedImage(
					grid.getDimension(grid.getXDimensionIndex()).getLength(),
					grid.getDimension(grid.getYDimensionIndex()).getLength(),
					BufferedImage.TYPE_4BYTE_ABGR);
			
			// if you want transparent background add below:
			Graphics2D g2d = (Graphics2D)bimage.getGraphics();
			g2d.setComposite(AlphaComposite.Clear);
			g2d.fillRect(0, 0, bimage.getWidth(), bimage.getHeight());
			g2d.dispose();
			
			System.out.println("populating image with colors based on difference values...");
			int n=0;
			for (int y=0; y<bimage.getHeight(); y++) {
				for (int x=0; x<bimage.getWidth(); x++) {
				
					// this is where some mapping between data value and color is done
					int rgb = getColor(t1DataArray[n++]); 
					bimage.setRGB(x, y, rgb);
				}
			}
					
			System.out.println("encode image as png");
			ImageIO.write(bimage, "png", outputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	private static int getColor(float dataValue) {
		if (Float.isNaN(dataValue)) {
			return TRANSPARENT_COLOR_RGB;
		}
		else if (dataValue < 0) {
			return COLOR_MAP_NEG.getRGB(dataValue);
		}
		else {
			return COLOR_MAP_POS.getRGB(dataValue);
		}
	}
}
