use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum OrderStatus {
    #[serde(rename = "placed")]
    Placed,
    #[serde(rename = "approved")]
    Approved,
    #[serde(rename = "delivered")]
    Delivered,
}
impl Enum for OrderStatus {
    fn label(&self) -> &str {
        match self {
            OrderStatus::Placed => "placed",
            OrderStatus::Approved => "approved",
            OrderStatus::Delivered => "delivered",
        }
    }
    fn from_label(s: &str) -> Option<Self> {
        match s {
            "placed" => Some(OrderStatus::Placed),
            "approved" => Some(OrderStatus::Approved),
            "delivered" => Some(OrderStatus::Delivered),
            _ => None,
        }
    }
}
impl std::fmt::Display for OrderStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{}", self.label())
    }
}
