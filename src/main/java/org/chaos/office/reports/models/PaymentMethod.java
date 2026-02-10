package org.chaos.office.reports.models;

/**
 * Payment method enumeration for sales reports
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    CHECK("Check");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static PaymentMethod fromString(String value) {
        if (value == null) {
            return CASH;
        }
        
        switch (value.toLowerCase()) {
            case "card":
                return CARD;
            case "check":
                return CHECK;
            case "cash":
            default:
                return CASH;
        }
    }
}
