use std::time::Duration;

use wirespec_aeron_client::client::AeronRpcClient;
use wirespec_aeron_client::gen::rpc::get_quote::GetQuote;
use wirespec_aeron_client::serialization::{empty_params, get_quote_params, get_quote_response, ping_response};

const TIMEOUT: Duration = Duration::from_secs(10);

fn main() -> Result<(), String> {
    // The media driver directory of the running Kotlin Spring Boot backend
    // (../maven-spring-aeron), which defaults to ${java.io.tmpdir}/wirespec-aeron.
    let aeron_dir = std::env::args()
        .nth(1)
        .or_else(|| std::env::var("AERON_DIR").ok())
        .unwrap_or_else(|| std::env::temp_dir().join("wirespec-aeron").to_string_lossy().into_owned());

    println!("Attaching to Aeron media driver at {aeron_dir}");
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

    Ok(())
}
