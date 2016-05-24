package gov.noaa.ncdc.wct.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.icepdf.ri.common.ComponentKeyBinding;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;

import gov.noaa.ncdc.wct.WCTConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class CDOStationDataForm {



	public static final String SERVICE_URL_STRING = "http://www.ncdc.noaa.gov/cdo-web/quickdata";
	public static final String AJAX_SERVICE_URL_STRING = "http://www.ncdc.noaa.gov/cdo-web/ajax/ajaxquickdata";

	public static final File CACHE_PDF = new File(WCTConstants.getInstance().getCacheLocation()+File.separator+
			"config"+File.separator+"cdoStationForms.pdf");

	// build a controller
	private SwingController controller = new SwingController();
	
	private Frame parent;
	
	
	public static void main(String[] args) {
		CDOStationDataForm form = new CDOStationDataForm(null);


		String station = "GHCND:USW00003812";
		try {
			form.loadForm(station, "2008", "01");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CDOStationDataForm(final Frame parent) {
		this.parent = parent;
	}




	/**
	 * 
	 * @param station - if null, then just load cached PDF
	 * @return
	 * @throws IOException
	 * @throws DocumentException
	 */
	public JPanel loadForm(final String station, final String yyyy, final String mm) throws IOException, DocumentException {
		ArrayList<String> stationList = new ArrayList<String>();
		if (station != null) {
			stationList.add(station);
		}
		return loadForm(stationList, yyyy, mm);
	}
	

	/**
	 * 
	 * @param station - if null, then just load cached PDF
	 * @return
	 * @throws IOException
	 * @throws DocumentException
	 */
	public JPanel loadForm(final List<String> stationList, final String yyyy, final String mm) throws IOException, DocumentException {
	



		//
		//		// Open a PDF document to view
		//		controller.openDocument(new URL(filePath));
		//
		//
		//		System.out.println(filePath);



		// read persistent pdf in cache
		
		File cachePdfToWrite = new File(CACHE_PDF.toString()+".tmp");

		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document();
		PdfCopy pdfCopy = new PdfCopy(document, baos);
		document.open();

		// copy existing pages in persistent pdf
		if (CACHE_PDF.exists()) {

			ByteArrayInputStream cachePdfInputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(CACHE_PDF));
			try {
				PdfReader pdfReader = new PdfReader(cachePdfInputStream);
				// loop over the pages in that document
				int n = pdfReader.getNumberOfPages();
				for (int page = 0; page < n; ) {
					pdfCopy.addPage(pdfCopy.getImportedPage(pdfReader, ++page));
				}
				pdfCopy.freeReader(pdfReader);
				pdfReader.close();

			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.err.println("Error Overwriting existing persistent PDF");
			} finally {
				cachePdfInputStream.close();
			}
			
		}

		if (stationList != null && stationList.size() > 0) {
			
			for (String station : stationList) {
				
				// http://www.ncdc.noaa.gov/cdo-web/quickdatapdf/steve?datasetId=GHCND&productId=GHCN_DAILY_FORM&stationId=GHCND:US1GARB0006&year=2010&month=5&day=1
				String filePath = null;
				if (stationList != null) {
					filePath = "http://www.ncdc.noaa.gov/cdo-web/quickdatapdf/wct"+getQueryString(station, yyyy, mm);
				}

				System.out.println(filePath);

				try {
					// copy/append new pages
					PdfReader pdfReader = new PdfReader(new URL(filePath));
					// loop over the pages in that document
					int n = pdfReader.getNumberOfPages();
					for (int page = 0; page < n; ) {
						pdfCopy.addPage(pdfCopy.getImportedPage(pdfReader, ++page));
					}
					pdfCopy.freeReader(pdfReader);
					pdfReader.close();
				} catch (Exception e) {
					WCTUiUtils.showErrorMessage(parent, "No Data Found for Station: "+station, e);
				}
				
			}
			
		}
	
		
		
		
		
		document.close();

		controller.closeDocument();
		controller.dispose();
		
		
//		FileUtils.forceDelete(cachePdf);
//		FileUtils.moveFile(cachePdfToWrite, cachePdf);
		FileUtils.writeByteArrayToFile(CACHE_PDF, baos.toByteArray());
		




		
		

//		// build a controller
		controller = new SwingController();

		// Open merged PDF document to view
//		controller.openDocument(cachePdf.toString());
		controller.openDocument(new ByteArrayInputStream(baos.toByteArray()), "Historical Station Data Forms", "ncei-station-data.pdf");
		controller.updateDocumentView();
		controller.getDocumentViewController().setCurrentPageIndex(controller.getDocument().getNumberOfPages()-1);
		controller.setAnnotationPanel(null);

		// Build a SwingViewFactory configured with the controller
		SwingViewBuilder factory = new SwingViewBuilder(controller);

		// Use the factory to build a JPanel that is pre-configured
		//with a complete, active Viewer UI.
		JPanel pdfPanel = factory.buildViewerPanel();
//		pdfPanel.remove(factory.buildAnnotationlToolBar());
//		pdfPanel.remove(factory.buildAnnotationPanel());
//		pdfPanel.remove(factory.buildAnnotationUtilityToolBar());
//		pdfPanel.getComponents()[0].list();
//		for (Component c : pdfPanel.getComponents()) {
//			System.out.println(c.get);
//		}

		
		// add copy keyboard command
		ComponentKeyBinding.install(controller, pdfPanel);

		// add interactive mouse link annotation support via callback
		controller.getDocumentViewController().setAnnotationCallback(
				new org.icepdf.ri.common.MyAnnotationCallback(
						controller.getDocumentViewController()));
		
		
		

		return pdfPanel;
		
//		WCTFrame frame = new WCTFrame("Station Temp./Precip. Form");
//		frame.getContentPane().setLayout(new BorderLayout());
//
//		frame.getContentPane().add(pdfPanel, BorderLayout.CENTER);
//		frame.setSize(800, 600);
//		frame.setVisible(true);






	}


	public void loadFormOLD(final String station) throws IOException {


		//		System.out.println("mark 1");
		//		NativeInterface.open();
		//		System.out.println("mark 2");



		//		String test = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/cdo-web/resources-2.3.0/formcss/form.css\" />";
		//		test = test.replaceAll("resources-2", "steve-2");
		//		System.out.println(test);


		String html = process(station);

		System.out.println("before replace");

		//		html = html.replaceAll("/cdo-web/resources-2.3.0/formcss/form.css",
		//				"http://www.ncdc.noaa.gov/cdo-web/resources-2.3.0/formcss/form.css");

		html = html.replaceAll("/cdo-web/resources-3.", 
				"http://www.ncdc.noaa.gov/cdo-web/resources-3.");

		html = html.replace("initQuickData(data);", getQuickdataAjaxFunction());




		//		html = "<html><body>test steve test</body></html>";

		System.out.println(html);

		final String mapurl = "http://tmappsevents.esri.com/website/pim_severe_weather/index.html?" +
				"bm=6e03e8c26aad4b9c92a87c1063ddb0e3&lrs=b59a%2Ce46f%2C9764%2C38bf%2C6ba2%2Chtwn&" +
				"ytkw=storm&ytr=this_week&twkw=%23storm&flkw=storm&smd=1&xmin=-14096269.697251236&" +
				"ymin=2851099.966426694&xmax=-6269118.000851267&ymax=6466265.656201429";

		final String finalHTML = html;


		//		final JWebBrowser webMapBrowser = new JWebBrowser();
		//		SwingUtilities.invokeLater(new Runnable() {
		//			public void run() {
		//				webMapBrowser.setBarsVisible(false);
		//				webMapBrowser.setHTMLContent(finalHTML);
		//			}
		//		});


		final JFXPanel jfxPanel = new JFXPanel();




		Platform.runLater(new Runnable() {
			@Override public void run() {

				final WebView view = new WebView();
				final WebEngine engine = view.getEngine();


				//                engine.load(SERVICE_URL_STRING+getQueryString());
				engine.loadContent(finalHTML);
				//                engine.load(mapurl);




				//                try {
				//					Desktop.getDesktop().browse(new URI(SERVICE_URL_STRING+getQueryString()));
				//				} catch (IOException e) {
				//					// TODO Auto-generated catch block
				//					e.printStackTrace();
				//				} catch (URISyntaxException e) {
				//					// TODO Auto-generated catch block
				//					e.printStackTrace();
				//				}

				jfxPanel.setScene(new Scene(view));








				WCTFrame frame = new WCTFrame(station);
				frame.getContentPane().setLayout(new BorderLayout());

				JPanel topPanel = new JPanel();
				JButton zoomInButton = new JButton("Zoom In");
				zoomInButton.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent e) {
						//                        view.setScaleX(view.getScaleX()*1.1);
						//                        view.setScaleY(view.getScaleY()*1.1);	


						Platform.runLater(new Runnable() {
							@Override public void run() {
								view.fontScaleProperty().set(view.fontScaleProperty().doubleValue()+0.1);
							}
						});
					}
				});
				JButton zoomOutButton = new JButton("Zoom Out");
				zoomOutButton.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent e) {
						//                        view.setScaleX(view.getScaleX()/1.1);
						//                        view.setScaleY(view.getScaleY()/1.1);		
						Platform.runLater(new Runnable() {
							@Override public void run() {
								view.fontScaleProperty().set(view.fontScaleProperty().doubleValue()-0.1);
							}
						});
					}
				});
				topPanel.add(zoomInButton);
				topPanel.add(zoomOutButton);
				frame.getContentPane().add(topPanel, BorderLayout.NORTH);
				frame.getContentPane().add(jfxPanel, BorderLayout.CENTER);
				frame.setSize(800, 600);
				frame.setVisible(true);

			}
		});




		//		JEditorPane htmlPane = new JEditorPane("text/html", html);
		//		frame.getContentPane().add(htmlPane, BorderLayout.CENTER);



		//		try {
		//			NativeInterface.runEventPump();
		//		} catch (Exception e) {
		//			;
		//		}
		System.out.println("mark 3");


	}



	// http://www.ncdc.noaa.gov/cdo-web/quickdatapdf/steve?datasetId=GHCND&productId=GHCN_DAILY_FORM&stationId=GHCND:US1GARB0006&year=2010&month=5&day=1
	private static String getQueryString(String station, String yyyy, String mm) {
		//		String dataset = "NORMAL_DLY";
		String dataset = "GHCND";
		//		String station = "GHCND:USW00003812";
//		String year = "2010";
//		String month = "5";
		String day = "1";
		//		String productID = "NORMAL_DLY_FORM";
		String productID = "GHCN_DAILY_FORM";

		StringBuilder sb = new StringBuilder();
		sb.append("?datasetId="+dataset);
		sb.append("&stationId="+station);
		sb.append("&year="+yyyy);
		sb.append("&month="+mm);
		sb.append("&day="+day);
		sb.append("&productId="+productID);

		return sb.toString();
	}



	public String process(String station) throws UnsupportedEncodingException {

		String dataset = "GHCND";
		//		String dataset = "NORMAL_DLY";
		//		String station = "GHCND:USW00003812";
		String year = "2010";
		String month = "5";
		String day = "1";
		String productID = "GHCN_DAILY_FORM";
		//		String productID = "NORMAL_DLY_FORM";


		HttpClient httpClient = new DefaultHttpClient();

		HttpPost postRequest = new HttpPost(SERVICE_URL_STRING);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("dataSetId", dataset));
		nvps.add(new BasicNameValuePair("stationId", station));
		nvps.add(new BasicNameValuePair("year", year));
		nvps.add(new BasicNameValuePair("month", month));
		nvps.add(new BasicNameValuePair("day", day));
		nvps.add(new BasicNameValuePair("productId", productID));
		postRequest.setEntity(new UrlEncodedFormEntity(nvps));


		final StringBuilder sb = new StringBuilder();
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();


				if (status != 200) {
					throw new IOException("ERROR IN WEB SERVICE REQUEST (statusCode="+status+")");
				}

				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();

					sb.append(entity != null ? EntityUtils.toString(entity) : null);

					return entity != null ? EntityUtils.toString(entity) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};


		// 	        String hasNumber = "-1";
		int statusCode = 0;
		try {


			httpClient.execute(postRequest, responseHandler);
			String data =sb.toString();




			return data;

		} catch (Exception e) {
			System.out.println("POST REQUEST STATUS CODE: "+statusCode);
			e.printStackTrace();
		} finally {
			postRequest.releaseConnection();
		}

		return "error.  status code="+statusCode;
	}


	// workaround:
	// The web resource that we POST to, returns HTML that makes
	// an AJAX call to get the div with the table of data.
	// The AJAX call uses a method 'getQuickData(data)' that is
	// in the CDO Javascript Library, but that method uses a 
	// relative link to the AJAX data servlet.  We have 2 options,
	// 1 - suck in the javascript library file and replace the local
	// url with the full url.  Or 2, override the function with local
	// javascript, which is what is done below...
	private static String getQuickdataAjaxFunction() {
		StringBuilder sb = new StringBuilder();
		sb.append("    $(\"#form\").hide();            \n");
		sb.append("    $(\"#loading\").show();         \n");
		sb.append("    $.ajax({                        \n");
		sb.append("        type: \"POST\",             \n");
		sb.append("        url: \""+AJAX_SERVICE_URL_STRING+"\",\n");
		sb.append("        data: data,                    \n");
		sb.append("        success: function (a) {     \n");
		sb.append("            $(\"#form\").html(a);   \n");
		sb.append("            $(\"#form\").show();    \n");
		sb.append("            $(\"#loading\").hide(); \n");
		sb.append("            $(\"#containerBox\").removeClass(\"initial\") \n");
		sb.append("        } \n");
		sb.append("    })    \n");
		return sb.toString();
	}

	public void deleteCachedForm() throws IOException {
		if (! CACHE_PDF.exists()) {
			return;
		}
		
		if (! CACHE_PDF.delete()) {
			throw new IOException("Unable to delete cached station form: "+CACHE_PDF);
		}
	}
	
	

	//	private static String getQuickdataFull() {

	//		StringBuilder sb = new StringBuilder();
	//		sb.append("\n");
	//	}
}
