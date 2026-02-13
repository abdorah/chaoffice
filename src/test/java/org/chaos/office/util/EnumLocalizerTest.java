package org.chaos.office.util;

import org.chaos.office.reports.models.PaymentMethod;
import org.chaos.office.reports.models.StockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class EnumLocalizerTest {
    
    @BeforeEach
    void setUp() {
        // Set locale to English for consistent testing
        LocaleManager.setLocale(Locale.US);
    }
    
    @Test
    void testLocalizedStockStatusEnglish() {
        assertEquals("Normal", EnumLocalizer.getLocalizedStockStatus(StockStatus.NORMAL));
        assertEquals("Low Stock", EnumLocalizer.getLocalizedStockStatus(StockStatus.LOW_STOCK));
        assertEquals("Out of Stock", EnumLocalizer.getLocalizedStockStatus(StockStatus.OUT_OF_STOCK));
    }
    
    @Test
    void testLocalizedStockStatusArabic() {
        LocaleManager.setLocale(new Locale("ar"));
        
        assertEquals("عادي", EnumLocalizer.getLocalizedStockStatus(StockStatus.NORMAL));
        assertEquals("مخزون منخفض", EnumLocalizer.getLocalizedStockStatus(StockStatus.LOW_STOCK));
        assertEquals("نفذت من المخزون", EnumLocalizer.getLocalizedStockStatus(StockStatus.OUT_OF_STOCK));
    }
    
    @Test
    void testLocalizedStockStatusFrench() {
        LocaleManager.setLocale(Locale.FRANCE);
        
        assertEquals("Normal", EnumLocalizer.getLocalizedStockStatus(StockStatus.NORMAL));
        assertEquals("Stock faible", EnumLocalizer.getLocalizedStockStatus(StockStatus.LOW_STOCK));
        assertEquals("Rupture de stock", EnumLocalizer.getLocalizedStockStatus(StockStatus.OUT_OF_STOCK));
    }
    
    @Test
    void testLocalizedStockStatusNull() {
        assertEquals("", EnumLocalizer.getLocalizedStockStatus(null));
    }
    
    @Test
    void testLocalizedPaymentMethodEnglish() {
        assertEquals("Cash", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CASH));
        assertEquals("Card", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CARD));
        assertEquals("Check", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CHECK));
    }
    
    @Test
    void testLocalizedPaymentMethodArabic() {
        LocaleManager.setLocale(new Locale("ar"));
        
        assertEquals("نقداً", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CASH));
        assertEquals("بطاقة", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CARD));
        assertEquals("شيك", EnumLocalizer.getLocalizedPaymentMethod(PaymentMethod.CHECK));
    }
    
    @Test
    void testLocalizedPaymentMethodNull() {
        assertEquals("", EnumLocalizer.getLocalizedPaymentMethod(null));
    }
}
