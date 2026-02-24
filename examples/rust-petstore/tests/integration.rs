use actix_web::{web, App, HttpRequest, HttpServer};
use wirespec_petstore::gen::endpoint::{add_pet, find_pets_by_status, get_pet_by_id};
use wirespec_petstore::gen::model::pet::Pet;
use wirespec_petstore::gen::wirespec::{Method, Server};
use wirespec_petstore::register;
use wirespec_petstore::serialization::JsonSerialization;
use wirespec_petstore::service::PetstoreService;
use wirespec_petstore::transportation::{ClientProxy, ReqwestTransport};

use add_pet::AddPet::Handler as _;
use find_pets_by_status::FindPetsByStatus::Handler as _;
use get_pet_by_id::GetPetById::Handler as _;

#[test]
fn test_petstore_endpoints() {
    let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
    let port = listener.local_addr().unwrap().port();

    std::thread::spawn(move || {
        let rt = tokio::runtime::Runtime::new().unwrap();
        rt.block_on(async {
            HttpServer::new(|| {
                App::new().configure(|cfg| {
                    register!(cfg, PetstoreService;
                        find_pets_by_status::FindPetsByStatus,
                        add_pet::AddPet,
                        get_pet_by_id::GetPetById,
                    );
                })
            })
            .listen(listener)
            .unwrap()
            .run()
            .await
            .unwrap();
        });
    });

    // Wait for server readiness
    for _ in 0..50 {
        if std::net::TcpStream::connect(format!("127.0.0.1:{}", port)).is_ok() {
            break;
        }
        std::thread::sleep(std::time::Duration::from_millis(100));
    }

    let api = ClientProxy {
        transport: ReqwestTransport::new(&format!("http://127.0.0.1:{}", port)),
        serialization: JsonSerialization,
    };

    // Test GetPetById: request pet ID 42 → expect 200 with id=42, name="Pet 42"
    let resp = api.get_pet_by_id(get_pet_by_id::Request::new(42));
    match resp {
        get_pet_by_id::Response::Response200(r) => {
            assert_eq!(r.body.id, Some(42));
            assert_eq!(r.body.name, "Pet 42");
        }
        other => panic!("Expected Response200, got {:?}", other),
    }

    // Test FindPetsByStatus: request status "available" → expect 200 with 1 pet named "Buddy"
    let resp =
        api.find_pets_by_status(find_pets_by_status::Request::new(vec!["available".into()]));
    match resp {
        find_pets_by_status::Response::Response200(r) => {
            assert_eq!(r.body.len(), 1);
            assert_eq!(r.body[0].name, "Buddy");
        }
        other => panic!("Expected Response200, got {:?}", other),
    }

    // Test AddPet: send a pet → expect 405 response
    let pet = Pet {
        id: None,
        category: None,
        name: "TestPet".into(),
        photoUrls: vec![],
        tags: None,
        status: None,
    };
    let resp = api.add_pet(add_pet::Request::new(pet));
    match resp {
        add_pet::Response::Response405(_) => {}
    }
}
