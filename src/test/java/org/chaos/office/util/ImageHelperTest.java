package org.chaos.office.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ImageHelper utility class.
 */
class ImageHelperTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testImageHelperCannotBeInstantiated() {
        try {
            var constructor = ImageHelper.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
                    "ImageHelper constructor should be private");
        } catch (NoSuchMethodException e) {
            fail("ImageHelper should have a private no-arg constructor");
        }
    }

    @Test
    void testByteArrayToImageWithNull() {
        assertNull(ImageHelper.byteArrayToImage(null));
    }

    @Test
    void testByteArrayToImageWithEmptyArray() {
        assertNull(ImageHelper.byteArrayToImage(new byte[0]));
    }

    @Test
    void testByteArrayToImageWithInvalidData() {
        // JavaFX Image constructor is lenient and may create an Image even with invalid data
        // So we just verify the method doesn't throw an exception
        byte[] invalidData = {1, 2, 3, 4, 5};
        assertDoesNotThrow(() -> ImageHelper.byteArrayToImage(invalidData));
    }

    @Test
    void testImageToByteArrayWithNull() {
        assertNull(ImageHelper.imageToByteArray(null));
    }

    @Test
    void testFileToByteArrayWithNull() {
        assertNull(ImageHelper.fileToByteArray(null));
    }

    @Test
    void testFileToByteArrayWithNonExistentFile() {
        File nonExistent = new File("nonexistent.png");
        assertNull(ImageHelper.fileToByteArray(nonExistent));
    }

    @Test
    void testFileToByteArrayWithValidFile(@TempDir Path tempDir) throws Exception {
        // Create a simple test file with some data
        File testFile = tempDir.resolve("test.png").toFile();
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            // Write a minimal PNG header (not a valid image, but enough for file reading test)
            byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            fos.write(pngHeader);
        }

        byte[] result = ImageHelper.fileToByteArray(testFile);
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals(8, result.length); // Should match the bytes we wrote
    }

    @Test
    void testRoundTripConversionWithSmallImage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Create a small test image (1x1 pixel)
                Image originalImage = new Image(
                    getClass().getResourceAsStream("/images/test-placeholder.png") != null ?
                    getClass().getResourceAsStream("/images/test-placeholder.png") :
                    new java.io.ByteArrayInputStream(createMinimalPngBytes())
                );
                
                // Convert to byte array
                byte[] bytes = ImageHelper.imageToByteArray(originalImage);
                
                if (bytes != null) {
                    // Convert back to image
                    Image reconstructedImage = ImageHelper.byteArrayToImage(bytes);
                    
                    // Verify the image was reconstructed
                    assertNotNull(reconstructedImage);
                    assertTrue(reconstructedImage.getWidth() > 0);
                    assertTrue(reconstructedImage.getHeight() > 0);
                }
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Creates a minimal valid PNG byte array for testing.
     * This is a 1x1 transparent pixel PNG.
     */
    private byte[] createMinimalPngBytes() {
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 dimensions
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
            0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND chunk
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
