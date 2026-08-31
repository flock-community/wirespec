use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc};
use std::time::Duration;

use wirespec_aeron::client::AeronRpcClient;
use wirespec_aeron_client::gen::rpc::get_quote::GetQuote;
use wirespec_aeron_client::rpc::{empty_params, get_quote_params, get_quote_response, get_watchlist_quotes_response, ping_response};
use wirespec_aeron_client::terminal::{watchlist_server, WATCHLIST_STREAM_ID};

const TIMEOUT: Duration = Duration::from_secs(10);

fn main() -> Result<(), String> {
    // The media driver directory of the running Kotlin Spring Boot backend
    // (../server), which defaults to ${java.io.tmpdir}/wirespec-aeron.
    let aeron_dir = std::env::args()
        .nth(1)
        .or_else(|| std::env::var("AERON_DIR").ok())
        .unwrap_or_else(|| std::env::temp_dir().join("wirespec-aeron").to_string_lossy().into_owned());

    println!("Attaching to Aeron media driver at {aeron_dir}");

    // Serve this client's own rpcs (GetWatchlist) so the backend can call back.
    let running = Arc::new(AtomicBool::new(true));
    let (ready, ready_signal) = mpsc::channel();
    let serving = {
        let aeron_dir = aeron_dir.clone();
        let running = running.clone();
        std::thread::spawn(move || watchlist_server().serve(&aeron_dir, WATCHLIST_STREAM_ID, running, ready))
    };
    if ready_signal.recv().is_err() {
        return serving.join().unwrap();
    }

    let mut client = AeronRpcClient::connect(&aeron_dir)?;

    let pong = ping_response(&client.call("Ping", empty_params(), TIMEOUT)?)?;
    println!("Ping -> {pong}");

    for symbol in ["AAPL", "GOOG", "FLCK", "NOPE"] {
        match get_quote_response(&client.call("GetQuote", get_quote_params(symbol), TIMEOUT)?)? {
            GetQuote::Response::Result(result) => {
                let quote = result.value;
                println!("GetQuote {symbol} -> {} {} {}", quote.symbol, quote.price, quote.currency);
            }
            GetQuote::Response::Error(error) => {
                let quote_error = error.value;
                println!("GetQuote {symbol} -> error {}: {}", quote_error.code, quote_error.message);
            }
        }
    }

    // The full loop: this call makes the backend call GetWatchlist on us.
    let quote_list = get_watchlist_quotes_response(&client.call("GetWatchlistQuotes", empty_params(), TIMEOUT)?)?;
    for quote in &quote_list.quotes {
        println!("GetWatchlistQuotes -> {} {} {}", quote.symbol, quote.price, quote.currency);
    }

    running.store(false, Ordering::SeqCst);
    serving.join().unwrap()?;
    Ok(())
}
