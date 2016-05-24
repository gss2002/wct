package gov.noaa.ncdc.wct.export;

public class ExportIsosurfaceOptions {
	
	private float[] isoValues;
	private String[] isoColors;
	
	
	public float[] getIsoValues() {
		return isoValues;
	}
	public void setIsoValues(float[] isoValues) {
		this.isoValues = isoValues;
	}
	public String[] getIsoColors() {
		return isoColors;
	}
	public void setIsoColors(String[] isoColors) {
		this.isoColors = isoColors;
	}
	
	
}
