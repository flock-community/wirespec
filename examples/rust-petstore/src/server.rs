use actix_web::{web, App, HttpRequest, HttpServer};
use wirespec_petstore::gen::endpoint::{add_pet, find_pets_by_status, get_pet_by_id};
use wirespec_petstore::gen::wirespec::{Method, Server};
use wirespec_petstore::register;
use wirespec_petstore::service::PetstoreService;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    println!("Starting petstore server on http://127.0.0.1:8080");

    HttpServer::new(|| {
        App::new()
            .configure(|cfg| {
                register!(cfg, PetstoreService;
                    find_pets_by_status::FindPetsByStatus,
                    add_pet::AddPet,
                    get_pet_by_id::GetPetById,
                );
            })
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}
