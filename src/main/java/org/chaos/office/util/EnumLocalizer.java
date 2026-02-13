package org.chaos.office.util;

import org.chaos.office.reports.models.PaymentMethod;
import org.chaos.office.reports.models.StockStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for localizing enum values.
 * Provides methods to get localized display text for enums used in reports and PDFs.
 */
public class EnumLocalizer {
    private static final Logger logger = LoggerFactory.getLogger(EnumLocalizer.class);
    
    private EnumLocalizer() {
        // Utility class - no instantiation
    }
    
    /**
     * Gets the localized display text for a StockStatus enum value.
     * 
     * @param status the stock status enum value
     * @return localized display text
     */
    public static String getLocalizedStockStatus(StockStatus status) {
        if (status == null) {
            return "";
        }
        
        String key = switch (status) {
            case NORMAL -> "stock.status.normal";
            case LOW_STOCK -> "stock.status.low";
            case OUT_OF_STOCK -> "stock.status.out";
        };
        
        return LocaleManager.getString(key);
    }
    
    /**
     * Gets the localized display text for a PaymentMethod enum value.
     * 
     * @param method the payment method enum value
     * @return localized display text
     */
    public static String getLocalizedPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "";
        }
        
        String key = switch (method) {
            case CASH -> "payment.method.cash";
            case CARD -> "payment.method.card";
            case CHECK -> "payment.method.check";
        };
        
        return LocaleManager.getString(key);
    }
}
