package gov.noaa.ncdc.wct.decoders.sigmet;

import java.io.IOException;
import java.util.Formatter;

import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.util.CancelTask;

public class SigmetDatasetFactory implements FeatureDatasetFactory {

	@Override
	public FeatureType[] getFeatureType() {
		return new FeatureType[] { FeatureType.STATION_RADIAL, FeatureType.RADIAL };
	}

	@Override
	public Object isMine(FeatureType ft, NetcdfDataset ds, Formatter fmt)
			throws IOException {
		

		Attribute ga = ds.findGlobalAttribute("definition");
        if (ga != null ) {
        	if (ga.getStringValue().equalsIgnoreCase("SIGMET-IRIS RAW")) {
//        		System.out.println("isMine true !!!!!!!!!!!!!!!!!!!!!!!!!!! :)");
        		return true;
        	}
        }
//        System.out.println("isMine FALSEEEEEEEEE: "+ ds.toString());
        return null;
	}

	
	
	@Override
	public FeatureDataset open(FeatureType ft, NetcdfDataset ds,
			Object analysis, CancelTask cancelTask, Formatter fmt) throws IOException {
		
		System.out.println("SIGMET OPEN !!!!!!!!!!!!!!!!!!!!!!!!");
		return new SigmetDataset(ds);
	}

}
