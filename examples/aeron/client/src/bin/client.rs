use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc};
use std::time::{Duration, Instant};

use wirespec_aeron::client::AeronRpcClient;
use wirespec_aeron_client::gen::rpc::get_quote::GetQuote;
use wirespec_aeron_client::rpc::{empty_params, get_quote_params, get_quote_response, get_watchlist_quotes_response, ping_response};
use wirespec_aeron_client::terminal::{watchlist_server, WATCHLIST_STREAM_ID};

const TIMEOUT: Duration = Duration::from_secs(10);

fn env_or(name: &str, default: &str) -> String {
    std::env::var(name).unwrap_or_else(|_| default.to_string())
}

fn main() -> Result<(), String> {
    // This client attaches to its own local media driver and talks to the
    // backend (../server) over the network: every channel is an aeron:udp
    // endpoint with a host both sides can resolve.
    let aeron_dir = std::env::args()
        .nth(1)
        .or_else(|| std::env::var("AERON_DIR").ok())
        .unwrap_or_else(|| std::env::temp_dir().join("wirespec-aeron").to_string_lossy().into_owned());
    let request_channel = env_or("REQUEST_CHANNEL", "aeron:udp?endpoint=localhost:40123");
    let reply_channel = env_or("REPLY_CHANNEL", "aeron:udp?endpoint=localhost:40124");
    let serve_channel = env_or("SERVE_CHANNEL", "aeron:udp?endpoint=localhost:40125");

    println!("Attaching to Aeron media driver at {aeron_dir}");

    // Serve this client's own rpcs (GetWatchlist) so the backend can call back.
    let running = Arc::new(AtomicBool::new(true));
    let (ready, ready_signal) = mpsc::channel();
    let serving = {
        let aeron_dir = aeron_dir.clone();
        let serve_channel = serve_channel.clone();
        let running = running.clone();
        std::thread::spawn(move || watchlist_server().serve_channel(&aeron_dir, &serve_channel, WATCHLIST_STREAM_ID, running, ready))
    };
    if ready_signal.recv().is_err() {
        return serving.join().unwrap();
    }

    // The local media driver may still be coming up (e.g. a sidecar container).
    let deadline = Instant::now() + Duration::from_secs(20);
    let mut client = loop {
        match AeronRpcClient::connect_channels(&aeron_dir, &request_channel, 1001, &reply_channel, 1002) {
            Ok(client) => break client,
            Err(error) if Instant::now() >= deadline => return Err(error),
            Err(_) => std::thread::sleep(Duration::from_millis(500)),
        }
    };

    let pong = ping_response(&client.call("Ping", empty_params(), TIMEOUT)?)?;
    println!("Ping -> {pong}");

    for symbol in ["AAPL", "GOOG", "FLCK", "NOPE"] {
        match get_quote_response(&client.call("GetQuote", get_quote_params(symbol)?, TIMEOUT)?)? {
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

    // The full loop: this call makes the backend call GetWatchlist on us. Give
    // it a longer deadline than the backend's own reverse-call timeout, so a
    // failure inside the loop comes back as the backend's typed error instead
    // of a silent timeout here.
    let quote_list = get_watchlist_quotes_response(&client.call("GetWatchlistQuotes", empty_params(), Duration::from_secs(30))?)?;
    for quote in &quote_list.quotes {
        println!("GetWatchlistQuotes -> {} {} {}", quote.symbol, quote.price, quote.currency);
    }

    running.store(false, Ordering::SeqCst);
    serving.join().unwrap()?;
    Ok(())
}
