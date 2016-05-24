package steve.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;
import ftp.FtpException;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.io.DirectoryScanner;
import gov.noaa.ncdc.wct.io.DirectoryScanner.ListResults;

/**
 *  
 * @author Steve.Ansari 
 * 
 * 1. Read all XML ISO files in a directory (WAF)
 * 2. Pull out all URLs referenced by CI_OnlineResource tags
 * 3. Create Unique List of CI_OnlineResource URLs for each ISO file (collection)
 * 4. Create Unique List of CI_OnlineResource URLs overall
 * 5. Take #4 and count exact or partial matches (such as basepath of web app) in web logs
 * 6. Apply counts back to #3 to identify which records link to popular resources
 *
 */



public class MetadataParser {

	public final static String[] BLACK_LIST_URLS_TO_EXCLUDE = 
		{ "http://www.ncdc.noaa.gov",
		  "http://www.ncdc.noaa.gov/"
		};

	public final static String[] BLACK_LIST_EXTENSIONS_TO_EXCLUDE = 
		{ ".gif", ".png", ".jpg", ".jpeg", ".js", ".css"
		};

	public static void main(String[] args) {
		try {
			URL dir = new URL("http://www1.ncdc.noaa.gov/pub/data/metadata/published/geoportal/iso/xml/");
			URL weblog = new File("C:\\work\\weblogs\\ncdc-hostgw1.20150521").toURI().toURL();
			
			processDir(dir, weblog);



		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	private static void processDir(URL wafURL, URL weblogURL) throws MalformedURLException, FtpException, IOException, XPathExpressionException, ParserConfigurationException, SAXException {

		DirectoryScanner dirScanner = new DirectoryScanner();
		ListResults listResults = dirScanner.listURL(wafURL);

		HashMap<URL, List<String>> urlMap = new HashMap<URL, List<String>>();
		HashSet<String> overallResourceList = new HashSet<String>();

		// 1. Populate the overall and per ISO record lists of referenced CI_OnlineResource URLs
		URL[] urlList = listResults.getUrlList();
		for (URL url : urlList) {
			if (url.toString().endsWith(".xml")) {
				List<String> resourceList = processISOFile(url);

				// add to list of unique CI_OnlineResource locations per URL				
				urlMap.put(url, resourceList);

				// add to list of unique CI_OnlineResource locations overall
				overallResourceList.addAll(resourceList);
			}
		}
		
		// clean up resource list
		removeBlackListURLs(overallResourceList); 
		

		// 2. Read web logs and count partial or full matches to overallResourceList
		HashMap<String, Integer> overallResourceCount = new HashMap<String, Integer>();
		// initialize keys
		for (String s : overallResourceList) {
			overallResourceCount.put(s, 0);
		}
		
		processWebLogs(weblogURL, overallResourceCount);
		
		Map<String, Integer> sortedResourceCount = sortByComparator(overallResourceCount, false);
		printMap(sortedResourceCount);
		
	}
	
	
	
	// from http://stackoverflow.com/questions/8119366/sorting-hashmap-by-values
	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
    public static void printMap(Map<String, Integer> map) {
        for (Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : "+ entry.getValue());
        }
    }

	
	
	private static void removeBlackListURLs(HashSet<String> set) {
		for (String s : BLACK_LIST_URLS_TO_EXCLUDE) {
			set.remove(s);
		}
	}


	private static void processWebLogs(URL url, HashMap<String, Integer> matchCount) throws IOException {

		Set<String> matchList = matchCount.keySet();

		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		CSVReader reader = new CSVReader(in, ' ');
		String[] cols = null;
		while ((cols = reader.readNext()) != null) {
			//    		  System.out.println(Arrays.toString(cols));
			if (cols.length > 7) {
				String host = cols[5];
				String getRequest = cols[7];
				if (getRequest.indexOf(" ") < 0) {
					System.err.println("bad GET request: "+getRequest);
				}
				else {
					String file = getRequest.split(" ")[1];


					for (String resource : matchList) {
						String requestedURL = "http://"+host+file;
						if (requestedURL.contains(resource) && isAccepted(requestedURL)) {

							System.out.println(resource + " linked to :::: "+requestedURL);
							matchCount.put(resource, new Integer(matchCount.get(resource) + 1));
						}

					}
				}
			}

		}
		reader.close();

		

	}
	
	private static boolean isAccepted(String requestedURL) {
		for (String s : BLACK_LIST_EXTENSIONS_TO_EXCLUDE) {
			if (requestedURL.endsWith(s)) {
				return false;
			}
		}
		return true;
	}


	private static List<String> processISOFile(URL url) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {
			@Override
			public Iterator getPrefixes(String namespaceURI) {
				return null;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return null;
			}

			@Override
			public String getNamespaceURI(String prefix) {
				if (prefix.equals("gmd")) {
					return "http://www.isotc211.org/2005/gmd";
				}
				else {
					return XMLConstants.NULL_NS_URI;
				}
			}
		});

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = domFactory.newDocumentBuilder();


		Document doc = builder.parse(url.toString());

		//		String urlString = WCTUtils.getXPathValue(doc, xpath, "//gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL/text()");
		List<String> urlList = WCTUtils.getXPathValues(doc, xpath, "//gmd:CI_OnlineResource/gmd:linkage/gmd:URL/text()");
		return removeDuplicate(urlList);
	}



	public static List<String> removeDuplicate(List<String> arlList) {
		HashSet<String> h = new HashSet<String>(arlList);
		ArrayList<String> returnList = new ArrayList<String>();
		returnList.addAll(h);
		return returnList;
	}
}
