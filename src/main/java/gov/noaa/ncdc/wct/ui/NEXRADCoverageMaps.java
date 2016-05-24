package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;

import gov.noaa.ncdc.nexradiv.legend.CategoryLegendImageProducer;

public class NEXRADCoverageMaps implements WmsResource {
    
    // http://gis.ncdc.noaa.gov/arcgis/rest/services/geo/radar_coverage/MapServer/export?f=image&bboxSR=4326&format=png24&transparent=true&bbox=-90,35,-91,36&size=800,800
	
    // defaults to first map
    private String yyMMdd = "000104";
    private String name = "NEXRAD Radar Coverage Quality";
    private float zIndex = 0.05f;
    private Rectangle imageSize = new Rectangle(640, 480);
//    private Image isLogo = null;
    
    
//    public ArrayList<String> getDates() throws Exception {
//        String startDate = "000104";
//        int interval = 7;
//        int delay = 2;
//        
//        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
//        Date curDate = new Date(new Date().getTime()-delay*24*60*60*1000);
//        Date prodDate = sdf.parse(startDate);
//        
//        ArrayList<String> dates = new ArrayList<String>();
//        while (prodDate.before(curDate)) {
////            System.out.println(sdf.format(prodDate));
//            dates.add(sdf.format(prodDate));            
//            prodDate = new Date(prodDate.getTime()+interval*24*60*60*1000);
//        }
//        
//        
//        return dates;
//    }
//    
//    public void setYYMMDD(String yyMMdd) {
//        this.yyMMdd = yyMMdd;
//    }
    public void setName(String name) {
        this.name = name;
    }
    public void setZIndex(float zIndex) {
        this.zIndex = zIndex;
    }

    public void setImageSize(Rectangle imageSize) {
        this.imageSize = imageSize;
    }
    
    
    @Override
    public URL getWmsUrl(Rectangle2D bounds) throws MalformedURLException {
        
        URL url = new URL("http://gis.ncdc.noaa.gov/arcgis/rest/services/geo/radar_coverage/MapServer/export?"
        		+ "f=image&bboxSR=4326&format=png24&transparent=true&bbox="
        		+ bounds.getMinX()+","+bounds.getMinY()+","+bounds.getMaxX()+","+bounds.getMaxY()
        		+"&size="+(int)imageSize.getWidth()+","+(int)imageSize.getHeight());
        		
        return url;
        
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getZIndex() {
        return zIndex;
    }
    
    @Override
    public Image getLegendImage() throws Exception {
//        return ImageIO.read(new URL(
//                "http://torka.unl.edu:8080/cgi-bin/mapserv.exe?"+
//                "map=/ms4w/apps/dm/service/usdm"+yyMMdd+"_wms.map&"+
//                "version=1.1.1&service=WMS&request=GetLegendGraphic&"+
//                "layer=USDM"+yyMMdd+"&format=image/png"));
        
        CategoryLegendImageProducer legendProducer = new CategoryLegendImageProducer();
        legendProducer.setBackgroundColor(new Color(255, 255, 255, 230));
        legendProducer.setDrawBorder(true);
        
        legendProducer.setCategoryColors(new Color[] {
                new Color(255, 255, 0),
                new Color(255, 211, 127),
                new Color(255, 170, 0),
                new Color(230, 0, 0),
                new Color(115, 0, 0)
            });
        legendProducer.setCategoryLabels(new String[] {
                "D0 Abnormally Dry",
                "D1 Drought - Moderate",
                "D2 Drought - Severe",
                "D3 Drought - Extreme",
                "D4 Drought - Exceptional"
            });
        
        SimpleDateFormat sdfIn = new SimpleDateFormat("yyMMdd");
        SimpleDateFormat sdfOut = new SimpleDateFormat("MMM dd, yyyy");
        String dateString = sdfOut.format(sdfIn.parse(yyMMdd));
        legendProducer.setLegendTitle(new String[] {
                "U.S. Drought Monitor",
                dateString
            });
        
        
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D)image.getGraphics();
        FontRenderContext fc = g.getFontRenderContext();
        int textHeight = (int) g.getFont().createGlyphVector(fc, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").getVisualBounds().getHeight();
//        System.out.println("TEXT HEIGHT:  "+textHeight);
        
//        return legendProducer.createLargeLegendImage(new Dimension(225, 120));
        return legendProducer.createLargeLegendImage(new Dimension(225, 6*textHeight+55));
    }

    @Override
    public String getInfo() {
        return "<html>Radar Coverages are calculated from the NOAA/NWS Radar Operations Center. <br><br>" +
        		"For more information, please refer to http://www.roc.noaa.gov <html>.";
    }

    @Override
    public Image getResourceLogo() {
        return null;
    }
    
    
    
}
