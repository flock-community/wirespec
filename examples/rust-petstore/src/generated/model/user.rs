use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct User {
    pub id: Option<i64>,
    pub username: Option<String>,
    pub firstName: Option<String>,
    pub lastName: Option<String>,
    pub email: Option<String>,
    pub password: Option<String>,
    pub phone: Option<String>,
    pub userStatus: Option<i32>,
}
impl User {
    pub fn validate(&self) -> Vec<String> {
        return Vec::<String>::new();
}
}
