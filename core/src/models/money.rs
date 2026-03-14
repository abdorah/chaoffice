use serde::{Deserialize, Serialize};
use std::fmt;
use std::iter::Sum;
use std::ops::{Add, Sub, Mul};

use crate::error::{AppError, AppResult};

/// Integer-cents monetary type. 1 Money = 0.01 display currency.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct Money(pub i64);

impl Money {
    pub const ZERO: Money = Money(0);

    /// Create from a whole-unit amount (e.g., 25.50 → Money(2550)).
    /// Used only for migration and test helpers.
    pub fn from_f64(val: f64) -> Money {
        Money((val * 100.0).round() as i64)
    }

    /// Convert to f64 for display or proto serialization (e.g., Money(2550) → 25.50).
    pub fn to_f64(self) -> f64 {
        self.0 as f64 / 100.0
    }

    /// Format as "25.50" for display.
    pub fn to_display(&self) -> String {
        let whole = self.0 / 100;
        let frac = (self.0 % 100).abs();
        if self.0 < 0 && whole == 0 {
            format!("-0.{frac:02}")
        } else {
            format!("{whole}.{frac:02}")
        }
    }

    /// Parse "25.50" → Money(2550). Returns Err on malformed input.
    pub fn parse(s: &str) -> AppResult<Money> {
        let s = s.trim();
        if s.is_empty() {
            return Err(AppError::Validation {
                field: "money".into(),
                message: "Invalid amount: empty string".into(),
            });
        }
        let parts: Vec<&str> = s.split('.').collect();
        match parts.len() {
            1 => {
                let whole: i64 = parts[0].parse().map_err(|_| AppError::Validation {
                    field: "money".into(),
                    message: format!("Invalid amount: {s}"),
                })?;
                Ok(Money(whole * 100))
            }
            2 => {
                let whole: i64 = parts[0].parse().map_err(|_| AppError::Validation {
                    field: "money".into(),
                    message: format!("Invalid amount: {s}"),
                })?;
                let frac_str = parts[1];
                if frac_str.len() > 2 {
                    return Err(AppError::Validation {
                        field: "money".into(),
                        message: "Max 2 decimal places".into(),
                    });
                }
                let padded = format!("{frac_str:0<2}");
                let frac: i64 = padded.parse().map_err(|_| AppError::Validation {
                    field: "money".into(),
                    message: format!("Invalid amount: {s}"),
                })?;
                let sign = if whole < 0 || s.starts_with('-') { -1 } else { 1 };
                Ok(Money(sign * (whole.abs() * 100 + frac)))
            }
            _ => Err(AppError::Validation {
                field: "money".into(),
                message: format!("Invalid amount: {s}"),
            }),
        }
    }
}

impl Add for Money {
    type Output = Money;
    fn add(self, rhs: Money) -> Money {
        Money(self.0 + rhs.0)
    }
}

impl Sub for Money {
    type Output = Money;
    fn sub(self, rhs: Money) -> Money {
        Money(self.0 - rhs.0)
    }
}

impl Mul<i64> for Money {
    type Output = Money;
    fn mul(self, rhs: i64) -> Money {
        Money(self.0 * rhs)
    }
}

impl Sum for Money {
    fn sum<I: Iterator<Item = Money>>(iter: I) -> Money {
        iter.fold(Money::ZERO, |acc, m| acc + m)
    }
}

impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.to_display())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_zero() {
        assert_eq!(Money::ZERO, Money(0));
    }

    #[test]
    fn test_from_f64() {
        assert_eq!(Money::from_f64(25.50), Money(2550));
        assert_eq!(Money::from_f64(0.0), Money(0));
        assert_eq!(Money::from_f64(-10.99), Money(-1099));
        assert_eq!(Money::from_f64(0.01), Money(1));
    }

    #[test]
    fn test_to_display() {
        assert_eq!(Money(2550).to_display(), "25.50");
        assert_eq!(Money(0).to_display(), "0.00");
        assert_eq!(Money(1).to_display(), "0.01");
        assert_eq!(Money(-1099).to_display(), "-10.99");
        assert_eq!(Money(-5).to_display(), "-0.05");
        assert_eq!(Money(100).to_display(), "1.00");
    }

    #[test]
    fn test_parse_whole_number() {
        assert_eq!(Money::parse("25").unwrap(), Money(2500));
        assert_eq!(Money::parse("-10").unwrap(), Money(-1000));
        assert_eq!(Money::parse("0").unwrap(), Money(0));
    }

    #[test]
    fn test_parse_decimal() {
        assert_eq!(Money::parse("25.50").unwrap(), Money(2550));
        assert_eq!(Money::parse("0.01").unwrap(), Money(1));
        assert_eq!(Money::parse("-10.99").unwrap(), Money(-1099));
        assert_eq!(Money::parse("1.5").unwrap(), Money(150));
        assert_eq!(Money::parse("-0.05").unwrap(), Money(-5));
    }

    #[test]
    fn test_parse_trims_whitespace() {
        assert_eq!(Money::parse("  25.50  ").unwrap(), Money(2550));
    }

    #[test]
    fn test_parse_rejects_malformed() {
        assert!(Money::parse("").is_err());
        assert!(Money::parse("abc").is_err());
        assert!(Money::parse("1.234").is_err());
        assert!(Money::parse("1.2.3").is_err());
    }

    #[test]
    fn test_arithmetic() {
        assert_eq!(Money(100) + Money(200), Money(300));
        assert_eq!(Money(500) - Money(200), Money(300));
        assert_eq!(Money(250) * 3, Money(750));
    }

    #[test]
    fn test_sum() {
        let values = vec![Money(100), Money(200), Money(300)];
        let total: Money = values.into_iter().sum();
        assert_eq!(total, Money(600));
    }

    #[test]
    fn test_display() {
        assert_eq!(format!("{}", Money(2550)), "25.50");
        assert_eq!(format!("{}", Money(-5)), "-0.05");
    }

    #[test]
    fn test_serialize_deserialize() {
        let m = Money(2550);
        let json = serde_json::to_string(&m).unwrap();
        let deserialized: Money = serde_json::from_str(&json).unwrap();
        assert_eq!(m, deserialized);
    }
}
