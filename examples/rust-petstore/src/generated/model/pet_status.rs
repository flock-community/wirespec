use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum PetStatus {
    #[serde(rename = "available")]
    Available,
    #[serde(rename = "pending")]
    Pending,
    #[serde(rename = "sold")]
    Sold,
}
