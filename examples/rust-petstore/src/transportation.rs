//! Sends the raw requests of the generated client over HTTP with reqwest.

use reqwest::Url;
use reqwest::header::{ACCEPT, CONTENT_TYPE};

use crate::generated::wirespec::{RawRequest, RawResponse, Transportation, WirespecError};
use crate::{group, ungroup};

pub struct Http {
    client: reqwest::Client,
    base_url: Url,
}

impl Http {
    pub fn new(base_url: &str) -> Self {
        Http {
            client: reqwest::Client::new(),
            base_url: Url::parse(base_url).expect("invalid base URL"),
        }
    }
}

impl Transportation for Http {
    async fn transport(&self, request: &RawRequest) -> Result<RawResponse, WirespecError> {
        let mut url = self.base_url.clone();
        url.path_segments_mut()
            .expect("invalid base URL")
            .pop_if_empty()
            .extend(&request.path);

        let builder = self
            .client
            .request(request.method.parse().expect("invalid method"), url)
            .query(&ungroup(&request.queries).collect::<Vec<_>>())
            .header(ACCEPT, "application/json");
        let builder = ungroup(&request.headers).fold(builder, |builder, (name, value)| {
            builder.header(name, value)
        });
        let builder = match &request.body {
            Some(body) => builder
                .header(CONTENT_TYPE, "application/json")
                .body(body.clone()),
            None => builder,
        };

        let response = builder.send().await.map_err(unreachable)?;
        let status_code = response.status().as_u16().into();
        let headers = group(response.headers().iter().map(|(name, value)| {
            (
                name.to_string(),
                value.to_str().unwrap_or_default().to_string(),
            )
        }));
        let body = response.bytes().await.map_err(unreachable)?.to_vec();
        Ok(RawResponse {
            status_code,
            headers,
            body: Some(body).filter(|body| !body.is_empty()),
        })
    }
}

/// A server that cannot be reached is an error for the caller, not a panic.
fn unreachable(error: reqwest::Error) -> WirespecError {
    WirespecError::Transport(error.to_string())
}
