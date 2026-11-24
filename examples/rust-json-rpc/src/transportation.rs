//! Sends the JSON-RPC requests of the generated client to a server over HTTP with reqwest.

use std::collections::HashMap;

use reqwest::header::CONTENT_TYPE;

use crate::generated::wirespec::{RawRequest, RawResponse, Transportation, WirespecError};

/// The client posts every request to the empty path, so the transportation decides where it goes: the JSON-RPC
/// endpoint of the server, such as `http://localhost:8080/rpc`.
pub struct Http {
    client: reqwest::Client,
    url: String,
}

impl Http {
    pub fn new(url: &str) -> Self {
        Http {
            client: reqwest::Client::new(),
            url: url.to_string(),
        }
    }
}

impl Transportation for Http {
    async fn transport(&self, request: &RawRequest) -> Result<RawResponse, WirespecError> {
        let method = request
            .method
            .parse()
            .map_err(|_| WirespecError::Transport(format!("invalid method {}", request.method)))?;
        let response = self
            .client
            .request(method, &self.url)
            .header(CONTENT_TYPE, "application/json")
            .body(request.body.clone().unwrap_or_default())
            .send()
            .await
            .map_err(unreachable)?;
        let status_code = response.status().as_u16().into();
        let body = response.bytes().await.map_err(unreachable)?.to_vec();
        Ok(RawResponse {
            status_code,
            headers: HashMap::new(),
            body: Some(body).filter(|body| !body.is_empty()),
        })
    }
}

/// A server that cannot be reached is an error for the caller, not a panic.
fn unreachable(error: reqwest::Error) -> WirespecError {
    WirespecError::Transport(error.to_string())
}
