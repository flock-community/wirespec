use actix_web::{App, HttpServer};
use wirespec_json_rpc::server;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    println!("JSON-RPC listening on http://localhost:8080/rpc");
    // One server for every worker, so they share the users.
    let users = server::server();
    HttpServer::new(move || {
        App::new()
            .app_data(users.clone())
            .configure(server::configure)
    })
    .bind(("127.0.0.1", 8080))?
    .run()
    .await
}
