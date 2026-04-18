use crate::RecordStockMovementDto;
use crate::RecordStockMovementResultDto;
use crate::dtos::MovementTypeInput;
use crate::error::StockTrackingError;
use crate::quantity::{apply_delta, compute_delta};
use crate::validation::validate_movement;
use anyhow::Result;
use common::database::CommandUnitOfWork;
use common::entities::{Location, MovementType, Product, Root, StockMovement, User};
use common::types::EntityId;
use inventory_security::SecurityContext;
use inventory_security_macros::check_permission;

pub trait RecordStockMovementUnitOfWorkFactoryTrait: Send + Sync {
    fn create(&self) -> Box<dyn RecordStockMovementUnitOfWorkTrait>;
}

// Exactly the same macros must be set in ../units_of_work/record_stock_movement_uow.rs
#[macros::uow_action(entity = "Root", action = "Get")]
#[macros::uow_action(entity = "Product", action = "Get")]
#[macros::uow_action(entity = "Product", action = "Update")]
#[macros::uow_action(entity = "Product", action = "Snapshot")]
#[macros::uow_action(entity = "Product", action = "Restore")]
#[macros::uow_action(entity = "Location", action = "Get")]
#[macros::uow_action(entity = "StockMovement", action = "Create")]
#[macros::uow_action(entity = "User", action = "Get")]
pub trait RecordStockMovementUnitOfWorkTrait: CommandUnitOfWork {}

fn to_movement_type(input: &MovementTypeInput) -> MovementType {
    match input {
        MovementTypeInput::Inbound => MovementType::Inbound,
        MovementTypeInput::Outbound => MovementType::Outbound,
        MovementTypeInput::Transfer => MovementType::Transfer,
        MovementTypeInput::Adjustment => MovementType::Adjustment,
        MovementTypeInput::Return => MovementType::Return,
    }
}

pub struct RecordStockMovementUseCase {
    uow_factory: Box<dyn RecordStockMovementUnitOfWorkFactoryTrait>,
}

impl RecordStockMovementUseCase {
    pub fn new(uow_factory: Box<dyn RecordStockMovementUnitOfWorkFactoryTrait>) -> Self {
        RecordStockMovementUseCase { uow_factory }
    }

    pub fn execute(
        &mut self,
        dto: &RecordStockMovementDto,
        security_context: &SecurityContext,
    ) -> Result<RecordStockMovementResultDto> {
        // RBAC check: requires stock:* permission
        check_permission(security_context, "stock:*")?;

        let movement_type = to_movement_type(&dto.movement_type);

        let mut uow = self.uow_factory.create();
        uow.begin_transaction()?;

        // 1. Load Product by product_id
        let product = uow
            .get_product(&(dto.product_id as EntityId))?
            .ok_or(StockTrackingError::ProductNotFound { id: dto.product_id })?;

        // 2. Validate locations exist if provided
        if dto.from_location_id > 0 {
            uow.get_location(&(dto.from_location_id as EntityId))?
                .ok_or(StockTrackingError::LocationNotFound {
                    id: dto.from_location_id,
                })?;
        }
        if dto.to_location_id > 0 {
            uow.get_location(&(dto.to_location_id as EntityId))?
                .ok_or(StockTrackingError::LocationNotFound {
                    id: dto.to_location_id,
                })?;
        }

        // 3. Validate movement-type-specific constraints
        validate_movement(
            &movement_type,
            dto.quantity,
            product.quantity,
            dto.from_location_id,
            dto.to_location_id,
        )?;

        // 4. Compute delta and apply
        let delta = compute_delta(&movement_type, dto.quantity);
        let new_quantity = apply_delta(product.quantity, delta);

        // 5. Update Product entity
        let mut updated_product = product.clone();
        updated_product.quantity = new_quantity;
        uow.update_product(&updated_product)?;

        // 6. Create StockMovement entity
        let root = uow
            .get_root(&1)?
            .ok_or_else(|| StockTrackingError::Internal("Root entity not found".into()))?;

        let from_loc = if dto.from_location_id > 0 { Some(dto.from_location_id as EntityId) } else { None };
        let to_loc = if dto.to_location_id > 0 { Some(dto.to_location_id as EntityId) } else { None };

        let stock_movement = StockMovement {
            id: 0,
            movement_type,
            quantity: dto.quantity,
            note: dto.note.clone(),
            product: Some(dto.product_id as EntityId),
            from_location: from_loc,
            to_location: to_loc,
            performed_by: Some(security_context.user_id),
            ..Default::default()
        };
        let created = uow.create_stock_movement(&stock_movement, root.id, -1)?;

        uow.commit()?;

        Ok(RecordStockMovementResultDto {
            movement_id: created.id as i64,
            new_product_quantity: new_quantity,
        })
    }
}
