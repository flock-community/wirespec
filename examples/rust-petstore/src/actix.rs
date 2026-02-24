use actix_web::{web, HttpRequest, HttpResponse};
use crate::gen::wirespec::{Method, RawRequest, RawResponse, Server};
use std::collections::HashMap;

pub fn to_raw_request(req: &HttpRequest, body: web::Bytes) -> RawRequest {
    let path: Vec<String> = req
        .path()
        .split('/')
        .filter(|s| !s.is_empty())
        .map(String::from)
        .collect();

    let mut queries: HashMap<String, Vec<String>> = HashMap::new();
    if let Some(query_string) = req.uri().query() {
        for pair in query_string.split('&') {
            if let Some((key, value)) = pair.split_once('=') {
                queries
                    .entry(key.to_string())
                    .or_default()
                    .push(value.to_string());
            }
        }
    }

    let mut headers: HashMap<String, Vec<String>> = HashMap::new();
    for (key, value) in req.headers() {
        headers
            .entry(key.as_str().to_string())
            .or_default()
            .push(value.to_str().unwrap_or("").to_string());
    }

    RawRequest {
        method: req.method().to_string(),
        path,
        queries,
        headers,
        body: if body.is_empty() {
            None
        } else {
            Some(body.to_vec())
        },
    }
}

pub fn to_http_response(raw: RawResponse) -> HttpResponse {
    let mut builder = HttpResponse::build(
        actix_web::http::StatusCode::from_u16(raw.status_code as u16)
            .unwrap_or(actix_web::http::StatusCode::INTERNAL_SERVER_ERROR),
    );

    for (key, values) in &raw.headers {
        for value in values {
            builder.append_header((key.as_str(), value.as_str()));
        }
    }

    match raw.body {
        Some(body) => builder.content_type("application/json").body(body),
        None => builder.finish(),
    }
}

#[macro_export]
macro_rules! register {
    ($cfg:expr, $handler_type:ident; $($module:ident :: $ns:ident),* $(,)?) => {
        $cfg.app_data(web::Data::new($handler_type));
        $cfg.app_data(web::Data::new($crate::serialization::JsonSerialization));
        $(register!($cfg, $module::$ns, $handler_type);)*
    };
    ($cfg:expr, $module:ident :: $ns:ident, $handler_type:ty) => {{
        let api = $module::$ns::Api;
        let path = api.path_template();
        let method = api.method();
        let route = match method {
            Method::GET => web::get(),
            Method::PUT => web::put(),
            Method::POST => web::post(),
            Method::DELETE => web::delete(),
            Method::HEAD => web::head(),
            Method::PATCH => web::patch(),
            Method::OPTIONS => web::method(actix_web::http::Method::OPTIONS),
            Method::TRACE => web::method(actix_web::http::Method::TRACE),
        };
        $cfg.route(
            path,
            route.to(
                |req: HttpRequest,
                 body: web::Bytes,
                 handler: web::Data<$handler_type>,
                 ser: web::Data<$crate::serialization::JsonSerialization>| async move {
                    let raw = $crate::actix::to_raw_request(&req, body);
                    let typed_req = $module::$ns::from_raw_request(&**ser, raw);
                    let typed_res =
                        <$handler_type as $module::$ns::Handler>::$module(
                            &**handler, typed_req,
                        );
                    let raw_res = $module::$ns::to_raw_response(&**ser, typed_res);
                    $crate::actix::to_http_response(raw_res)
                },
            ),
        );
    }};
}
