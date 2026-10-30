use std::any::TypeId;

use actix_web::{App, HttpServer};
use serde_json::{Value, json};
use wirespec_petstore::generated::client::Client;
use wirespec_petstore::generated::endpoint::{find_pets_by_status, get_pet_by_id, place_order};
use wirespec_petstore::generated::model::{
    order::Order, order_status::OrderStatus, pet::Pet, pet_status::PetStatus,
};
use wirespec_petstore::generated::wirespec::{
    BodyDeserializer, BodySerializer, ParamDeserializer, ParamSerializer,
};
use wirespec_petstore::serialization::Json;
use wirespec_petstore::server;
use wirespec_petstore::transportation::Http;

use find_pets_by_status::FindPetsByStatus::Call as _;
use get_pet_by_id::GetPetById::Call as _;
use place_order::PlaceOrder::Call as _;

#[test]
fn bodies_use_the_field_names_of_the_contract() {
    let pet = Pet {
        id: Some(1),
        name: "Rex".into(),
        photo_urls: vec!["rex.jpg".into()],
        status: Some(PetStatus::Available),
        ..Default::default()
    };

    let raw = Json.serialize_body(&pet, TypeId::of::<Pet>());

    assert_eq!(
        serde_json::from_slice::<Value>(&raw).unwrap(),
        json!({ "id": 1, "name": "Rex", "photoUrls": ["rex.jpg"], "status": "available" }),
    );
    assert_eq!(Json.deserialize_body::<Pet>(&raw, TypeId::of::<Pet>()), pet);
}

#[test]
fn parameters_are_strings() {
    let statuses = vec!["available".to_string(), "sold".to_string()];

    assert_eq!(
        Json.serialize_param(&statuses, TypeId::of::<Vec<String>>()),
        statuses
    );
    assert_eq!(Json.serialize_param(&5000, TypeId::of::<i32>()), ["5000"]);
    assert_eq!(
        Json.deserialize_param::<Vec<String>>(&["sold".into()], TypeId::of::<Vec<String>>()),
        ["sold"]
    );
    assert_eq!(
        Json.deserialize_param::<String>(&["42".into()], TypeId::of::<String>()),
        "42"
    );
    assert_eq!(
        Json.deserialize_param::<i32>(&["42".into()], TypeId::of::<i32>()),
        42
    );
}

#[actix_web::test]
async fn client_calls_server_over_http() {
    let server = HttpServer::new(|| App::new().configure(server::configure))
        .workers(1)
        .bind(("127.0.0.1", 0))
        .unwrap();
    let petstore = Client {
        serialization: Json,
        transportation: Http::new(&format!("http://{}", server.addrs()[0])),
    };
    actix_web::rt::spawn(server.run());

    let find_pets_by_status::Response::Response200(sold) =
        petstore.find_pets_by_status(vec!["sold".into()]).await
    else {
        panic!("expected sold pets")
    };
    assert_eq!(
        sold.body
            .iter()
            .map(|pet| pet.name.as_str())
            .collect::<Vec<_>>(),
        ["Tom"]
    );

    let get_pet_by_id::Response::Response200(rex) = petstore.get_pet_by_id(1).await else {
        panic!("expected Rex")
    };
    assert_eq!(
        rex.body
            .category
            .and_then(|category| category.name)
            .as_deref(),
        Some("Dogs")
    );
    assert_eq!(rex.body.tags.unwrap()[0].name.as_deref(), Some("good boy"));

    assert!(matches!(
        petstore.get_pet_by_id(99).await,
        get_pet_by_id::Response::Response404(_)
    ));

    let order = Order {
        pet_id: Some(1),
        quantity: Some(2),
        ..Default::default()
    };
    assert_eq!(
        petstore.place_order(order.clone()).await,
        place_order::Response200::new(Order {
            id: Some(1),
            status: Some(OrderStatus::Placed),
            ..order
        })
        .into(),
    );
}
