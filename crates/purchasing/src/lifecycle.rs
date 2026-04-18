use common::entities::DealStatus;

use crate::error::PurchasingError;

/// Allowed status transitions:
///   Draft   → Active, Cancelled
///   Active  → Completed, Cancelled
///   Completed → (none)
///   Cancelled → (none)
pub fn can_transition(from: &DealStatus, to: &DealStatus) -> bool {
    matches!(
        (from, to),
        (DealStatus::Draft, DealStatus::Active)
            | (DealStatus::Draft, DealStatus::Cancelled)
            | (DealStatus::Active, DealStatus::Completed)
            | (DealStatus::Active, DealStatus::Cancelled)
    )
}

/// Attempt a status transition. Returns the new status or an error if disallowed.
pub fn transition_status(
    current: &DealStatus,
    target: &DealStatus,
) -> Result<DealStatus, PurchasingError> {
    if can_transition(current, target) {
        Ok(target.clone())
    } else {
        Err(PurchasingError::InvalidStatusTransition {
            from: current.clone(),
            to: target.clone(),
        })
    }
}
