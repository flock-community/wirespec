//! Serves every rpc at `POST /rpc` with actix-web.
//!
//! The generated `JsonRpcServer` reads a request, single or batched, from the body, calls `Users`, which implements
//! the `Service` of every rpc, and writes the response.

use std::collections::HashMap;

use actix_web::http::StatusCode;
use actix_web::{HttpResponse, web};

use crate::generated::json_rpc::JsonRpcServer;
use crate::generated::wirespec::RawRequest;
use crate::serialization::Json;
use crate::users::Users;

pub type Server = JsonRpcServer<Json, Users>;

pub fn server() -> web::Data<Server> {
    web::Data::new(JsonRpcServer {
        serialization: Json,
        service: Users::default(),
    })
}

pub fn configure(config: &mut web::ServiceConfig) {
    config.route("/rpc", web::post().to(rpc));
}

async fn rpc(server: web::Data<Server>, body: web::Bytes) -> HttpResponse {
    let request = RawRequest {
        method: String::from("POST"),
        path: vec![String::from("rpc")],
        queries: HashMap::new(),
        headers: HashMap::new(),
        body: Some(body.to_vec()),
    };
    let response = server.handle(&request).await;
    let mut http = HttpResponse::build(
        StatusCode::from_u16(response.status_code as u16).expect("a status code of JSON-RPC"),
    );
    match response.body {
        Some(body) => http.content_type("application/json").body(body),
        None => http.finish(),
    }
}
