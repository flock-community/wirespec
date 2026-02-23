use super::super::wirespec::*;
use regex;
use super::order_status::OrderStatus;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Order {
    pub id: Option<i64>,
    pub petId: Option<i64>,
    pub quantity: Option<i32>,
    pub shipDate: Option<String>,
    pub status: Option<OrderStatus>,
    pub complete: Option<bool>,
}
impl Order {
    pub fn validate(&self) -> Vec<String> {
        return Vec::<String>::new();
}
}
