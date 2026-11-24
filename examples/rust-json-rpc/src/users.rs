//! Keeps the users in memory, and implements the `Service` of every rpc, so the JSON-RPC server can call it.

use std::sync::Mutex;

use crate::generated::model::user::User;
use crate::generated::model::user_not_found::UserNotFound;
use crate::generated::rpc::create_user::CreateUser;
use crate::generated::rpc::find_users::FindUsers;
use crate::generated::rpc::get_user::GetUser;
use crate::generated::rpc::rename_user::RenameUser;

#[derive(Default)]
pub struct Users {
    users: Mutex<Vec<User>>,
}

impl Users {
    fn find(&self, id: &str) -> Option<User> {
        self.users
            .lock()
            .unwrap()
            .iter()
            .find(|user| user.id == id)
            .cloned()
    }
}

impl GetUser::Service for Users {
    async fn get_user(&self, id: &str) -> GetUser::Response {
        match self.find(id) {
            Some(value) => GetUser::Response::Result(GetUser::Result { value }),
            None => GetUser::Response::Error(GetUser::Error {
                value: UserNotFound { id: id.to_string() },
            }),
        }
    }
}

impl FindUsers::Service for Users {
    async fn find_users(&self, name: Option<&str>) -> Vec<User> {
        let name = name.map(str::to_lowercase);
        self.users
            .lock()
            .unwrap()
            .iter()
            .filter(|user| {
                name.as_ref()
                    .is_none_or(|name| user.name.to_lowercase().contains(name))
            })
            .cloned()
            .collect()
    }
}

impl CreateUser::Service for Users {
    async fn create_user(&self, name: &str, email: Option<&str>) -> User {
        let mut users = self.users.lock().unwrap();
        let user = User {
            id: (users.len() + 1).to_string(),
            name: name.to_string(),
            email: email.map(String::from),
        };
        users.push(user.clone());
        user
    }
}

impl RenameUser::Service for Users {
    async fn rename_user(&self, id: &str, name: &str) -> RenameUser::Response {
        let mut users = self.users.lock().unwrap();
        match users.iter_mut().find(|user| user.id == id) {
            Some(user) => {
                user.name = name.to_string();
                RenameUser::Response::Result(RenameUser::Result {
                    value: user.clone(),
                })
            }
            None => RenameUser::Response::Error(RenameUser::Error {
                value: UserNotFound { id: id.to_string() },
            }),
        }
    }
}
