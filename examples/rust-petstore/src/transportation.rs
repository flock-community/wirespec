use crate::generated::wirespec::{Client, RawRequest, RawResponse, Transportation};
use crate::serialization::JsonSerialization;
use std::collections::HashMap;

pub struct ReqwestTransport {
    client: reqwest::blocking::Client,
    base_url: String,
}

impl ReqwestTransport {
    pub fn new(base_url: &str) -> Self {
        ReqwestTransport {
            client: reqwest::blocking::Client::new(),
            base_url: base_url.to_string(),
        }
    }
}

impl Transportation for ReqwestTransport {
    fn transport(&self, request: &RawRequest) -> RawResponse {
        let path = request.path.join("/");
        let url = format!("{}/{}", self.base_url, path);

        let mut req_builder = match request.method.as_str() {
            "GET" => self.client.get(&url),
            "POST" => self.client.post(&url),
            "PUT" => self.client.put(&url),
            "DELETE" => self.client.delete(&url),
            "PATCH" => self.client.patch(&url),
            "HEAD" => self.client.head(&url),
            _ => self.client.get(&url),
        };

        for (key, values) in &request.queries {
            for value in values {
                req_builder = req_builder.query(&[(key, value)]);
            }
        }

        for (key, values) in &request.headers {
            if let Some(value) = values.first() {
                req_builder = req_builder.header(key.as_str(), value.as_str());
            }
        }

        req_builder = req_builder.header("Accept", "application/json");
        req_builder = req_builder.header("Content-Type", "application/json");

        if let Some(body) = &request.body {
            req_builder = req_builder.body(body.clone());
        }

        match req_builder.send() {
            Ok(response) => {
                let status_code = response.status().as_u16() as i32;
                let mut headers: HashMap<String, Vec<String>> = HashMap::new();
                for (key, value) in response.headers() {
                    headers
                        .entry(key.to_string())
                        .or_default()
                        .push(value.to_str().unwrap_or("").to_string());
                }
                let body = response.bytes().ok().map(|b| b.to_vec());

                RawResponse {
                    status_code,
                    headers,
                    body,
                }
            }
            Err(e) => {
                eprintln!("Transport error: {}", e);
                RawResponse {
                    status_code: 0,
                    headers: HashMap::new(),
                    body: None,
                }
            }
        }
    }
}

pub struct ClientProxy<T: Transportation> {
    pub transport: T,
    pub serialization: JsonSerialization,
}

impl<T: Transportation> Client for ClientProxy<T> {
    type Transport = T;
    type Ser = JsonSerialization;
    fn transport(&self) -> &T { &self.transport }
    fn serialization(&self) -> &JsonSerialization { &self.serialization }
}
