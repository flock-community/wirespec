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
