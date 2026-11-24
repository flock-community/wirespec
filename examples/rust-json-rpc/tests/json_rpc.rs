use actix_web::{App, HttpServer};
use serde_json::{Value, json};
use wirespec_json_rpc::generated::json_rpc::{JsonRpcClient, JsonRpcError, METHOD_NOT_FOUND};
use wirespec_json_rpc::generated::model::user::User;
use wirespec_json_rpc::generated::model::user_not_found::UserNotFound;
use wirespec_json_rpc::generated::rpc::get_user::GetUser;
use wirespec_json_rpc::generated::rpc::rename_user::RenameUser;
use wirespec_json_rpc::serialization::Json;
use wirespec_json_rpc::server;
use wirespec_json_rpc::transportation::Http;

/// Serves the rpcs on a free port, and gives the URL of the JSON-RPC endpoint.
fn serve() -> String {
    let users = server::server();
    let server = HttpServer::new(move || {
        App::new()
            .app_data(users.clone())
            .configure(server::configure)
    })
    .workers(1)
    .bind(("127.0.0.1", 0))
    .expect("a free port");
    let url = format!("http://{}/rpc", server.addrs()[0]);
    actix_web::rt::spawn(server.run());
    url
}

/// Posts a JSON-RPC message the way any other client would, and reads the answer, if there is one.
async fn post(url: &str, message: &str) -> (u16, Value) {
    let response = reqwest::Client::new()
        .post(url)
        .body(message.to_string())
        .send()
        .await
        .unwrap();
    let status = response.status().as_u16();
    let body = response.bytes().await.unwrap();
    (
        status,
        if body.is_empty() {
            Value::Null
        } else {
            serde_json::from_slice(&body).unwrap()
        },
    )
}

fn user(id: &str, name: &str, email: Option<&str>) -> User {
    User {
        id: id.to_string(),
        name: name.to_string(),
        email: email.map(String::from),
    }
}

#[actix_web::test]
async fn the_client_calls_the_server_over_http() -> Result<(), JsonRpcError> {
    let users = JsonRpcClient {
        serialization: Json,
        transportation: Http::new(&serve()),
    };

    let ada = users.create_user("Ada", Some("ada@example.com")).await?;
    let grace = users.create_user("Grace", None).await?;

    assert_eq!(ada, user("1", "Ada", Some("ada@example.com")));
    assert_eq!(
        users.get_user("1").await?,
        GetUser::Response::Result(GetUser::Result { value: ada.clone() })
    );
    assert_eq!(users.find_users(None).await?, vec![ada, grace.clone()]);
    assert_eq!(users.find_users(Some("grace")).await?, vec![grace]);
    assert_eq!(
        users.rename_user("2", "Grace Hopper").await?,
        RenameUser::Response::Result(RenameUser::Result {
            value: user("2", "Grace Hopper", None)
        })
    );
    Ok(())
}

#[actix_web::test]
async fn the_error_of_an_rpc_is_a_value() -> Result<(), JsonRpcError> {
    let users = JsonRpcClient {
        serialization: Json,
        transportation: Http::new(&serve()),
    };

    assert_eq!(
        users.get_user("42").await?,
        GetUser::Response::Error(GetUser::Error {
            value: UserNotFound {
                id: String::from("42")
            }
        })
    );
    Ok(())
}

#[actix_web::test]
async fn any_json_rpc_client_can_call_the_server() {
    let url = serve();
    let (_, batch) = post(
        &url,
        r#"[{"jsonrpc":"2.0","method":"createUser","params":["Ada"],"id":1},{"jsonrpc":"2.0","method":"users/rename","params":{"id":"2","name":"Nobody"},"id":"b"}]"#,
    )
    .await;
    assert_eq!(
        batch,
        json!([
            { "jsonrpc": "2.0", "result": { "id": "1", "name": "Ada", "email": null }, "id": 1 },
            { "jsonrpc": "2.0", "error": { "code": -32000, "message": "Server error", "data": { "id": "2" } }, "id": "b" },
        ])
    );

    let (status, _) = post(
        &url,
        r#"{"jsonrpc":"2.0","method":"createUser","params":{"name":"Grace"}}"#,
    )
    .await;
    assert_eq!(status, 204);
}

#[actix_web::test]
async fn a_request_the_server_cannot_answer_fails() {
    // A server that serves no rpcs at /rpc answers with an HTTP error, not with JSON-RPC.
    let unknown = JsonRpcClient {
        serialization: Json,
        transportation: Http::new(&serve().replace("/rpc", "/nothing")),
    };
    assert!(matches!(
        unknown.find_users(None).await,
        Err(JsonRpcError::Wirespec(_))
    ));

    let (_, error) = post(
        &serve(),
        r#"{"jsonrpc":"2.0","method":"deleteUser","id":1}"#,
    )
    .await;
    assert_eq!(error["error"]["code"], METHOD_NOT_FOUND);
}
