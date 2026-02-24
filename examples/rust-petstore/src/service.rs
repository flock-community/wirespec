use crate::gen::endpoint::{add_pet, find_pets_by_status, get_pet_by_id};
use crate::gen::model::category::Category;
use crate::gen::model::pet::Pet;

pub struct PetstoreService;

impl get_pet_by_id::GetPetById::Handler for PetstoreService {
    fn get_pet_by_id(&self, request: get_pet_by_id::Request) -> get_pet_by_id::Response {
        let pet = Pet {
            id: Some(request.path.pet_id),
            category: Some(Category {
                id: Some(1),
                name: Some("Dogs".into()),
            }),
            name: format!("Pet {}", request.path.pet_id),
            photoUrls: vec!["https://example.com/pet.jpg".into()],
            tags: None,
            status: None,
        };
        get_pet_by_id::Response200::new(pet).into()
    }
}

impl find_pets_by_status::FindPetsByStatus::Handler for PetstoreService {
    fn find_pets_by_status(
        &self,
        _request: find_pets_by_status::Request,
    ) -> find_pets_by_status::Response {
        let pets = vec![Pet {
            id: Some(1),
            category: Some(Category {
                id: Some(1),
                name: Some("Dogs".into()),
            }),
            name: "Buddy".into(),
            photoUrls: vec!["https://example.com/buddy.jpg".into()],
            tags: None,
            status: None,
        }];
        find_pets_by_status::Response200::new(pets).into()
    }
}

impl add_pet::AddPet::Handler for PetstoreService {
    fn add_pet(&self, request: add_pet::Request) -> add_pet::Response {
        println!("Received pet: {:?}", request.body);
        add_pet::Response405::new().into()
    }
}
