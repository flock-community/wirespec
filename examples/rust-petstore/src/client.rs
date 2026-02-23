mod generated;
mod serialization;
mod transportation;

use generated::endpoint::{
    find_pets_by_status, get_inventory, get_pet_by_id, place_order,
};
use generated::model::category::Category;
use generated::model::order::Order;
use generated::model::pet::Pet;
use generated::model::tag::Tag;
use generated::wirespec::Client;
use serialization::JsonSerialization;
use transportation::{ClientProxy, ReqwestTransport};

trait PetstoreApi:
    find_pets_by_status::FindPetsByStatus::Handler
    + get_pet_by_id::GetPetById::Handler
    + get_inventory::GetInventory::Handler
    + place_order::PlaceOrder::Handler
{}

impl<T: Client> PetstoreApi for T {}

fn main() {
    let api = ClientProxy {
        transport: ReqwestTransport::new("https://petstore.swagger.io/v2"),
        serialization: JsonSerialization,
    };
    run(&api);
}

fn run(api: &impl PetstoreApi) {
    println!("=== Find pets by status 'available' ===");
    let resp = api.find_pets_by_status(find_pets_by_status::Request::new(vec!["available".into()]));
    match resp {
        find_pets_by_status::Response::Response200(r) => {
            println!("Found {} pets", r.body.len());
            for pet in r.body.iter().take(3) {
                println!(
                    "  - {} (id: {:?}, status: {:?})",
                    pet.name, pet.id, pet.status
                );
            }
        }
        _ => println!("Error fetching pets"),
    }

    println!("\n=== Get pet by ID ===");
    let resp = api.get_pet_by_id(get_pet_by_id::Request::new(1));
    match resp {
        get_pet_by_id::Response::Response200(r) => {
            println!("Pet: {} (id: {:?})", r.body.name, r.body.id);
            println!("  Category: {:?}", r.body.category);
            println!("  Tags: {:?}", r.body.tags);
            println!("  Status: {:?}", r.body.status);
            println!("  Photo URLs: {:?}", r.body.photoUrls);
        }
        _ => println!("Error fetching pet"),
    }

    println!("\n=== Add a new pet ===");
    let new_pet = Pet {
        id: None,
        category: Some(Category {
            id: Some(1),
            name: Some("Dogs".into()),
        }),
        name: "Wirespec Dog".into(),
        photoUrls: vec!["https://example.com/dog.jpg".into()],
        tags: Some(vec![Tag {
            id: Some(1),
            name: Some("generated".into()),
        }]),
        status: Some(generated::model::pet_status::PetStatus::Available),
    };

    println!("\n=== Get store inventory ===");
    let resp = api.get_inventory(get_inventory::Request::new());
    match resp {
        get_inventory::Response::Response200(r) => {
            for (status, count) in &r.body {
                println!("  {}: {}", status, count);
            }
        }
        _ => println!("Error fetching inventory"),
    }

    println!("\n=== Place an order ===");
    let order = Order {
        id: None,
        petId: Some(1),
        quantity: Some(1),
        shipDate: Some("2025-01-01T00:00:00.000Z".into()),
        status: Some(generated::model::order_status::OrderStatus::Placed),
        complete: Some(false),
    };
    let resp = api.place_order(place_order::Request::new(order));
    match resp {
        place_order::Response::Response200(r) => println!(
            "Order placed: id={:?}, status={:?}",
            r.body.id, r.body.status
        ),
        _ => println!("Error placing order"),
    }
}
