package org.chaos.office.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * ImageHelper utility class for image loading and conversion operations.
 * Provides methods to convert between byte arrays, JavaFX Images, and files.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Converting byte arrays to JavaFX Image objects</li>
 *   <li>Converting JavaFX Image objects to byte arrays</li>
 *   <li>Loading images from files and converting to byte arrays</li>
 *   <li>Supporting PNG and JPG image formats</li>
 * </ul>
 * 
 * <p>Requirements: 5.7
 */
public class ImageHelper {
    private static final Logger logger = LoggerFactory.getLogger(ImageHelper.class);
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private ImageHelper() {
        // Utility class - no instantiation
    }
    
    /**
     * Converts a byte array to a JavaFX Image object.
     * 
     * @param bytes the byte array containing image data
     * @return the JavaFX Image object, or null if conversion fails
     */
    public static Image byteArrayToImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            logger.warn("Cannot convert null or empty byte array to image");
            return null;
        }
        
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return new Image(inputStream);
        } catch (Exception e) {
            logger.error("Failed to convert byte array to image", e);
            return null;
        }
    }
    
    /**
     * Converts a JavaFX Image object to a byte array in PNG format.
     * 
     * @param image the JavaFX Image to convert
     * @return the byte array containing PNG image data, or null if conversion fails
     */
    public static byte[] imageToByteArray(Image image) {
        if (image == null) {
            logger.warn("Cannot convert null image to byte array");
            return null;
        }
        
        try {
            // Convert JavaFX Image to BufferedImage
            BufferedImage bufferedImage = convertToBufferedImage(image);
            
            // Write BufferedImage to byte array as PNG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to convert image to byte array", e);
            return null;
        }
    }
    
    /**
     * Loads an image from a file and converts it to a byte array.
     * Supports PNG and JPG formats.
     * 
     * @param file the image file to load
     * @return the byte array containing image data, or null if loading fails
     */
    public static byte[] fileToByteArray(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            logger.warn("Invalid file: {}", file);
            return null;
        }
        
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            logger.info("Successfully loaded image from file: {}", file.getName());
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to load image from file: {}", file.getName(), e);
            return null;
        }
    }
    
    /**
     * Converts a JavaFX Image to a BufferedImage.
     * This is a helper method for image format conversion.
     * 
     * @param image the JavaFX Image to convert
     * @return the BufferedImage
     */
    private static BufferedImage convertToBufferedImage(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }
        
        return bufferedImage;
    }
}
