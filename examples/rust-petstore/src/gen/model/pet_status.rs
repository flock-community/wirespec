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
impl Enum for PetStatus {
    fn label(&self) -> &str {
        match self {
            PetStatus::Available => "available",
            PetStatus::Pending => "pending",
            PetStatus::Sold => "sold",
        }
    }
    fn from_label(s: &str) -> Option<Self> {
        match s {
            "available" => Some(PetStatus::Available),
            "pending" => Some(PetStatus::Pending),
            "sold" => Some(PetStatus::Sold),
            _ => None,
        }
    }
}
impl std::fmt::Display for PetStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{}", self.label())
    }
}
