package gov.noaa.ncdc.wct.decoders;

/**
 * NOAA's National Centers for Environmental Information
 * NOAA/NESDIS/NCEI
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class BlockBunzipper {
	static String AR = "AR";
	static int MAX_BLOCK_LEN = 2000000;
	
	public static int ERROR_MISSING_BLOCK_LEN = 0;
	public static int ERROR_LIST_LEN = 1;

	public static void main(String[] args){

		HashMap<String,String> opts = new HashMap<String,String>();
		ArrayList<String> realArgs = new ArrayList<String>();
		if (!processArgs(args, opts, realArgs)){
			printSyntax();
			System.exit(1);
		}

		boolean gzip = false;
		boolean debug = false;
		boolean ignoreError1 = false;
		
//		for (String opt:opts.keySet())
//			System.out.println("option: "+opt+"\t=\t"+opts.get(opt));
//		for (String arg:realArgs)
//			System.out.println("argument: "+arg);

		if (realArgs.size() != 2){
			System.err.println("Error: Wrong number of arguments");
			printSyntax();
			System.exit(1);
		}
		for (String opt:opts.keySet()){
			if (opt.equals("gzip"))
				gzip = true;
			else if (opt.equals("d"))
				debug = true;
			else if (opt.equals("ignoreError1"))
				ignoreError1 = true;
			else {
				System.err.println("Error: Unknown option: "+opt);
				printSyntax();
				System.exit(1);
			}
		}
		String fileIn = realArgs.get(0);
		String fileOut = realArgs.get(1);

		BlockBunzipper bun = new BlockBunzipper();
		bun.setDebug(debug);
		if (ignoreError1)
			bun.ignoreError(ERROR_MISSING_BLOCK_LEN);
		if (!bun.rezip(fileIn, fileOut, gzip)){
			System.err.println("Error(s) encountered while processing file");
			for (String err:bun.getErrors())
				System.err.println("BlockUnzipper Error: "+err);
		}
	}

	public static void printSyntax(){
		System.err.println("Syntax: BlockBunzipper [OPTION] [file_in] [file_out]");
		System.err.println("\t"+"file_in"+"\t"+"Filename to be extracted");
		System.err.println("\t"+"file_out"+"\t"+"Output filename");
		System.err.println("\t"+"OPTIONS");
		System.err.println("\t\t"+"-d"+"\t"+"debug mode");
		System.err.println("\t\t"+"-gzip"+"\t"+"gzip output file");
		System.err.println("\t\t"+"-ignoreError1"+"\t"+"ignore missing block length error");
	}

	/**
	 * Extract options of the form "-x=y", or "-z" into HashMap, and add non-option
	 * arguments into ArrayList.  Options in "-z" format will have null values.
	 * @param allArgs input arguments to parse
	 * @param opts map of options parsed
	 * @param realArgs list of non-option arguments
	 * @return true on success
	 */
	public static boolean processArgs(String[] allArgs,
			HashMap<String,String> opts,
			ArrayList<String> realArgs){
		if (realArgs == null || opts == null)
			throw new IllegalArgumentException("No (null) argument or option container provided to argument processor");
		if (allArgs == null)
			throw new IllegalArgumentException("Cannot process null arguments");

		String[] argParts;
		for (String arg:allArgs){
			if (arg.startsWith("-")){
				if (!arg.contains("=")){
					opts.put(arg.substring(1),null);
				} else {
					argParts = arg.split("=", 2);
					opts.put(argParts[0].substring(1), argParts[1]);
				}
			}
			else
				realArgs.add(arg);
		}

		return true;
	}

	
	
	
	/**
	 * Non static
	 */

	
	ArrayList<String> errors;
	boolean debug = false;
	boolean[] ignoreError = new boolean[ERROR_LIST_LEN];


	BlockBunzipper(){
		errors = new ArrayList<String>();
		for (int i=0;i<ignoreError.length;i++)
			ignoreError[i] = false;
	}

	/**
	 * If unzip operation returned failure, this method will return list of errors.
	 * @return array of error strings which occurred during processing
	 */
	public String[] getErrors(){
		return errors.toArray(new String[]{});
	}

	/**
	 * Ignore error identified by one of the statically identified ERROR values
	 * in the class.  If this error is encountered it will not be reported, and
	 * processing will continue in the best way possible as though this were not
	 * an error.
	 * @param errorNum an integer statically identified in this class (ERROR_X) 
	 * @return False if error specified is unknown.  Will not occur if statically
	 * identified ERROR_ fields are used.
	 */
	public boolean ignoreError(int errorNum){
		if (errorNum < 0 || errorNum > ERROR_LIST_LEN)
			return false;
		ignoreError[errorNum] = true;
		return true;
	}
	
	/**
	 * Turn on debug output.
	 * @param debug
	 */
	public void setDebug(boolean debug){
		this.debug = debug;
	}

	boolean gzip;
	ByteArrayInputStream bas;
	BZip2CompressorInputStream biz = null;
	OutputStream os;

	
	/**
	 * Rezip input file to output file.
	 * @param fileIn Block zipped file to be read
	 * @param fileOut output file
	 * @param gzipIn Whether output file will be gzipped
	 * @return False on failure.  See @getErrors for details on cause of failure.
	 * True on success
	 */
	public boolean rezip(String fileIn, String fileOut, boolean gzipIn){
		gzip = gzipIn;
		gzip = gzipIn;
		errors.clear();

		boolean worked = rezipInner(fileIn, fileOut);
		try{
			if (os != null){
				os.flush();
				os.close();
			}
		} catch (IOException e) {
			errors.add("Exception closing file: "+e.getMessage());
			if (debug) e.printStackTrace();
			worked = false;
		}
		
		return worked;
	}
	
	/**
	 * Not for you.
	 * @param fileIn
	 * @param fileOut
	 * @return
	 */
	public boolean rezipInner(String fileIn, String fileOut){


		try {
			if (debug) System.out.println("Opening file");
			File inf = new File(fileIn);
			FileInputStream fis = new FileInputStream(inf);

			if (debug) System.out.println("Creating output stream");
			OutputStream fos = new FileOutputStream(new File(fileOut));
			if (gzip)
				os = new GZIPOutputStream(fos);
			else
				os = fos;

			byte[] lenBytes = new byte[4];
			byte[] header = new byte[20];
			byte[] block = new byte[MAX_BLOCK_LEN];
			byte[] outBlock = new byte[MAX_BLOCK_LEN];
			long count = 0;
			int outBytes;

			boolean more = true;
			while (more){
				// get next block length
				if (fis.read(lenBytes) != 4){
					if (ignoreError[ERROR_MISSING_BLOCK_LEN])
						return true;						
					errors.add("Missing block length.");
					return false;
				}
				if (debug) System.out.println("Read length:"+getHexString(lenBytes));
				count += 4;

				// check for header
				if (debug) System.out.println("Checking bytes as characters :"+
						getHexString(Arrays.copyOfRange(lenBytes,0,2))+"x");
				if (AR.equals(new String(lenBytes, 0, 2))){
					 if(fis.read(header) != 20){
							errors.add("Invalid/missing header.");
							return false;
					 }
					 if (debug) System.out.println("Read header end:"+getHexString(header));

					 os.write(lenBytes);
					 os.write(header);

					 count += 20;
					 continue;
				}

				// reverse length
				Integer len = byteArrayToInt(lenBytes,0);
				if (debug) System.out.println("Length of block is:"+len);

				// check for special EOF
				if (len < 0){
					len = -len;
					more = false;
					if (debug) System.out.println("Found EOF");
				}

				if (len > MAX_BLOCK_LEN){
					errors.add("Unexpectedly long block length: "+len+">"+MAX_BLOCK_LEN);
					return false;
				}

				// more than 10 good, less bad
				if (len > 10){
					if (debug) System.out.println("Reading block len: "+len);

					if (fis.read(block,0,len) != len){
						errors.add("Block was shorter than indicated!");
						return false;
					}
					bas = new ByteArrayInputStream(block,0,len);
					biz = new BZip2CompressorInputStream(bas);
					while((outBytes = biz.read(outBlock,0,outBlock.length))>0){
						os.write(outBlock,0,outBytes);
						os.flush();
					}

					count += len;
				} else {
					if (debug) System.out.println("Skipping block.  We don't like short blocks.  No idea why.");
				}
			}
			if (debug) System.out.println("Read file :"+count);

			if (count != inf.length()){
				errors.add("ERROR: only read "+count+" bytes of "+inf.length()+"byte file!");
				return false;
			}


		} catch (IOException e) {
			errors.add("Exception processing file: "+e.getMessage());
			if (debug) e.printStackTrace();
			return false;
		} catch (Exception e){
			errors.add("Unexpected Exception processing file: "+e+": "+e.getMessage());
			if (debug) e.printStackTrace();
			return false;
		}

		if (debug) System.out.println("Successfully read file");
		return true;

	}

	static final byte[] HEX_CHAR_TABLE = {
	    (byte)'0', (byte)'1', (byte)'2', (byte)'3',
	    (byte)'4', (byte)'5', (byte)'6', (byte)'7',
	    (byte)'8', (byte)'9', (byte)'a', (byte)'b',
	    (byte)'c', (byte)'d', (byte)'e', (byte)'f'
	  };

	  public static String getHexString(byte[] raw)
	  {
	    byte[] hex = new byte[2 * raw.length];
	    int index = 0;

	    for (byte b : raw) {
	      int v = b & 0xFF;
	      hex[index++] = HEX_CHAR_TABLE[v >>> 4];
	      hex[index++] = HEX_CHAR_TABLE[v & 0xF];
	    }
	    try {
			return new String(hex, "ASCII");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	  }

	public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
	

}
