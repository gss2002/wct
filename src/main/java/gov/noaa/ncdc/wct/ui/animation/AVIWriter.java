package gov.noaa.ncdc.wct.ui.animation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;




/**
This saves BufferedImage objects into an uncompressed AVI movie format. 
It is based on the AVIWriter class in ImageJ, which
in turn is based on the FileAvi class written by
William Gandler. The FileAvi class is part of Matthew J. McAuliffe's MIPAV program,
available from http://mipav.cit.nih.gov/.

Major changes from Steve.Ansari@noaa.gov modifications: <br>
1)  Complete separation of AVIWriter from ImageJ API <br>
2)  'Streaming' capability, where frames can be added one at a time as available.
  This eliminates the need for a large BufferedImage array in memory.

 */
public class AVIWriter {
    
    private RandomAccessFile raFile;
    private int bytesPerPixel;

    private int           xDim,yDim,zDim,tDim;
    private int           microSecPerFrame;
    private int           xPad;
    private byte[]      bufferWrite;



    private long saveFileSize; // location of file size in bytes not counting first 8 bytes
    private byte[] dataSignature;
    private long idx1Pos;
    private long saveidx1Length;
    private long savedbLength[];
    private long saveLIST2Size;
    private long savemovi;
    private long endPos;


    private int frameCount = 0;
    private double frameRateInFPS;


    /**
     * Initializes the AVI file
     * @param outFile
     * @param width  number of pixels in x direction
     * @param height  number of pixels in y direction
     * @param numFrames  number of frames expected in this movie
     * @param frameRateInFPS  frame rate in frames per second
     * @throws IOException
     */
    public void init(File outFile, int width, int height, int numFrames, double frameRateInFPS) throws IOException {


        System.out.println("WRITING AVI TO: "+outFile);
        
        this.frameRateInFPS = frameRateInFPS;




        long saveLIST1Size; // location of length of CHUNK with first LIST - not including
        // first 8 bytes with LIST and size.  JUNK follows the end of 
        // this CHUNK
        long saveLIST1subSize; // location of length of CHUNK with second LIST - not including
        // first 8 bytes with LIST and size.  Note that saveLIST1subSize =
        // saveLIST1Size + 76, and that the length size written to 
        // saveLIST2Size is 76 less than that written to saveLIST1Size.
        // JUNK follows the end of this CHUNK.
        long savestrfSize; // location of lenght of strf CHUNK - not including the first
        // 8 bytes with strf and size.  strn follows the end of this
        // CHUNK.
        long savestrnPos;
        long saveJUNKsignature;
        int paddingBytes;
        int i;
        long savedcLength[];
        int xMod;

        bytesPerPixel = 3;

        raFile = new RandomAccessFile(outFile, "rw");
        raFile.setLength(0);
//      imp.startTiming();
        writeString("RIFF"); // signature
        saveFileSize = raFile.getFilePointer();
        // Bytes 4 thru 7 contain the length of the file.  This length does
        // not include bytes 0 thru 7.
        writeInt(0); // for now write 0 in the file size location
        writeString("AVI "); // RIFF type

        // Write the first LIST chunk, which contains information on data decoding
        writeString("LIST"); // CHUNK signature
        // Write the length of the LIST CHUNK not including the first 8 bytes with LIST and
        // size.  Note that the end of the LIST CHUNK is followed by JUNK.
        saveLIST1Size = raFile.getFilePointer();
        writeInt(0); // for now write 0 in avih sub-CHUNK size location
        writeString("hdrl"); // CHUNK type
        writeString("avih"); // Write the avih sub-CHUNK        
        writeInt(0x38); // Write the length of the avih sub-CHUNK (38H) not including the
        // the first 8 bytes for avihSignature and the length
        microSecPerFrame = (int)((1.0/getFrameRate())*1.0e6);
        writeInt(microSecPerFrame); // dwMicroSecPerFrame - Write the microseconds per frame
        writeInt(0); // dwMaxBytesPerSec (maximum data rate of the file in bytes per second)
        writeInt(0); // dwReserved1 - Reserved1 field set to zero
        writeInt(0x10); // dwFlags - just set the bit for AVIF_HASINDEX
        // 10H AVIF_HASINDEX: The AVI file has an idx1 chunk containing
        //   an index at the end of the file.  For good performance, all
        //   AVI files should contain an index.        
        tDim = 1;
        zDim = numFrames;
        yDim = height;
        xDim = width;

        xPad = 0;
        xMod = xDim%4;
        if (xMod != 0) {
            xPad = 4 - xMod;
            xDim = xDim + xPad;
        }

        writeInt(zDim*tDim); // dwTotalFrames - total frame number
        writeInt(0); // dwInitialFrames -Initial frame for interleaved files. 
        // Noninterleaved files should specify 0.
        writeInt(1); // dwStreams - number of streams in the file - here 1 video and zero audio.
        writeInt(0); // dwSuggestedBufferSize - Suggested buffer size for reading the file.
        // Generally, this size should be large enough to contain the largest
        // chunk in the file.
        writeInt(xDim-xPad); // dwWidth - image width in pixels
        writeInt(yDim); // dwHeight - image height in pixels
        // dwReserved[4] - Microsoft says to set the following 4 values to 0.
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeInt(0);

        // Write the Stream line header CHUNK
        writeString("LIST"); 
        // Write the size of the first LIST subCHUNK not including the first 8 bytes with 
        // LIST and size.  Note that saveLIST1subSize = saveLIST1Size + 76, and that
        // the length written to saveLIST1subSize is 76 less than the length written to saveLIST1Size.
        // The end of the first LIST subCHUNK is followed by JUNK.
        saveLIST1subSize = raFile.getFilePointer();
        writeInt(0); // for now write 0 in CHUNK size location    
        writeString("strl");   // Write the chunk type
        writeString("strh"); // Write the strh sub-CHUNK
        writeInt(56); // Write the length of the strh sub-CHUNK
        writeString("vids"); // fccType - Write the type of data stream - here vids for video stream
        // Write DIB for Microsoft Device Independent Bitmap.  Note: Unfortunately,
        // at least 3 other four character codes are sometimes used for uncompressed
        // AVI videos: 'RGB ', 'RAW ', 0x00000000
        writeString("DIB ");
        writeInt(0); // dwFlags
        writeInt(0); // dwPriority
        writeInt(0); // dwInitialFrames
        writeInt(1); // dwScale
        writeInt((int)getFrameRate()); //  dwRate - frame rate for video streams
        writeInt(0); // dwStart - this field is usually set to zero
        writeInt(tDim*zDim); // dwLength - playing time of AVI file as defined by scale and rate
        // Set equal to the number of frames
        writeInt(0); // dwSuggestedBufferSize - Suggested buffer size for reading the stream.
        // Typically, this contains a value corresponding to the largest chunk
        // in a stream.
        writeInt(-1); // dwQuality - encoding quality given by an integer between 
        // 0 and 10,000.  If set to -1, drivers use the default 
        // quality value.
        writeInt(0); // dwSampleSize #
        // write 16-bit frame (unused)
        writeShort((short)0); // left
        writeShort((short)0); // top
        writeShort((short)0); // right
        writeShort((short)0); // bottom      
        writeString("strf"); // Write the stream format chunk
        // Write the size of the stream format CHUNK not including the first 8 bytes for
        // strf and the size.  Note that the end of the stream format CHUNK is followed by
        // strn.
        savestrfSize = raFile.getFilePointer();
        writeInt(0); // for now write 0 in the strf CHUNK size location
        writeInt(40); // biSize - Write header size of BITMAPINFO header structure
        // Applications should use this size to determine which BITMAPINFO header structure is 
        // being used.  This size includes this biSize field.
        writeInt(xDim);  // biWidth - BITMAP width in pixels
        writeInt(yDim);  // biHeight - image height in pixels.  If height is positive,
        // the bitmap is a bottom up DIB and its origin is in the lower left corner.  If 
        // height is negative, the bitmap is a top-down DIB and its origin is the upper
        // left corner.  This negative sign feature is supported by the Windows Media Player, but it is not
        // supported by PowerPoint.
        writeShort(1); // biPlanes - number of color planes in which the data is stored
        // This must be set to 1.
        int bitsPerPixel = (bytesPerPixel==3) ? 24 : 8;
        writeShort((short)bitsPerPixel); // biBitCount - number of bits per pixel #
        // 0L for BI_RGB, uncompressed data as bitmap
        //writeInt(bytesPerPixel*xDim*yDim*zDim*tDim); // biSizeImage #
        writeInt(0); // biSizeImage #
        writeInt(0); // biCompression - type of compression used
        writeInt(0); // biXPelsPerMeter - horizontal resolution in pixels
        writeInt(0); // biYPelsPerMeter - vertical resolution in pixels
        // per meter
        if (bitsPerPixel==8)
            writeInt(256); // biClrUsed 
        else
            writeInt(0); // biClrUsed 
        writeInt(0); // biClrImportant - specifies that the first x colors of the color table 
        // are important to the DIB.  If the rest of the colors are not available,
        // the image still retains its meaning in an acceptable manner.  When this
        // field is set to zero, all the colors are important, or, rather, their
        // relative importance has not been computed.
        // Write the LUTa.getExtents()[1] color table entries here.  They are written:
        // blue byte, green byte, red byte, 0 byte

        // Use strn to provide a zero terminated text string describing the stream
        savestrnPos = raFile.getFilePointer();
        raFile.seek(savestrfSize);
        writeInt((int)(savestrnPos - (savestrfSize+4)));
        raFile.seek(savestrnPos);
        writeString("strn");
        writeInt(16); // Write the length of the strn sub-CHUNK
        writeString("FileAVI write  ");
        raFile.write(0); // write string termination byte

        // write a JUNK CHUNK for padding
        saveJUNKsignature = raFile.getFilePointer();
        raFile.seek(saveLIST1Size);
        writeInt((int)(saveJUNKsignature - (saveLIST1Size+4)));
        raFile.seek(saveLIST1subSize);
        writeInt((int)(saveJUNKsignature - (saveLIST1subSize+4)));
        raFile.seek(saveJUNKsignature);
        writeString("JUNK");
        paddingBytes = (int)(4084 - (saveJUNKsignature + 8));
        writeInt(paddingBytes);
        for (i = 0; i < (paddingBytes/2); i++)
            writeShort((short)0);

        // Write the second LIST chunk, which contains the actual data
        writeString("LIST");
        // Write the length of the LIST CHUNK not including the first 8 bytes with LIST and
        // size.  The end of the second LIST CHUNK is followed by idx1.
        saveLIST2Size = raFile.getFilePointer();
        writeInt(0);  // For now write 0
        savemovi = raFile.getFilePointer();       
        writeString("movi"); // Write CHUNK type 'movi'
        savedbLength = new long[tDim*zDim];
        savedcLength = new long[tDim*zDim];
        
        dataSignature = new byte[4];
        dataSignature[0] = 48; // 0
        dataSignature[1] = 48; // 0
        dataSignature[2] = 100; // d
        dataSignature[3] = 98; // b

        // Write the data.  Each 3-byte triplet in the bitmap array represents the relative intensities
        // of blue, green, and red, respectively, for a pixel.  The color bytes are in reverse order
        // from the Windows convention.
        bufferWrite = new byte[bytesPerPixel*xDim*yDim];

    }

    /**
     * Add an image frame to the movie
     * @param bimage
     * @throws IOException
     */
    public void addFrame(BufferedImage bimage) throws IOException {

        raFile.write(dataSignature);
        savedbLength[frameCount++] = raFile.getFilePointer();
        writeInt(bytesPerPixel*xDim*yDim); // Write the data length
        writeRGBFrame(bimage);

    }

    /**
     * Finish the movie - this MUST be called to set up the correct 
     * index of frame locations.
     * @throws IOException
     */
    public void close() throws IOException {

        // Write the idx1 CHUNK
        // Write the 'idx1' signature
        idx1Pos = raFile.getFilePointer();
        raFile.seek(saveLIST2Size);
        writeInt((int)(idx1Pos - (saveLIST2Size + 4)));
        raFile.seek(idx1Pos);
        writeString("idx1");
        // Write the length of the idx1 CHUNK not including the idx1 signature and the 4 length
        // bytes. Write 0 for now.
        saveidx1Length = raFile.getFilePointer();
        writeInt(0);
        for (int z = 0; z < zDim; z++) {
            // In the ckid field write the 4 character code to identify the chunk 00db or 00dc
            raFile.write(dataSignature);
            if (z == 0) {
                writeInt(0x10); // Write the flags - select AVIIF_KEYFRAME
            }
            else {
                writeInt(0x00);
            }
            // AVIIF_KEYFRAME 0x00000010L
            // The flag indicates key frames in the video sequence.
            // Key frames do not need previous video information to be decompressed.
            // AVIIF_NOTIME 0x00000100L The CHUNK does not influence video timing(for 
            //   example a palette change CHUNK).
            // AVIIF_LIST 0x00000001L Marks a LIST CHUNK.
            // AVIIF_TWOCC 2L
            // AVIIF_COMPUSE 0x0FFF0000L These bits are for compressor use.
            writeInt((int)(savedbLength[z]- 4 - savemovi)); 
            // Write the offset (relative to the 'movi' field) to the relevant CHUNK
            writeInt(bytesPerPixel*xDim*yDim); // Write the length of the relevant
            // CHUNK.  Note that this length is
            // also written at savedbLength
        }  // for (z = 0; z < zDim; z++)
        endPos = raFile.getFilePointer();
        raFile.seek(saveFileSize);
        writeInt((int)(endPos - (saveFileSize+4)));
        raFile.seek(saveidx1Length);
        writeInt((int)(endPos - (saveidx1Length+4)));
        raFile.close();
    }

    private void writeRGBFrame(BufferedImage image) throws IOException {

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int c, offset, index = 0;
        for (int y=height-1; y>=0; y--) {
            offset = y*width;
            for (int x=0; x<width; x++) {
                c = pixels[offset++];
                bufferWrite[index++] = (byte)(c&0xff); // blue
                bufferWrite[index++] = (byte)((c&0xff00)>>8); //green
                bufferWrite[index++] = (byte)((c&0xff0000)>>16); // red
            }
            for (int i = 0; i<xPad; i++) {
                bufferWrite[index++] = (byte)0;
                bufferWrite[index++] = (byte)0;
                bufferWrite[index++] = (byte)0;
            }
        }
        raFile.write(bufferWrite);
    }


    /**
     * Returns the frame rate in frames per second
     */
    public double getFrameRate() {
        return frameRateInFPS;
    }


    final private void writeString(String s) throws IOException {
        //byte[] bytes =  s.getBytes();
        byte[] bytes =  s.getBytes("UTF8");
        raFile.write(bytes);
    }

    final private void writeInt(int v) throws IOException {
        raFile.write(v & 0xFF);
        raFile.write((v >>>  8) & 0xFF);
        raFile.write((v >>> 16) & 0xFF);
        raFile.write((v >>> 24) & 0xFF);
    }

    final private void writeShort(int v) throws IOException {
        raFile.write(v& 0xFF);
        raFile.write((v >>> 8) & 0xFF);
    }








    /**
     * Writes out an array of BufferedImage objects to an uncompressed AVI
     * @param bimages Array of images
     * @param frameRateInFPS  Frame rate in frames per second
     * @param outFile  Output file
     * @throws IOException
     */
    public static void writeImages(BufferedImage[] bimages, double frameRateInFPS, File outFile) throws IOException {
        AVIWriter aviWriter = new AVIWriter();
        int width = bimages[0].getWidth(null);
        int height = bimages[0].getHeight(null);
        aviWriter.init(outFile, width, height, bimages.length, frameRateInFPS);
        for (int n=0; n<bimages.length; n++) {
            aviWriter.addFrame(bimages[n]);
        }
        aviWriter.close();
    }


}
