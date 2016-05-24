package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Characterises the compression status of a stream, and provides methods
 * for decompressing it.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class Compression {

    private String name;

    /** Number of bytes needed to determine compression type (magic number). */
    public static final int MAGIC_SIZE = 3;

    /**
     * Private sole constructor.
     *
     * @param   name  the name of this compression method
     */
    private Compression( String name ) {
        this.name = name;
    }

    /**
     * Returns a stream which is a decompressed version of the input stream,
     * according to this objects compression type.
     *
     * @param  raw  the raw input stream
     * @return  a stream giving the decompressed version of <tt>raw</tt>
     */
    public abstract InputStream decompress( InputStream raw ) 
            throws IOException;

    /**
     * Returns a Compression object characterising the compression (if any)
     * represented by a given magic number.
     *
     * @param  magic  a buffer containing the first {@link #MAGIC_SIZE}
     *         bytes of input of the stream to be characterised
     * @return  a <tt>Compression</tt> object of the type represented by
     *          <tt>magic</tt>
     * @throws IllegalArgumentException  if <tt>magic.length&lt;MAGIC_SIZE</tt>
     */
    public static Compression getCompression( byte[] magic ) {
        if ( magic.length < MAGIC_SIZE ) {
            throw new IllegalArgumentException( 
                "Magic buffer must be at least MAGIC_SIZE=" + MAGIC_SIZE + 
                " bytes" );
        }
        else if ( magic[ 0 ] == (byte) 0x1f && 
                  magic[ 1 ] == (byte) 0x8b ) {
            return GZIP;
        }
        else if ( magic[ 0 ] == (byte) 'B' &&
                  magic[ 1 ] == (byte) 'Z' &&
                  magic[ 2 ] == (byte) 'h' ) {
            return BZIP2;
        }
        else if ( magic[ 0 ] == (byte) 0x1f &&
                  magic[ 1 ] == (byte) 0x9d ) {
            return COMPRESS;
        }
        else {
            return NONE;
        }
    }

    /**
     * Returns a decompressed version of the given input stream.
     *
     * @param  raw  the raw input stream
     * @return  the decompressed version of <tt>raw</tt>
     */
    public static InputStream decompressStatic( InputStream raw ) 
             throws IOException {
        if ( ! raw.markSupported() ) {
            raw = new BufferedInputStream( raw );
        }
        raw.mark( MAGIC_SIZE );
        byte[] buf = new byte[ MAGIC_SIZE ];
        raw.read( buf );
        raw.reset();
        Compression compress = getCompression( buf );
        return compress.decompress( raw );
    }

    /**
     * Returns the name of this compression type.
     *
     * @return  string representation
     */
    public String toString() {
        return name;
    }

    /**
     * A Compression object representing no compression (or perhaps an
     * unknown one).  The <tt>decompress</tt> method will return the
     * raw input stream unchanged.
     */
    public static final Compression NONE = new Compression( "none" ) {
        public InputStream decompress( InputStream raw ) throws IOException {
//System.out.println("NO COMPRESSION FOUND");       
            return raw;
        }
    };

    /**
     * A Compression object representing GZip compression.
     */
    public static final Compression GZIP = new Compression( "gzip" ) {
        public InputStream decompress( InputStream raw ) throws IOException {
//System.out.println("GZIP COMPRESSION FOUND");       

            /* This is a workaround for a bug in GZIPInputStream in J2SE1.4.0
             * GZIPInputStream.markSupported() returns true; however
             * instances of this class do not support marking, which
             * screws up some things that the DataSource class tries to do.
             * So we fiddle the inflating stream to tell the truth. */
            /* Note this seems to be not uncommon in decompression streams
             * (had to fix the same bug in the UncompressInputStream 
             * implementation used here too). */
            /* (bug ID 4812237 submitted to developer.java.sun.com by mbt) */
            return new GZIPInputStream( raw ) {
                public boolean markSupported() {
                    return false;
                }
            };
        }
    };

    /**
     * A Compression object representing BZip2 compression.
     */
    public static final Compression BZIP2 = new Compression( "bzip2" ) {
        public InputStream decompress( InputStream raw ) throws IOException {
//System.out.println("BZIP2 COMPRESSION FOUND");       

            /* Eat the first two bytes. */
            if ( raw.read() != 'B' || raw.read() != 'Z' ) {
                throw new IllegalArgumentException( 
                    "Wrong magic number for bzip2 encoding" );
            }
            return new CBZip2InputStream( raw );
        }
    };

    /**
     * A Compression object representing Unix compress-type compression.
     */
    public static final Compression COMPRESS = new Compression( "compress" ) {
        public InputStream decompress( InputStream raw ) throws IOException {
//System.out.println("UNIZ .Z COMPRESSION FOUND");       
            return new UncompressInputStream( raw );
        }
    };
}

