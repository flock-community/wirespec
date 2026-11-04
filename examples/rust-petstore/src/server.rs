//! Serves the Petstore with actix-web.
//!
//! Every generated endpoint knows its path template and method (`Api`), and converts between the raw
//! and the typed request and response. `route!` hands those to actix-web, and `PetStore` implements
//! the typed `Handler` of each endpoint.

use actix_web::http::{Method, StatusCode};
use actix_web::{HttpRequest, HttpResponse, web};

use crate::generated::endpoint::{find_pets_by_status, get_pet_by_id, place_order};
use crate::generated::model::{
    category::Category, order::Order, order_status::OrderStatus, pet::Pet, pet_status::PetStatus,
    tag::Tag,
};
use crate::generated::wirespec::{Enum, RawRequest, RawResponse, Server};
use crate::serialization::Json;
use crate::{group, ungroup};

macro_rules! route {
    ($config:ident, $endpoint:ident :: $api:ident) => {
        $config.route(
            $endpoint::$api::Api.path_template(),
            web::method(
                Method::from_bytes(format!("{:?}", $endpoint::$api::Api.method()).as_bytes())
                    .unwrap(),
            )
            .to(|http: HttpRequest, body: web::Bytes| async move {
                let request = $endpoint::$api::from_raw_request(&Json, to_raw_request(&http, body));
                let response =
                    <PetStore as $endpoint::$api::Handler>::$endpoint(&PetStore, request).await;
                to_http_response($endpoint::$api::to_raw_response(&Json, response))
            }),
        )
    };
}

pub fn configure(config: &mut web::ServiceConfig) {
    route!(config, find_pets_by_status::FindPetsByStatus);
    route!(config, get_pet_by_id::GetPetById);
    route!(config, place_order::PlaceOrder);
}

fn to_raw_request(http: &HttpRequest, body: web::Bytes) -> RawRequest {
    let queries = web::Query::<Vec<(String, String)>>::from_query(http.query_string())
        .map(web::Query::into_inner)
        .unwrap_or_default();
    RawRequest {
        method: http.method().to_string(),
        path: http
            .match_info()
            .as_str()
            .split('/')
            .filter(|segment| !segment.is_empty())
            .map(String::from)
            .collect(),
        queries: group(queries),
        headers: group(http.headers().iter().map(|(name, value)| {
            (
                name.to_string(),
                value.to_str().unwrap_or_default().to_string(),
            )
        })),
        body: Some(body.to_vec()).filter(|body| !body.is_empty()),
    }
}

fn to_http_response(raw: RawResponse) -> HttpResponse {
    let mut response = HttpResponse::build(
        StatusCode::from_u16(raw.status_code as u16).expect("invalid status code"),
    );
    ungroup(&raw.headers).for_each(|(name, value)| {
        response.append_header((name.as_str(), value.as_str()));
    });
    match raw.body {
        Some(body) => response.content_type("application/json").body(body),
        None => response.finish(),
    }
}

struct PetStore;

impl find_pets_by_status::FindPetsByStatus::Handler for PetStore {
    async fn find_pets_by_status(
        &self,
        request: find_pets_by_status::Request,
    ) -> find_pets_by_status::Response {
        let pets = pets()
            .into_iter()
            .filter(|pet| {
                pet.status.as_ref().is_some_and(|status| {
                    request.queries.status.iter().any(|it| it == status.label())
                })
            })
            .collect();
        find_pets_by_status::Response200::new(pets).into()
    }
}

impl get_pet_by_id::GetPetById::Handler for PetStore {
    async fn get_pet_by_id(&self, request: get_pet_by_id::Request) -> get_pet_by_id::Response {
        match pets()
            .into_iter()
            .find(|pet| pet.id == Some(request.path.pet_id))
        {
            Some(pet) => get_pet_by_id::Response200::new(pet).into(),
            None => get_pet_by_id::Response404::new().into(),
        }
    }
}

impl place_order::PlaceOrder::Handler for PetStore {
    async fn place_order(&self, request: place_order::Request) -> place_order::Response {
        let order = Order {
            id: Some(1),
            status: Some(OrderStatus::Placed),
            ..request.body
        };
        place_order::Response200::new(order).into()
    }
}

fn pets() -> Vec<Pet> {
    vec![
        Pet {
            id: Some(1),
            name: "Rex".into(),
            category: Some(Category {
                id: Some(1),
                name: Some("Dogs".into()),
            }),
            tags: Some(vec![Tag {
                id: Some(1),
                name: Some("good boy".into()),
            }]),
            status: Some(PetStatus::Available),
            ..Default::default()
        },
        Pet {
            id: Some(2),
            name: "Tom".into(),
            category: Some(Category {
                id: Some(2),
                name: Some("Cats".into()),
            }),
            status: Some(PetStatus::Sold),
            ..Default::default()
        },
    ]
}
