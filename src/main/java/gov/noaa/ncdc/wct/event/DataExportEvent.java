/**
 * NOAA's National Climatic Data Center
 * NOAA/NESDIS/NCDC
 * 151 Patton Ave, Asheville, NC  28801
 * 
 * THIS SOFTWARE AND ITS DOCUMENTATION ARE CONSIDERED TO BE IN THE 
 * PUBLIC DOMAIN AND THUS ARE AVAILABLE FOR UNRESTRICTED PUBLIC USE.  
 * THEY ARE FURNISHED "AS IS." THE AUTHORS, THE UNITED STATES GOVERNMENT, ITS
 * INSTRUMENTALITIES, OFFICERS, EMPLOYEES, AND AGENTS MAKE NO WARRANTY,
 * EXPRESS OR IMPLIED, AS TO THE USEFULNESS OF THE SOFTWARE AND
 * DOCUMENTATION FOR ANY PURPOSE. THEY ASSUME NO RESPONSIBILITY (1)
 * FOR THE USE OF THE SOFTWARE AND DOCUMENTATION; OR (2) TO PROVIDE
 * TECHNICAL SUPPORT TO USERS.
 */

package gov.noaa.ncdc.wct.event;

// Imports
// -------
import java.io.File;
import java.net.URL;
import java.util.EventObject;

/**
 * A ftp transfer event handles the details of a data transfer.  Data
 * transfer events are generated by a <code>FTPThread</code> object
 * to signal the start, progress, stop, and error in a data transfer
 * operation.
 */
public class DataExportEvent extends EventObject {

     
  private URL dataURL;
  private File outputFile;
  private int progress;
  private String status;
     
  ////////////////////////////////////////////////////////////

  /** Create a new data transfer event. */
  public DataExportEvent (Object source, URL dataURL, File outputFile) { 
 
    super (source);

    this.dataURL = dataURL;
    this.outputFile = outputFile;    

  } // NexradExportEvent

  ////////////////////////////////////////////////////////////
  
  
  public URL getDataURL() {
     return dataURL;
  }
  
  public File getOutputFile() {
     return outputFile;
  }
  
  public void setProgress(int progress) {
     this.progress = progress;
  }
  
  public int getProgress() {
     return progress;
  }
  
  /**
	 * Returns the value of status.
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * Sets the value of status.
	 * @param status The value to assign status.
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

  
  

} // NexradExportEvent class

////////////////////////////////////////////////////////////////////////