use sqlx::SqlitePool;

use crate::error::AppResult;
use crate::models::domain::{
    FinishedGood, FinishedGoodReport, InventoryReport, Pagination, RawMaterial, RawMaterialReport,
};
use crate::models::Money;
use crate::persistence::queries::finished_goods as fg_queries;
use crate::persistence::queries::raw_materials as rm_queries;

/// Build an inventory report with low-stock flags (Req 12.2).
///
/// An item is flagged as low-stock when its `current_quantity < low_stock_threshold`.
pub async fn get_inventory_report(
    pool: &SqlitePool,
    low_stock_threshold: f64,
) -> AppResult<InventoryReport> {
    let rm_rows = rm_queries::list_all(pool, &Pagination::default()).await?;
    let raw_materials: Vec<RawMaterialReport> = rm_rows
        .into_iter()
        .map(|r| -> AppResult<RawMaterialReport> {
            let last_updated = crate::utils::parse_timestamp(&r.last_updated)?;
            let material = RawMaterial {
                id: crate::utils::parse_uuid("raw_material", &r.id)?,
                name: r.name,
                unit: r.unit,
                current_quantity: r.current_quantity,
                last_updated,
            };
            let is_low_stock = material.current_quantity < low_stock_threshold;
            Ok(RawMaterialReport {
                material,
                is_low_stock,
            })
        })
        .collect::<AppResult<Vec<_>>>()?;

    let fg_rows = fg_queries::list_all(pool, &Pagination::default()).await?;
    let finished_goods: Vec<FinishedGoodReport> = fg_rows
        .into_iter()
        .map(|r| -> AppResult<FinishedGoodReport> {
            let last_updated = crate::utils::parse_timestamp(&r.last_updated)?;
            let good = FinishedGood {
                id: crate::utils::parse_uuid("finished_good", &r.id)?,
                name: r.name,
                current_quantity: r.current_quantity,
                unit_price: Money(r.unit_price),
                last_updated,
            };
            let is_low_stock = good.current_quantity < low_stock_threshold;
            Ok(FinishedGoodReport {
                good,
                is_low_stock,
            })
        })
        .collect::<AppResult<Vec<_>>>()?;

    Ok(InventoryReport {
        raw_materials,
        finished_goods,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;
    use uuid::Uuid;
    use crate::persistence::db;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::raw_materials as rm_queries;

    async fn setup() -> SqlitePool {
        db::init_db(":memory:").await.expect("DB init failed")
    }

    #[tokio::test]
    async fn inventory_report_flags_low_stock_correctly() {
        let pool = setup().await;
        let now = Utc::now().to_rfc3339();

        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let fg1 = Uuid::new_v4();

        rm_queries::insert(&pool, &rm1.to_string(), "Milk", "L", 5.0, &now).await.unwrap();
        rm_queries::insert(&pool, &rm2.to_string(), "Sugar", "kg", 50.0, &now).await.unwrap();
        fg_queries::insert(&pool, &fg1.to_string(), "Sweet Box", 3.0, 2500, &now).await.unwrap();

        let report = get_inventory_report(&pool, 10.0).await.unwrap();

        assert_eq!(report.raw_materials.len(), 2);
        assert_eq!(report.finished_goods.len(), 1);

        // Milk (5) < 10 → low stock
        let milk = report.raw_materials.iter().find(|r| r.material.name == "Milk").unwrap();
        assert!(milk.is_low_stock);

        // Sugar (50) >= 10 → not low stock
        let sugar = report.raw_materials.iter().find(|r| r.material.name == "Sugar").unwrap();
        assert!(!sugar.is_low_stock);

        // Sweet Box (3) < 10 → low stock
        assert!(report.finished_goods[0].is_low_stock);
    }

    #[tokio::test]
    async fn inventory_report_empty_when_no_items() {
        let pool = setup().await;
        let report = get_inventory_report(&pool, 10.0).await.unwrap();
        assert!(report.raw_materials.is_empty());
        assert!(report.finished_goods.is_empty());
    }

    #[tokio::test]
    async fn inventory_report_threshold_zero_flags_nothing() {
        let pool = setup().await;
        let now = Utc::now().to_rfc3339();

        rm_queries::insert(&pool, &Uuid::new_v4().to_string(), "Milk", "L", 0.5, &now).await.unwrap();

        let report = get_inventory_report(&pool, 0.0).await.unwrap();
        // 0.5 is not < 0.0
        assert!(!report.raw_materials[0].is_low_stock);
    }

    #[tokio::test]
    async fn inventory_report_exact_threshold_not_flagged() {
        let pool = setup().await;
        let now = Utc::now().to_rfc3339();

        rm_queries::insert(&pool, &Uuid::new_v4().to_string(), "Cream", "L", 10.0, &now).await.unwrap();

        // quantity == threshold → not low stock (strictly less than)
        let report = get_inventory_report(&pool, 10.0).await.unwrap();
        assert!(!report.raw_materials[0].is_low_stock);
    }
}
