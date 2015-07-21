package gov.pnnl.stucco.utilities;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/** 
 * Utility for generating a checksum (such as MD5 or SHA-1) for a file.
 */
public class FileChecksum {

    /** Converts a byte array to a two-chars-per-byte hex string. */
    private static String toHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        
        return builder.toString();
    }
    
    /**
     * Computes the checksum of a file. This uses a buffer, so it avoids having
     * to read the entire file into RAM. 
     * 
     * @param algorithm  The algorithm to use, such as MD5 or SHA-1.
     * @param file       File to checksum.
     * 
     * @return Lowercase hexadecimal checksum string
     */
    public static String compute(String algorithm, File file) throws IOException, NoSuchAlgorithmException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            // Get the algorithm
            MessageDigest digest = MessageDigest.getInstance(algorithm);
        
            // Read the file in chunks, updating checksum as we go
            byte[] buffer = new byte[8192]; // 8K
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
            
            // Get the result in hex format
            byte[] checksumArray = digest.digest();            
            String checksum = toHexString(checksumArray);
            return checksum;
        }
    }
    
    /**
     * Computes the checksum of a byte array.
     * 
     * @param algorithm  The algorithm to use, such as MD5 or SHA-1.
     * @param content    The byte array to checksum.
     * 
     * @return Lowercase hexadecimal checksum string
     */
    public static String compute(String algorithm, byte[] content) throws NoSuchAlgorithmException {
        // Get the algorithm
        MessageDigest digest = MessageDigest.getInstance(algorithm);
    
        // Compute checksum
        digest.update(content);

        // Get the result in hex format
        byte[] checksumArray = digest.digest();            
        String checksum = toHexString(checksumArray);
        return checksum;
    }
    
    public static void main(String[] args) {
        // Parse command line
        
        if (args.length != 2) {
            System.err.println("Usage: FileChecksum <hash> <file>");
            System.exit(1);
        }
        
        String algorithm = args[0];
        File file = new File(args[1]);
        
        if (!file.exists()) {
            System.err.format("\"%s\" not found", file);
            System.exit(2);
        }
        
        try {
            // Get the checksum
            String checksum = compute(algorithm, file);
            System.out.println(checksum);
        } 
        catch (NoSuchAlgorithmException nsae) {
            System.err.format("Unrecognized algorithm: %s%n", algorithm);
            System.exit(3);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
        try (
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file)); 
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000000);
        ) {
            // Read the file in chunks
            byte[] buffer = new byte[8192]; // 8K
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            
            // Convert to byte array
            byte[] content = out.toByteArray();
            
            // Compute checksum on byte array
            String checksum = compute(algorithm, content);
            System.out.println(checksum);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.exit(0);
    }


}
