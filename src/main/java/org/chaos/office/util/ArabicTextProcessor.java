package org.chaos.office.util;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArabicTextProcessor handles proper Arabic text shaping and bidirectional text processing for PDF generation.
 * 
 * <p>This utility uses ICU4J library to:
 * <ul>
 *   <li>Shape Arabic characters correctly (connecting letters)</li>
 *   <li>Handle bidirectional text (Arabic RTL mixed with English LTR)</li>
 *   <li>Ensure proper visual ordering for PDF rendering</li>
 * </ul>
 * 
 * <p>Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 */
public class ArabicTextProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ArabicTextProcessor.class);
    
    private ArabicTextProcessor() {
        // Utility class - no instantiation
    }
    
    /**
     * Processes text for PDF rendering, handling Arabic shaping and bidirectional text.
     * 
     * @param text the input text (may contain Arabic, English, numbers, etc.)
     * @param isRTL whether the text should be processed as RTL
     * @return processed text ready for PDF rendering
     */
    public static String processForPDF(String text, boolean isRTL) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // Step 1: Shape Arabic characters (connect letters properly)
            String shapedText = shapeArabicText(text);
            
            // Step 2: Handle bidirectional text if RTL
            if (isRTL) {
                shapedText = reorderBidiText(shapedText);
            }
            
            return shapedText;
            
        } catch (Exception e) {
            logger.error("Error processing Arabic text: {}", text, e);
            // Return original text if processing fails
            return text;
        }
    }
    
    /**
     * Shapes Arabic text without reordering (for use in RTL tables/containers).
     * Use this when the PDF library handles text direction (e.g., PdfPTable with setRunDirection).
     * 
     * @param text the input text
     * @return shaped text with properly connected Arabic letters
     */
    public static String shapeOnly(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            return shapeArabicText(text);
        } catch (Exception e) {
            logger.error("Error shaping Arabic text: {}", text, e);
            return text;
        }
    }
    
    /**
     * Shapes Arabic text by connecting letters properly.
     * Converts isolated Arabic letters to their contextual forms (initial, medial, final).
     * 
     * @param text the input text
     * @return shaped text with properly connected Arabic letters
     */
    private static String shapeArabicText(String text) {
        try {
            ArabicShaping shaper = new ArabicShaping(
                ArabicShaping.LETTERS_SHAPE | 
                ArabicShaping.LENGTH_GROW_SHRINK
            );
            return shaper.shape(text);
        } catch (ArabicShapingException e) {
            logger.warn("Failed to shape Arabic text: {}", text, e);
            return text;
        }
    }
    
    /**
     * Reorders bidirectional text for visual display.
     * Handles mixed RTL (Arabic) and LTR (English, numbers) text.
     * 
     * @param text the shaped text
     * @return visually ordered text for PDF rendering
     */
    private static String reorderBidiText(String text) {
        try {
            // Create Bidi object with RTL base direction
            Bidi bidi = new Bidi(text, Bidi.DIRECTION_RIGHT_TO_LEFT);
            
            // If text is purely LTR, no reordering needed
            if (!bidi.isMixed() && Bidi.getBaseDirection(text) == Bidi.LTR) {
                return text;
            }
            
            // Get visually ordered text
            return bidi.writeReordered(Bidi.DO_MIRRORING);
            
        } catch (Exception e) {
            logger.warn("Failed to reorder bidirectional text: {}", text, e);
            return text;
        }
    }
    
    /**
     * Checks if the given text contains Arabic characters.
     * 
     * @param text the text to check
     * @return true if text contains Arabic characters
     */
    public static boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            // Arabic Unicode range: U+0600 to U+06FF
            // Arabic Supplement: U+0750 to U+077F
            // Arabic Extended-A: U+08A0 to U+08FF
            if ((c >= 0x0600 && c <= 0x06FF) ||
                (c >= 0x0750 && c <= 0x077F) ||
                (c >= 0x08A0 && c <= 0x08FF)) {
                return true;
            }
        }
        
        return false;
    }
}
