//! Wirespec rpc over Aeron — the Rust counterpart of the `aeron-jvm`
//! integration. Speaks the same wire protocol (see [protocol]): [client]
//! calls rpcs a remote peer serves, [server] serves this process's own rpcs.

pub mod client;
pub mod protocol;
pub mod server;
