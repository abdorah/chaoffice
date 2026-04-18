use common::entities::MovementType;

use crate::error::StockTrackingError;

/// Validate a stock movement request based on movement type.
/// Checks location requirements, quantity constraints, and sufficient stock.
///
/// `from_location_id` and `to_location_id` use 0 to indicate "not provided".
pub fn validate_movement(
    movement_type: &MovementType,
    quantity: i64,
    product_quantity: i64,
    from_location_id: i64,
    to_location_id: i64,
) -> Result<(), StockTrackingError> {
    // 1. Reject zero quantity for all types
    if quantity == 0 {
        return Err(StockTrackingError::ZeroQuantity);
    }

    match movement_type {
        MovementType::Inbound => {
            if quantity < 0 {
                return Err(StockTrackingError::NegativeQuantity);
            }
            if to_location_id <= 0 {
                return Err(StockTrackingError::MissingToLocation);
            }
        }
        MovementType::Outbound => {
            if quantity < 0 {
                return Err(StockTrackingError::NegativeQuantity);
            }
            if from_location_id <= 0 {
                return Err(StockTrackingError::MissingFromLocation);
            }
            if product_quantity < quantity {
                return Err(StockTrackingError::InsufficientStock {
                    available: product_quantity,
                    requested: quantity,
                });
            }
        }
        MovementType::Transfer => {
            if quantity < 0 {
                return Err(StockTrackingError::NegativeQuantity);
            }
            if from_location_id <= 0 || to_location_id <= 0 {
                return Err(StockTrackingError::MissingTransferLocations);
            }
            if from_location_id == to_location_id {
                return Err(StockTrackingError::SameLocation);
            }
            if product_quantity < quantity {
                return Err(StockTrackingError::InsufficientStock {
                    available: product_quantity,
                    requested: quantity,
                });
            }
        }
        MovementType::Adjustment => {
            // Adjustment can be positive or negative, but result must be >= 0
            let result = product_quantity + quantity;
            if result < 0 {
                return Err(StockTrackingError::AdjustmentUnderflow {
                    current: product_quantity,
                    adjustment: quantity,
                });
            }
        }
        MovementType::Return => {
            if quantity < 0 {
                return Err(StockTrackingError::NegativeQuantity);
            }
            if to_location_id <= 0 {
                return Err(StockTrackingError::MissingToLocation);
            }
        }
    }

    Ok(())
}
