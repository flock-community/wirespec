use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Category {
    pub id: Option<i64>,
    pub name: Option<String>,
}
impl Category {
    pub fn validate(&self) -> Vec<String> {
        return Vec::<String>::new();
    }
}
