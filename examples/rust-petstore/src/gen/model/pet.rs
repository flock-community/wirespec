use super::super::wirespec::*;
use regex;
use super::category::Category;
use super::tag::Tag;
use super::pet_status::PetStatus;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Pet {
    pub id: Option<i64>,
    pub category: Option<Category>,
    #[serde(default, deserialize_with = "null_default")]
    pub name: String,
    #[serde(default, deserialize_with = "null_default")]
    pub photo_urls: Vec<String>,
    pub tags: Option<Vec<Tag>>,
    pub status: Option<PetStatus>,
}
impl Pet {
    pub fn validate(&self) -> Vec<String> {
        return vec![self.category.as_ref().map(|it| it.validate().iter().map(|e| format!("category.{}", e)).collect::<Vec<_>>()).unwrap_or(Vec::<String>::new()).as_slice(), self.tags.as_ref().map(|it| it.iter().enumerate().flat_map(|(i, el)| el.validate().iter().map(|e| format!("tags[{}].{}", i, e)).collect::<Vec<_>>()).collect::<Vec<_>>()).unwrap_or(Vec::<String>::new()).as_slice()].concat();
    }
}
