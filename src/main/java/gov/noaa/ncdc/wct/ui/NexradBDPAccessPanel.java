package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.wct.WCTProperties;
import gov.noaa.ncdc.wct.decoders.nexrad.RadarHashtables;

public class NexradBDPAccessPanel extends JPanel {

	public final static String AWS_BUCKET_ROOT = "http://noaa-nexrad-level2.s3.amazonaws.com";
	public final static int AWS_BUCKET_SEARCH_MAX_RESULTS = 1000;
	
	public final static SimpleDateFormat SDF_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
	static {
		SDF_YYYYMMDD.setTimeZone(TimeZone.getTimeZone("GMT"));		
	}
//	private JList<String> fileList;
	private JComboBox<String> siteList, datasetList, providerList;
	private JLabel statusLabel = new JLabel();
	private final JXDatePicker picker = new JXDatePicker(new Date(), Locale.US);

	private WCTViewer viewer;
	private ActionListener listButtonListener;

	public NexradBDPAccessPanel(WCTViewer viewer, ActionListener listButtonListener) throws ParserConfigurationException {
		// super(viewer, "NOAA Big Data Project - Access Tool", false);
		super(new RiverLayout());
		this.viewer = viewer;		
		this.listButtonListener = listButtonListener;
		init();
		createGUI();
		// pack();
	}

	private void init() throws ParserConfigurationException {
		datasetList = new JComboBox<String>(new String[] { "NEXRAD Level-2" });
		providerList = new JComboBox<String>(new String[] { "Amazon" });
	}

	private void createGUI() {


		siteList = new JComboBox<String>();
		listSites();
		String lastSiteProp = WCTProperties.getWCTProperty("bdpAWS_SiteID");
		if (lastSiteProp != null) {
			for (int n=0; n<siteList.getItemCount(); n++) {
				if (siteList.getItemAt(n).startsWith(lastSiteProp)) {
					siteList.setSelectedIndex(n);
				}
			}   
		}

		picker.setTimeZone(TimeZone.getTimeZone("GMT"));
		picker.getMonthView().setUpperBound(new Date());
		picker.getMonthView().setFlaggedDayForeground(Color.BLUE.darker());
		String lastDateProp = WCTProperties.getWCTProperty("bdpAWS_Date");
		if (lastDateProp != null) {
			try {
				picker.setDate(SDF_YYYYMMDD.parse(lastDateProp));
			} catch (ParseException e1) {
				picker.setDate(new Date());
				e1.printStackTrace();
			}
		}
		else {
			picker.setDate(new Date());
		}
		picker.addPropertyChangeListener(e -> {
			if ("date".equals(e.getPropertyName())) {
				// refresh upper bound, in case the next GMT day becomes available after picker has been created
				picker.getMonthView().setUpperBound(new Date());
				selectDate(picker.getDate());
			}
		});
		picker.getMonthView().addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent evt) {
			}
			@Override
			public void mouseMoved(MouseEvent evt) {
				Date d = picker.getMonthView().getDayAtLocation(evt.getX(), evt.getY());
				if (d != null) {
					picker.getMonthView().clearFlaggedDates();
					picker.getMonthView().setFlaggedDates(d);
				}
			}
		});


		JXHyperlink bdpLink = new JXHyperlink();
		bdpLink.setText("[ ? ]");
		bdpLink.addActionListener(e -> WCTUiUtils.browse(viewer, "https://data-alliance.noaa.gov/", "Error browsing to: https://data-alliance.noaa.gov/"));        

		JXHyperlink mapLink = new JXHyperlink();
		mapLink.setText("[map]");
		mapLink.addActionListener(e -> {
			try {
				viewer.getMapSelector().setLabelVisibility(WCTViewer.WSR, false);
				viewer.getMapSelector().setLayerVisibility(WCTViewer.WSR, false);
				viewer.getMapSelector().setLabelVisibility(WCTViewer.WSR, true);
				viewer.getMapSelector().setLayerVisibility(WCTViewer.WSR, true);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		JXHyperlink awsLink = new JXHyperlink();
		awsLink.setText("Amazon Documentation");
		awsLink.addActionListener(e -> WCTUiUtils.browse(viewer, "https://aws.amazon.com/noaa-big-data/nexrad/", 
				"Error browsing to: https://aws.amazon.com/noaa-big-data/nexrad/"));   		

		JXHyperlink nceiNewsLink = new JXHyperlink();
		nceiNewsLink.setText("NCEI News Article");
		nceiNewsLink.addActionListener(e -> WCTUiUtils.browse(viewer, "https://www.ncdc.noaa.gov/news/partnering-amazon-web-services-big-data",
				"Error browsing to: https://www.ncdc.noaa.gov/news/partnering-amazon-web-services-big-data"));   
		
		JButton listButton = new JButton("  List Files  ");
		listButton.addActionListener(listButtonListener);
        listButton.setMnemonic(KeyEvent.VK_L);


//		this.setBorder(BorderFactory.createLineBorder(Color.RED));
		this.add("center", new JLabel("Access to data available from cloud collaborators in the NOAA Big Data Project"));
		this.add(bdpLink);
		this.add("p left", datasetList);
		this.add("tab", providerList);
//		this.add(new JLabel("Questions? "));
		this.add(awsLink);
		this.add(new JLabel("/"));
		this.add(nceiNewsLink);
		this.add("p left", listButton);
		this.add("tab", siteList);
		this.add(mapLink);
		this.add(picker);
		this.add(new JLabel("(GMT/UTC Day)", SwingConstants.LEFT));       
		this.add(statusLabel);


		this.setSize(700, 600);


	}

	
	
	private void selectDate(Date date) {
		System.out.println(date);
	}

	private void listSites() {

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

			DefaultComboBoxModel<String> listModel = new DefaultComboBoxModel<String>();
			// FeatureIterator iter =
			// viewer.getStationPointFeatures().features();
			RadarHashtables nxhash = RadarHashtables.getSharedInstance();
			final ArrayList<String> idList = nxhash.getIdList();
			for (String id : idList) {
				if (id.length() == 4 && (id.startsWith("K") || id.startsWith("P") || id.startsWith("R")
						|| id.startsWith("L") || id.equals("TJUA"))) {
					String s = id + " -- " + nxhash.getLocation(id)
					+ (nxhash.getState(id) != null ? ", " + nxhash.getState(id) : "");
//					System.out.println("adding " + s);
					listModel.addElement(s);
				}
			}
			siteList.setModel(listModel);

		} catch (Exception e) {
			// siteListCombo =
			e.printStackTrace();
		}
	}
	
	public String getDataPath() {
		

		Date d = picker.getMonthView().getFirstSelectionDate();

		String yyyy = SDF_YYYYMMDD.format(d).substring(0, 4);
		String mm = SDF_YYYYMMDD.format(d).substring(4, 6);
		String dd = SDF_YYYYMMDD.format(d).substring(6, 8);
		String siteID = siteList.getSelectedItem().toString().substring(0, 4);

		// save properties because this indicates a dir listing
        WCTProperties.setWCTProperty("bdpAWS_SiteID", siteID);
        WCTProperties.setWCTProperty("bdpAWS_Date", SDF_YYYYMMDD.format(d));
        
		return AWS_BUCKET_ROOT + "/?prefix=" + yyyy + "/" + mm + "/" + dd + "/" + siteID + "/";
	}
	
	public void setDate(String yyyymmdd) throws ParseException {
		picker.setDate(SDF_YYYYMMDD.parse(yyyymmdd));
	}
	public void setSite(String siteid) {
		for (int n=0; n<siteList.getItemCount(); n++) {
			if (siteList.getItemAt(n).startsWith(siteid)) {
				siteList.setSelectedIndex(n);
			}
		}
	}
	
}
