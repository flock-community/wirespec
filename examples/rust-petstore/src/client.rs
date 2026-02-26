use wirespec_petstore::gen::endpoint::{
    find_pets_by_status, get_inventory, get_pet_by_id, place_order,
};
use wirespec_petstore::gen::model::category::Category;
use wirespec_petstore::gen::model::order::Order;
use wirespec_petstore::gen::model::order_status::OrderStatus;
use wirespec_petstore::gen::model::pet::Pet;
use wirespec_petstore::gen::model::pet_status::PetStatus;
use wirespec_petstore::gen::model::tag::Tag;
use wirespec_petstore::gen::wirespec::Client;
use wirespec_petstore::serialization::JsonSerialization;
use wirespec_petstore::transportation::{ClientProxy, ReqwestTransport};

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
            println!("  Photo URLs: {:?}", r.body.photo_urls);
        }
        _ => println!("Error fetching pet"),
    }

    println!("\n=== Add a new pet ===");
    let _new_pet = Pet {
        id: None,
        category: Some(Category {
            id: Some(1),
            name: Some("Dogs".into()),
        }),
        name: "Wirespec Dog".into(),
        photo_urls: vec!["https://example.com/dog.jpg".into()],
        tags: Some(vec![Tag {
            id: Some(1),
            name: Some("gen".into()),
        }]),
        status: Some(PetStatus::Available),
    };

    println!("\n=== Get store inventory ===");
    let resp = api.get_inventory(get_inventory::Request::new());
    match resp {
        get_inventory::Response::Response200(r) => {
            for (status, count) in &r.body {
                println!("  {}: {}", status, count);
            }
        }
    }

    println!("\n=== Place an order ===");
    let order = Order {
        id: None,
        pet_id: Some(1),
        quantity: Some(1),
        ship_date: Some("2025-01-01T00:00:00.000Z".into()),
        status: Some(OrderStatus::Placed),
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
