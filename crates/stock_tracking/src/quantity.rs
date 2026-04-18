use common::entities::MovementType;

/// Compute the signed quantity change for a movement.
/// Inbound: +quantity, Outbound: -quantity, Transfer: -quantity (source side),
/// Adjustment: quantity as-is (already signed), Return: +quantity.
pub fn compute_delta(movement_type: &MovementType, quantity: i64) -> i64 {
    match movement_type {
        MovementType::Inbound => quantity,
        MovementType::Outbound => -quantity,
        MovementType::Transfer => -quantity,
        MovementType::Adjustment => quantity,
        MovementType::Return => quantity,
    }
}

/// Apply a movement delta to a product quantity.
/// Returns the new quantity. Caller must validate before calling.
pub fn apply_delta(current_quantity: i64, delta: i64) -> i64 {
    current_quantity + delta
}
