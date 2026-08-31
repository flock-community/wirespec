//! The serving side of the client: the Kotlin backend calls rpcs here, in the
//! server-to-client direction. `TraderTerminal` implements the generated
//! `GetWatchlist::Service` trait and is bound onto a `wirespec_aeron` server.

use wirespec_aeron::server::{block_on, AeronRpcServer};

use crate::gen::model::watchlist::Watchlist;
use crate::gen::rpc::get_watchlist::GetWatchlist;

/// The stream this client serves its own rpcs on; the backend calls it there.
pub const WATCHLIST_STREAM_ID: i32 = 3001;

pub struct TraderTerminal;

impl GetWatchlist::Service for TraderTerminal {
    async fn get_watchlist() -> Watchlist {
        Watchlist {
            symbols: vec!["AAPL".into(), "FLCK".into()],
        }
    }
}

pub fn watchlist_server() -> AeronRpcServer {
    AeronRpcServer::new().bind("GetWatchlist", |_params| {
        println!("Serving GetWatchlist for the backend");
        let watchlist = block_on(<TraderTerminal as GetWatchlist::Service>::get_watchlist());
        serde_json::to_vec(&watchlist).map_err(|e| e.to_string().into_bytes())
    })
}
