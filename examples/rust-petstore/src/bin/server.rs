use actix_web::{App, HttpServer};
use wirespec_petstore::server;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    println!("Petstore listening on http://localhost:8080");
    HttpServer::new(|| App::new().configure(server::configure))
        .bind(("127.0.0.1", 8080))?
        .run()
        .await
}
