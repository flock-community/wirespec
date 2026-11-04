use wirespec_petstore::generated::client::Client;
use wirespec_petstore::generated::endpoint::{find_pets_by_status, get_pet_by_id, place_order};
use wirespec_petstore::generated::model::order::Order;
use wirespec_petstore::serialization::Json;
use wirespec_petstore::transportation::Http;

use find_pets_by_status::FindPetsByStatus::Call as _;
use get_pet_by_id::GetPetById::Call as _;
use place_order::PlaceOrder::Call as _;

#[tokio::main(flavor = "current_thread")]
async fn main() {
    let base_url = std::env::args()
        .nth(1)
        .unwrap_or_else(|| "http://localhost:8080".into());
    let petstore = Client {
        serialization: Json,
        transportation: Http::new(&base_url),
    };

    match petstore.find_pets_by_status(vec!["available".into()]).await {
        find_pets_by_status::Response::Response200(response) => response
            .body
            .iter()
            .for_each(|pet| println!("Available: {} ({:?})", pet.name, pet.id)),
        find_pets_by_status::Response::Response400(_) => println!("Invalid status"),
    }

    match petstore.get_pet_by_id(1).await {
        get_pet_by_id::Response::Response200(response) => println!("Pet 1: {:?}", response.body),
        get_pet_by_id::Response::Response400(_) => println!("Invalid id"),
        get_pet_by_id::Response::Response404(_) => println!("Pet 1 not found"),
    }

    let order = Order {
        pet_id: Some(1),
        quantity: Some(1),
        ..Default::default()
    };
    match petstore.place_order(order).await {
        place_order::Response::Response200(response) => {
            println!("Order placed: {:?}", response.body)
        }
        place_order::Response::Response400(_) => println!("Invalid order"),
    }
}
