package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * Unit tests for the Bill model with POS properties.
 */
class BillTest {

    private Bill bill;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        bill = new Bill();
        testDate = LocalDate.of(2024, 1, 15);
    }

    @Test
    void testDefaultConstructor() {
        Bill newBill = new Bill();
        assertNotNull(newBill);
        assertEquals(0, newBill.getId());
        assertNull(newBill.getClientName());
        assertNull(newBill.getClientPhone());
        assertEquals(0.0f, newBill.getTotalPrice());
        assertNull(newBill.getDate());
        
        // Test POS default values
        assertEquals(0.0f, newBill.getSubtotal());
        assertEquals("none", newBill.getDiscountType());
        assertEquals(0.0f, newBill.getDiscountValue());
        assertEquals("cash", newBill.getPaymentMethod());
    }

    @Test
    void testParameterizedConstructor() {
        Bill newBill = new Bill(1, "John Doe", "555-1234", 100.0f, testDate);
        
        assertEquals(1, newBill.getId());
        assertEquals("John Doe", newBill.getClientName());
        assertEquals("555-1234", newBill.getClientPhone());
        assertEquals(100.0f, newBill.getTotalPrice(), 0.001);
        assertEquals(testDate, newBill.getDate());
        
        // Test POS default values
        assertEquals(0.0f, newBill.getSubtotal());
        assertEquals("none", newBill.getDiscountType());
        assertEquals(0.0f, newBill.getDiscountValue());
        assertEquals("cash", newBill.getPaymentMethod());
    }

    @Test
    void testSettersAndGetters() {
        bill.setId(42);
        bill.setClientName("Jane Smith");
        bill.setClientPhone("555-5678");
        bill.setDate(testDate);
        bill.setSubtotal(300.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        bill.setPaymentMethod("card");
        
        assertEquals(42, bill.getId());
        assertEquals("Jane Smith", bill.getClientName());
        assertEquals("555-5678", bill.getClientPhone());
        assertEquals(testDate, bill.getDate());
        assertEquals(300.0f, bill.getSubtotal(), 0.001);
        assertEquals("percentage", bill.getDiscountType());
        assertEquals(10.0f, bill.getDiscountValue(), 0.001);
        assertEquals("card", bill.getPaymentMethod());
        // Total price should be automatically calculated: 300 - (300 * 0.10) = 270
        assertEquals(270.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testPropertyAccessors() {
        Bill newBill = new Bill(1, "John Doe", "555-1234", 100.0f, testDate);
        
        assertNotNull(newBill.idProperty());
        assertNotNull(newBill.clientNameProperty());
        assertNotNull(newBill.clientPhoneProperty());
        assertNotNull(newBill.totalPriceProperty());
        assertNotNull(newBill.dateProperty());
        assertNotNull(newBill.subtotalProperty());
        assertNotNull(newBill.discountTypeProperty());
        assertNotNull(newBill.discountValueProperty());
        assertNotNull(newBill.paymentMethodProperty());
    }

    @Test
    void testCalculateFinalTotalWithNoDiscount() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("none");
        bill.setDiscountValue(0.0f);
        
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWithPercentageDiscount() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWithFixedDiscount() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("fixed");
        bill.setDiscountValue(25.0f);
        
        assertEquals(75.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWith50PercentDiscount() {
        bill.setSubtotal(200.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(50.0f);
        
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWith100PercentDiscount() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(100.0f);
        
        assertEquals(0.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWithZeroSubtotal() {
        bill.setSubtotal(0.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        
        assertEquals(0.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testCalculateFinalTotalWithDiscountEqualsSubtotal() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("fixed");
        bill.setDiscountValue(100.0f);
        
        assertEquals(0.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testApplyDiscountWithValidPercentage() {
        bill.setSubtotal(100.0f);
        assertDoesNotThrow(() -> bill.applyDiscount("percentage", 25.0f));
        
        assertEquals("percentage", bill.getDiscountType());
        assertEquals(25.0f, bill.getDiscountValue(), 0.001);
        assertEquals(75.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testApplyDiscountWithValidFixedAmount() {
        bill.setSubtotal(100.0f);
        assertDoesNotThrow(() -> bill.applyDiscount("fixed", 30.0f));
        
        assertEquals("fixed", bill.getDiscountType());
        assertEquals(30.0f, bill.getDiscountValue(), 0.001);
        assertEquals(70.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testApplyDiscountWithNone() {
        bill.setSubtotal(100.0f);
        assertDoesNotThrow(() -> bill.applyDiscount("none", 0.0f));
        
        assertEquals("none", bill.getDiscountType());
        assertEquals(0.0f, bill.getDiscountValue(), 0.001);
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testApplyDiscountWithInvalidType() {
        bill.setSubtotal(100.0f);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.applyDiscount("invalid", 10.0f);
        });
        
        assertTrue(exception.getMessage().contains("Invalid discount type"));
    }

    @Test
    void testApplyDiscountWithNegativePercentage() {
        bill.setSubtotal(100.0f);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.applyDiscount("percentage", -10.0f);
        });
        
        assertTrue(exception.getMessage().contains("Percentage discount must be between 0 and 100"));
    }

    @Test
    void testApplyDiscountWithPercentageOver100() {
        bill.setSubtotal(100.0f);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.applyDiscount("percentage", 150.0f);
        });
        
        assertTrue(exception.getMessage().contains("Percentage discount must be between 0 and 100"));
    }

    @Test
    void testApplyDiscountWithNegativeFixedAmount() {
        bill.setSubtotal(100.0f);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.applyDiscount("fixed", -25.0f);
        });
        
        assertTrue(exception.getMessage().contains("Fixed discount cannot be negative"));
    }

    @Test
    void testApplyDiscountWithFixedAmountExceedingSubtotal() {
        bill.setSubtotal(100.0f);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.applyDiscount("fixed", 150.0f);
        });
        
        assertTrue(exception.getMessage().contains("cannot exceed subtotal"));
    }

    @Test
    void testSetPaymentMethodWithValidValues() {
        assertDoesNotThrow(() -> bill.setPaymentMethod("cash"));
        assertEquals("cash", bill.getPaymentMethod());
        
        assertDoesNotThrow(() -> bill.setPaymentMethod("card"));
        assertEquals("card", bill.getPaymentMethod());
        
        assertDoesNotThrow(() -> bill.setPaymentMethod("check"));
        assertEquals("check", bill.getPaymentMethod());
    }

    @Test
    void testSetPaymentMethodWithInvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bill.setPaymentMethod("bitcoin");
        });
        
        assertTrue(exception.getMessage().contains("Invalid payment method"));
    }

    @Test
    void testPaymentMethodChanges() {
        bill.setPaymentMethod("cash");
        assertEquals("cash", bill.getPaymentMethod());
        
        bill.setPaymentMethod("card");
        assertEquals("card", bill.getPaymentMethod());
        
        bill.setPaymentMethod("check");
        assertEquals("check", bill.getPaymentMethod());
    }

    @Test
    void testDiscountTypeTransitions() {
        bill.setSubtotal(100.0f);
        
        // none -> percentage
        bill.applyDiscount("none", 0.0f);
        assertEquals("none", bill.getDiscountType());
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
        
        bill.applyDiscount("percentage", 10.0f);
        assertEquals("percentage", bill.getDiscountType());
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
        
        // percentage -> fixed
        bill.applyDiscount("fixed", 20.0f);
        assertEquals("fixed", bill.getDiscountType());
        assertEquals(80.0f, bill.getTotalPrice(), 0.001);
        
        // fixed -> none
        bill.applyDiscount("none", 0.0f);
        assertEquals("none", bill.getDiscountType());
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testAutomaticRecalculationOnSubtotalChange() {
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        
        bill.setSubtotal(100.0f);
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
        
        bill.setSubtotal(200.0f);
        assertEquals(180.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testAutomaticRecalculationOnDiscountTypeChange() {
        bill.setSubtotal(100.0f);
        bill.setDiscountValue(10.0f);
        
        bill.setDiscountType("none");
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
        
        bill.setDiscountType("percentage");
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
        
        bill.setDiscountType("fixed");
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testAutomaticRecalculationOnDiscountValueChange() {
        bill.setSubtotal(100.0f);
        bill.setDiscountType("percentage");
        
        bill.setDiscountValue(10.0f);
        assertEquals(90.0f, bill.getTotalPrice(), 0.001);
        
        bill.setDiscountValue(20.0f);
        assertEquals(80.0f, bill.getTotalPrice(), 0.001);
        
        bill.setDiscountValue(0.0f);
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testToString() {
        bill.setId(1);
        bill.setClientName("John Doe");
        bill.setClientPhone("555-1234");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        bill.setPaymentMethod("card");
        bill.setDate(testDate);
        
        String result = bill.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("clientName='John Doe'"));
        assertTrue(result.contains("clientPhone='555-1234'"));
        assertTrue(result.contains("subtotal=100.0"));
        assertTrue(result.contains("discountType='percentage'"));
        assertTrue(result.contains("discountValue=10.0"));
        assertTrue(result.contains("paymentMethod='card'"));
    }

    @Test
    void testFloatPropertyPrecision() {
        bill.setSubtotal(19.99f);
        assertEquals(19.99f, bill.getSubtotal(), 0.001);
        
        bill.setSubtotal(0.01f);
        assertEquals(0.01f, bill.getSubtotal(), 0.001);
        
        bill.setSubtotal(9999.99f);
        assertEquals(9999.99f, bill.getSubtotal(), 0.001);
    }

    @Test
    void testEdgeCaseZeroPercentageDiscount() {
        bill.setSubtotal(100.0f);
        bill.applyDiscount("percentage", 0.0f);
        
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testEdgeCaseZeroFixedDiscount() {
        bill.setSubtotal(100.0f);
        bill.applyDiscount("fixed", 0.0f);
        
        assertEquals(100.0f, bill.getTotalPrice(), 0.001);
    }

    @Test
    void testComplexDiscountScenario() {
        // Start with subtotal
        bill.setSubtotal(500.0f);
        assertEquals(500.0f, bill.getTotalPrice(), 0.001);
        
        // Apply 20% discount
        bill.applyDiscount("percentage", 20.0f);
        assertEquals(400.0f, bill.getTotalPrice(), 0.001);
        
        // Change to fixed discount
        bill.applyDiscount("fixed", 100.0f);
        assertEquals(400.0f, bill.getTotalPrice(), 0.001);
        
        // Remove discount
        bill.applyDiscount("none", 0.0f);
        assertEquals(500.0f, bill.getTotalPrice(), 0.001);
    }
}
