package gov.pnnl.stucco.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;


/** Utilities for unpackaging content that has has tar and/or gzip applied. */
public class UnpackUtils {

    private UnpackUtils() {
    }

    /** Unpackages single file content that has been had tar and gzip applied. */
    public static byte[] unTarGzip(byte[] content) throws IOException {
        byte[] untarred = new byte[0];
        
        try (
                ByteArrayInputStream compressedIn = new ByteArrayInputStream(content);
                GzipCompressorInputStream uncompressedIn = new GzipCompressorInputStream(compressedIn);
                TarArchiveInputStream in = new TarArchiveInputStream(uncompressedIn);
                ) {
            untarred = unTar(in);
        }
        
        return untarred;    
    }

    /** Unpackages single file content that has been had gzip applied. */
    public static byte[] unGzip(byte[] content) throws IOException {
        try (
                ByteArrayInputStream compressedIn = new ByteArrayInputStream(content);
                GzipCompressorInputStream uncompressedIn = new GzipCompressorInputStream(compressedIn);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ) {
            final byte[] buffer = new byte[8192];
            int n;
            while ((n = uncompressedIn.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            byte[] uncompressed = out.toByteArray();
            return uncompressed;
        }
    }
    
    /** Unpackages single file content that has been had tar applied. */
    public static byte[] unTar(byte[] content) throws IOException {
        try (
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(content);
                TarArchiveInputStream in = new TarArchiveInputStream(bytesIn);
                ) {
            byte[] untarred = unTar(in);
            return untarred;
        }
    }

    /** Unpackages single file content that has been had tar applied. */
    static private byte[] unTar(TarArchiveInputStream in) throws IOException {
        try {
            // Get first entry
            ArchiveEntry tarEntry = in.getNextEntry();
            ByteArrayOutputStream out = null;
            
            if (tarEntry != null  &&  !tarEntry.isDirectory()) {
                // Pull bytes from container into array stream
                out = new ByteArrayOutputStream();
                final byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                out.close();
            }
            
            tarEntry = in.getNextEntry();
            if (tarEntry != null) {
                throw new IOException("Multiple-entry tar");
            }
            
            // Convert output stream to array
            byte[] untarred = new byte[0];
            if (out != null) {        
                untarred = out.toByteArray();
            }
            return untarred;
        }
        finally {
            in.close();
        }
    }
       
    /** Utility method for reading a file, used for testing. */
    static private byte[] readFile(File f) {
        // Get the content as a byte array, and compute its checksum
        byte[] content = new byte[0];
        try (
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
                ByteArrayOutputStream out = new ByteArrayOutputStream()
                ) {
            // Get a chunk at a time 
            byte[] buffer = new byte[8192]; // 8K
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            content = out.toByteArray();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
    
    public static void main(String[] args) throws Exception {
        byte[] raw, processed;
//        {
//            File file = new File("test_config.yml.tar.gz");
//            raw = readFile(file);
//            processed = unTarGzip(raw);
//        }
//        {
//            File file = new File("test_config.yml.gz");
//            raw = readFile(file);
//            processed = unGzip(raw);
//        }
        {
            File file = new File("test_config.yml.tar");
            raw = readFile(file);
            processed = unTar(raw);
        }
        
        System.err.println(new String(processed));
        return;
    }
}
