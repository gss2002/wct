//package steve.test.panoply;
//
//import gov.nasa.giss.netcdf.NcArray;
//import gov.nasa.giss.netcdf.NcDataset;
//import gov.nasa.giss.netcdf.NcVariable;
//import gov.nasa.giss.netcdf.NcVariable.Axes;
//import gov.nasa.giss.panoply.PanArrayDataPanel;
//import gov.nasa.giss.panoply.PanFileUtilities;
//import gov.nasa.giss.panoply.PanLonlatData;
//import gov.nasa.giss.panoply.PanLonlatPlot;
//import gov.nasa.giss.panoply.PanPlotMeta;
//import gov.nasa.giss.panoply.PanPlotType;
//import gov.nasa.giss.panoply.PanSourcesFrame;
//import gov.nasa.giss.panoply.Panoply;
//
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//
//import javax.swing.ImageIcon;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//
//public class PanoplyTest {
//
//	
//	public static void main(String[] args) {
//		
//		// figured out from: http://java.decompiler.free.fr/
//		
//		try {
//			URL url = new File("C:\\work\\wct-batch\\testdata\\flxf.01.2013062700.201307.avrg.grib.grb2").toURI().toURL();
//			NcDataset ncdataset = new NcDataset(url);
//			String varname = "Temperature_surface_1_Month_Average";
//			NcVariable ncvar = new NcVariable(ncdataset, varname);
//			NcArray ncarray = ncvar.getArray(Axes.LON_LAT);
//			
//			PanLonlatData pllData = new PanLonlatData(ncarray, 800, 800);
//			PanPlotMeta ppmeta = new PanPlotMeta(PanPlotType.LON_LAT);
//			ppmeta.setBoolean("plot:scale.autofit", true);
//			PanLonlatPlot pllPlot = new PanLonlatPlot(ppmeta, pllData);
//			pllPlot.setSize(900, 900);
////			pllPlot.setBounds(0, 0, 300, 300);
//			
//			BufferedImage bimage = pllPlot.getImage();
//			
//			
//			JFrame frame = new JFrame("panTest");
//			frame.getContentPane().add(new JLabel(new ImageIcon(bimage)));
//			frame.pack();
//			frame.setVisible(true);
//			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//			
//			
////			PanArrayDataPanel panel = new PanArrayDataPanel(PanPlotType.LON_LAT, pllData, 0);
////			JFrame frame2 = new JFrame("panTest2");
////			frame2.getContentPane().add(panel);
////			frame2.pack();
////			frame2.setVisible(true);
////			frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//			
////			Panoply pan = new Panoply();
//			
//			PanSourcesFrame psf = PanSourcesFrame.findFrame(true);
//			PanFileUtilities.openDataset(psf, url);
//			
//			
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
//}
