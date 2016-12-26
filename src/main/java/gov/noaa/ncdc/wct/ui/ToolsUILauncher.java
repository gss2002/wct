package gov.noaa.ncdc.wct.ui;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPSession;
import ucar.nc2.grib.collection.GribCollectionProto.GribCollection;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.util.SocketMessage;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.UrlAuthenticatorDialog;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.xml.RuntimeConfigParser;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import ucar.util.prefs.ui.Debug;

public class ToolsUILauncher {

    static String wantDataset;
    static ToolsUI ui;
    static private final String FRAME_SIZE = "FrameSize";

	public static void launch(String[] args) {
		
		// get a splash screen up right away
	    final SplashScreen splash = new SplashScreen();


	    
	    //////////////////////////////////////////////////////////////////////////
	    // handle multiple versions of ToolsUI, along with passing a dataset name
	    SocketMessage sm;
	    if (args.length > 0) {
	      // munge arguments into a single string
	      StringBuilder sbuff = new StringBuilder();
	      for (int i = 0; i < args.length; i++) {
	        sbuff.append(args[i]);
	        sbuff.append(" ");
	      }
	      String arguments = sbuff.toString();
	      System.out.println("ToolsUI arguments=" + arguments);

	      // LOOK - why does it have to start with http ??
	      if (arguments.startsWith("http:")) {
	        wantDataset = arguments;

	        // see if another version is running, if so send it the message
	        sm = new SocketMessage(14444, wantDataset);
	        if (sm.isAlreadyRunning()) {
	          System.out.println("ToolsUI already running - pass argument= '" + wantDataset + "' to it and exit");
//	          System.exit(0);
	        }
	      }

	    } else { // no arguments were passed

	      // look for messages from another ToolsUI
	      sm = new SocketMessage(14444, null);
	      if (sm.isAlreadyRunning()) {
	        System.out.println("ToolsUI already running - start up another copy");
	        sm = null;
	        
	        ui.setVisible(true);
	        return;
	        
	      } else {
	        sm.addEventListener(new SocketMessage.EventListener() {
	          public void setMessage(SocketMessage.Event event) {
	            wantDataset = event.getMessage();
//	            if (debugListen) System.out.println(" got message= '" + wantDataset);
//	            setDataset();
	          }
	        });
	      }
	    }
	    ////////////////////////////////////////////////////////////////////////////////////////////////

	    // spring initialization
	    ApplicationContext springContext =
	            new ClassPathXmlApplicationContext("classpath:resources/nj22/ui/spring/application-config.xml");

	    // look for run line arguments
	    boolean configRead = false;
	    for (int i = 0; i < args.length; i++) {
	      if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length - 1)) {
	        String runtimeConfig = args[i + 1];
	        i++;
	        try {
	          StringBuilder errlog = new StringBuilder();
	          FileInputStream fis = new FileInputStream(runtimeConfig);
	          RuntimeConfigParser.read(fis, errlog);
	          configRead = true;
	          System.out.println(errlog);
	        } catch (IOException ioe) {
	          System.out.println("Error reading " + runtimeConfig + "=" + ioe.getMessage());
	        }
	      }
	    }

	    if (!configRead) {
	      String filename = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "nj22Config.xml");
	      File f = new File(filename);
	      if (f.exists()) {
	        try {
	          StringBuilder errlog = new StringBuilder();
	          FileInputStream fis = new FileInputStream(filename);
	          RuntimeConfigParser.read(fis, errlog);
	          configRead = true;
	          System.out.println(errlog);
	        } catch (IOException ioe) {
	          System.out.println("Error reading " + filename + "=" + ioe.getMessage());
	        }
	      }
	    }

	    PreferencesExt prefs = null;
	    // prefs storage
	    try {
	      String prefStore = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
	      XMLStore store = ucar.util.prefs.XMLStore.createFromFile(prefStore, null);
	      prefs = store.getPreferences();
	      Debug.setStore(prefs.node("Debug"));
	    } catch (IOException e) {
	      System.out.println("XMLStore Creation failed " + e);
	    }

	    // misc initializations
	    BAMutil.setResourcePath("/resources/nj22/ui/icons/");

	    // filesystem caching
	    DiskCache2 cacheDir = new DiskCache2(".unidata/ehcache", true, -1, -1);
	    //cacheManager = thredds.filesystem.ControllerCaching.makeTestController(cacheDir.getRootDirectory());
	    //DatasetCollectionMFiles.setController(cacheManager); // ehcache for files
//	    thredds.inventory.CollectionManagerAbstract.enableMetadataManager();    // bdb for metadata

	    // for efficiency, persist aggregations. every hour, delete stuff older than 30 days
	    Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, 60 * 24 * 30, 60));

	    // test
	    // java.util.logging.Logger.getLogger("ucar.nc2").setLevel( java.util.logging.Level.SEVERE);

	    // put UI in a JFrame
	    JFrame frame = new JFrame("Unidata NetCDF Tools (version "+getVersion()+")");
	    ui = new ToolsUI(prefs, frame);

	    frame.setIconImage(BAMutil.getImage("netcdfUI"));

	    frame.addWindowListener(new WindowAdapter() {
	      public void windowActivated(WindowEvent e) {
	        splash.setVisible(false);
	        splash.dispose();
	      }

	      public void windowClosing(WindowEvent e) {
//	        if (!done) exit();
	      }
	    });

	    frame.getContentPane().add(ui);
	    Rectangle bounds = (Rectangle) prefs.getBean(FRAME_SIZE, new Rectangle(50, 50, 800, 450));
	    frame.setBounds(bounds);

	    frame.pack();
	    frame.setBounds(bounds);
	    frame.setVisible(true);

	    UrlAuthenticatorDialog provider = new UrlAuthenticatorDialog(frame);
	    try {
			HTTPSession.setGlobalCredentialsProvider(provider);
		} catch (HTTPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    HTTPSession.setGlobalUserAgent("ToolsUI v4.3");

	    // set Authentication for accessing passsword protected services like TDS PUT
	    java.net.Authenticator.setDefault(provider);

	    // open dap initializations
	    ucar.nc2.dods.DODSNetcdfFile.setAllowCompression(true);
	    ucar.nc2.dods.DODSNetcdfFile.setAllowSessions(true);

	  }


	
	 private static String getVersion() {

		    String version;
		    try {
		      InputStream is = ucar.nc2.ui.util.Resource.getFileResource("/README");
		      if (is == null) return "4.3.17";
		// DataInputStream dataIS = new DataInputStream( new BufferedInputStream(ios, 20000));
		      BufferedReader dataIS = new BufferedReader(new InputStreamReader(is));
		      StringBuilder sbuff = new StringBuilder();
		      for (int i = 0; i < 3; i++) {
		        sbuff.append(dataIS.readLine());
		        sbuff.append("<br>");
		      }
		      version = sbuff.toString();
		    } catch (IOException ioe) {
		      ioe.printStackTrace();
		      version = "version unknown";
		    }

		    return version;
		  }

		  // Splash Window
		  private static class SplashScreen extends javax.swing.JWindow {
		    public SplashScreen() {
		      Image image = Resource.getImage("/resources/nj22/ui/pix/ring2.jpg");
		      ImageIcon icon = new ImageIcon(image);
		      JLabel lab = new JLabel(icon);
		      getContentPane().add(lab);
		      pack();

		      //show();
		      java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		      int width = image.getWidth(null);
		      int height = image.getHeight(null);
		      setLocation(screenSize.width / 2 - (width / 2), screenSize.height / 2 - (height / 2));
		      addMouseListener(new MouseAdapter() {
		        public void mousePressed(MouseEvent e) {
		          setVisible(false);
		        }
		      });
		      setVisible(true);
		    }
		  }

}
