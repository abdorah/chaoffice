use common::entities::MovementType;

use crate::quantity::compute_delta;

/// Compute running totals from a chronologically ordered list of movements.
/// Starting from `initial_quantity`, applies each movement's signed delta.
/// Returns a Vec<i64> of the same length as `movements`.
pub fn compute_running_totals(
    initial_quantity: i64,
    movements: &[(MovementType, i64)],
) -> Vec<i64> {
    let mut totals = Vec::with_capacity(movements.len());
    let mut running = initial_quantity;
    for (mt, qty) in movements {
        let delta = compute_delta(mt, *qty);
        running += delta;
        totals.push(running);
    }
    totals
}
