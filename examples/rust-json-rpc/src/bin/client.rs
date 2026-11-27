use wirespec_json_rpc::generated::json_rpc::{JsonRpcClient, JsonRpcError};
use wirespec_json_rpc::generated::rpc::get_user::GetUser;
use wirespec_json_rpc::generated::rpc::rename_user::RenameUser;
use wirespec_json_rpc::serialization::Json;
use wirespec_json_rpc::transportation::Http;

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), JsonRpcError> {
    let url = std::env::args()
        .nth(1)
        .unwrap_or_else(|| "http://localhost:8080/rpc".into());
    let users = JsonRpcClient {
        serialization: Json,
        transportation: Http::new(&url),
    };

    let ada = users.create_user("Ada", Some("ada@example.com")).await?;
    println!("Created: {ada:?}");

    match users.rename_user(&ada.id, "Ada Lovelace").await? {
        RenameUser::Response::Result(result) => println!("Renamed: {:?}", result.value),
        RenameUser::Response::Error(error) => println!("No user {}", error.value.id),
    }

    match users.get_user("42").await? {
        GetUser::Response::Result(result) => println!("Found: {:?}", result.value),
        GetUser::Response::Error(error) => println!("No user {}", error.value.id),
    }

    println!("All: {:?}", users.find_users(None).await?);
    Ok(())
}
