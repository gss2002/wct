package gov.noaa.ncdc.wct.export;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Formatter;
import java.util.List;
import java.util.Vector;

import gov.noaa.ncdc.wct.WCTException;
import gov.noaa.ncdc.wct.WCTUtils;
import gov.noaa.ncdc.wct.event.DataDecodeEvent;
import gov.noaa.ncdc.wct.event.DataDecodeListener;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.unidata.geoloc.Station;

public class StationTimeseriesExport {

	private Writer writer = null;
	private boolean cancel = false;
	private StationTimeSeriesFeatureCollection stsfc = null;
	private boolean firstStationRead = true;
	private boolean firstWrite = true;

	private Vector<DataDecodeListener> listeners = new Vector<DataDecodeListener>();



	public StationTimeseriesExport(Writer w) {
		this.writer = w;
	}

	public void addDataDecodeListener(DataDecodeListener l) {
		listeners.add(l);
	}
	public void removeDataDecodeListener(DataDecodeListener l) {
		listeners.remove(l);
	}


	public void process(URL url) throws IOException {

		DataDecodeEvent event = new DataDecodeEvent(this);

		// Start decode
		// --------------
		for (int i = 0; i < listeners.size(); i++) {
			event.setProgress(0);
			listeners.get(i).decodeStarted(event);
		}

		try {


			Formatter fmter = new Formatter();
			FeatureDataset fd = FeatureDatasetFactoryManager.open(null, url.toString(), WCTUtils.getSharedCancelTask(), fmter);
			FeatureDatasetPoint fdp = null;
			if (fd != null && fd.getFeatureType().isPointFeatureType()) {
				fdp = (FeatureDatasetPoint)fd;
			}
			List<ucar.nc2.ft.FeatureCollection> pfcList = fdp.getPointFeatureCollectionList();
			this.stsfc = (StationTimeSeriesFeatureCollection)(pfcList.get(0));
			List<Station> stationList = stsfc.getStations();

			int cnt=0;
			for (Station station : stationList) {
				try {
					readStationData(station);
					cnt++;

				} catch (WCTException e) {
					e.printStackTrace();
				} catch (WCTExportNoDataException e) {
					e.printStackTrace();
				}

				for (int i = 0; i < listeners.size(); i++) {
					event.setProgress((int)(100*cnt/(double)stationList.size()));
					listeners.get(i).decodeProgress(event);
				}
			}
			
		} finally {
			for (int i = 0; i < listeners.size(); i++) {
				event.setProgress(0);
				listeners.get(i).decodeEnded(event);
			}
		}
	}





	public void readStationData(String id) throws WCTException, IOException, WCTExportNoDataException {
		readStationData(stsfc.getStation(id));
	}
	public void readStationData(Station station) throws WCTException, IOException, WCTExportNoDataException {
		if (stsfc == null) {
			throw new WCTException("No data file has been loaded");
		}

		// for (Station station : stationList) {
		StationTimeSeriesFeature sf = stsfc.getStationFeature(station);

		System.out.println("Station: "+station.toString());
		System.out.println("Location: "+sf.getLatLon());
		//		sb.append("## Station: "+station.toString()+" -- "+sf.getLatLon() +" \n");

		PointFeatureIterator pfIter = sf.getPointFeatureIterator(1*1024*1024);
		processFeatureIterator(station, pfIter);
	}



	private void processFeatureIterator(Station station, PointFeatureIterator pfIter) throws IOException, WCTExportNoDataException {
		//		JeksTableModel jeksTableModel = getTableModel();
		int c = 1;

		cancel = false;

		int row = 0;
		String[] columnNames = null;
		String[] columnInfo = null;
		DataType[] colTypes = null;
		StringBuilder sb = new StringBuilder();

		firstStationRead = true;

		// iterate through data for each station
		while (pfIter.hasNext()) {
			PointFeature pf = pfIter.next();

			// System.out.println( pf.getObservationTimeAsDate() + " -- " + pf.getLocation().toString());
			StructureData sdata = pf.getDataAll();
			StructureMembers smembers = sdata.getStructureMembers();
			// System.out.println( smembers.getMemberNames().toString() );


			if (firstStationRead) {
				List<String> nameList = smembers.getMemberNames();
				columnNames = new String[nameList.size()+1];
				columnInfo = new String[nameList.size()+1];
				colTypes = new DataType[nameList.size()+1];
				//                2010-03-23T12:34:00Z
				//				writer.append("#id, name, description, datetime");
				
				StringBuilder headerLine = new StringBuilder();
				headerLine.append("#datetime");
				columnNames[0] = "Date/Time";
				columnInfo[0] = "<html>Date/Time<br>Time Zone: UTC</html>";
				for (int n=0; n<nameList.size(); n++) {
					columnNames[n+1] = nameList.get(n);
					Member member = smembers.getMember(n);
					headerLine.append(", "+member.getName());
					if (member.getUnitsString() != null && member.getUnitsString().trim().length() > 0) {
						headerLine.append(" ("+member.getUnitsString()+")");
					}
					columnInfo[n+1] = "<html>"+member.getName()+"<br>"+
							member.getDescription()+"<br>"+
							member.getUnitsString()+"</html>";
					
										DataType dt = member.getDataType();
										System.out.println(member.getName() + ": "+member.getDataType());
										
					colTypes[n+1] = member.getDataType();
				}
				headerLine.append("\n");
				
				if (firstWrite) {
					writer.append(headerLine);
					firstWrite = false;
				}

				firstStationRead = false;
			}




			//			sb.append(station.getWmoId()).append(",");
			//			sb.append(station.getName()).append(",");
			//			sb.append("\"").append(station.getDescription()).append("\"").append(",");
			sb.append(stsfc.getTimeUnit().makeStandardDateString( pf.getObservationTime() ));
			//			sb.append(pf.getLocation().getLongitude()).append(",");
			//			sb.append(pf.getLocation().getLatitude()).append(",");
			//			sb.append(pf.getLocation().getAltitude()).append(",");
			//			jeksTableModel.setValueAt(stsfc.getTimeUnit().makeStandardDateString( pf.getObservationTime() ), row, 0);
			c = 1;
			for (String col : smembers.getMemberNames()) {
				
				String data = sdata.getScalarObject(col).toString();				
				
				if (colTypes[c] != null && (colTypes[c] == DataType.STRING || colTypes[c] == DataType.CHAR)) {
					sb.append(", \"").append(data).append("\"");
				}
				else {
					sb.append(", ").append(data);
				}
				c++;
			}
			//			textArea.append("\n");
			sb.append("\n");


			if (cancel) {
				return;
			}


			row++;

			if (row % 100 == 0) {
				System.out.println(".... processed "+row+" rows");
				writer.append(sb.toString());
				sb.setLength(0);
			}
		}

		// add remaining text
		writer.append(sb.toString());
		sb.setLength(0);

		//		// only format if there is not a excess of data
		//		if (row < 10000) {
		//			formatTextArea();
		//		}
		//		textArea.setForeground(Color.BLACK);




		if (columnNames == null) {
			throw new WCTExportNoDataException("No Data Found");
		}
		//		textArea.append(sb.toString());

		// System.out.println("before tablemodel copy");
		//		JeksTableModel jeksTableModel2 = new JeksTableModel(row, c, columnNames);
		//		for (int i=0; i<c; i++) {
		//			for (int j=0; j<row; j++) {
		//				jeksTableModel2.setValueAt(jeksTableModel.getValueAt(j, i), j, i);
		//			}
		//		}
		// System.out.println("after tablemodel copy");

		//		jeksTable.setModel(jeksTableModel2);
		//		jeksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// System.out.println("after setTableModel");
		//		autoFitColumns(jeksTable, columnNames);

		// textArea.append("\nDONE PROCESSING: "+station.getName());
		//		ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
		//		for (int i = 0; i < jeksTable.getColumnCount(); i++) {
		//			TableColumn col = jeksTable.getColumnModel().getColumn(i);
		//			tips.setToolTip(col, columnInfo[i]);
		//			col.setHeaderValue(columnNames[i]);
		//		}
		//		jeksTable.getTableHeader().addMouseMotionListener(tips);
	}


}
