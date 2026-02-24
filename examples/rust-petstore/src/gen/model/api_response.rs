use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct ApiResponse {
    pub code: Option<i32>,
    #[serde(rename = "type")]
    pub r#type: Option<String>,
    pub message: Option<String>,
}
impl ApiResponse {
    pub fn validate(&self) -> Vec<String> {
        return Vec::<String>::new();
    }
}
