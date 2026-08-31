//! The Wirespec-over-Aeron RPC wire protocol, mirroring the Kotlin codec in
//! `src/integration/aeron` (RpcFrame.kt). Version 1, all integers little-endian:
//!
//! ```text
//! u8   protocol version (1)
//! u8   frame kind (1 = REQUEST, 2 = RESULT, 3 = ERROR)
//! i64  correlation id
//! u16  method length, followed by that many bytes of UTF-8 method name
//! -- REQUEST only --
//! u16  reply channel length, followed by that many bytes of UTF-8 Aeron channel URI
//! i32  reply stream id
//! ------------------
//! u32  payload length, followed by that many bytes of payload
//! ```

pub const VERSION: u8 = 1;
pub const KIND_REQUEST: u8 = 1;
pub const KIND_RESULT: u8 = 2;
pub const KIND_ERROR: u8 = 3;

pub const DEFAULT_CHANNEL: &str = "aeron:ipc";
pub const DEFAULT_REQUEST_STREAM_ID: i32 = 1001;
pub const DEFAULT_REPLY_STREAM_ID: i32 = 1002;

#[derive(Debug, Clone, PartialEq)]
pub enum RpcFrame {
    Request {
        correlation_id: i64,
        method: String,
        reply_channel: String,
        reply_stream_id: i32,
        payload: Vec<u8>,
    },
    Result {
        correlation_id: i64,
        method: String,
        payload: Vec<u8>,
    },
    Error {
        correlation_id: i64,
        method: String,
        payload: Vec<u8>,
    },
}

impl RpcFrame {
    pub fn correlation_id(&self) -> i64 {
        match self {
            RpcFrame::Request { correlation_id, .. }
            | RpcFrame::Result { correlation_id, .. }
            | RpcFrame::Error { correlation_id, .. } => *correlation_id,
        }
    }

    pub fn encode(&self) -> Vec<u8> {
        let (kind, correlation_id, method, payload) = match self {
            RpcFrame::Request { correlation_id, method, payload, .. } => (KIND_REQUEST, correlation_id, method, payload),
            RpcFrame::Result { correlation_id, method, payload } => (KIND_RESULT, correlation_id, method, payload),
            RpcFrame::Error { correlation_id, method, payload } => (KIND_ERROR, correlation_id, method, payload),
        };
        let mut bytes = vec![VERSION, kind];
        bytes.extend_from_slice(&correlation_id.to_le_bytes());
        bytes.extend_from_slice(&(method.len() as u16).to_le_bytes());
        bytes.extend_from_slice(method.as_bytes());
        if let RpcFrame::Request { reply_channel, reply_stream_id, .. } = self {
            bytes.extend_from_slice(&(reply_channel.len() as u16).to_le_bytes());
            bytes.extend_from_slice(reply_channel.as_bytes());
            bytes.extend_from_slice(&reply_stream_id.to_le_bytes());
        }
        bytes.extend_from_slice(&(payload.len() as u32).to_le_bytes());
        bytes.extend_from_slice(payload);
        bytes
    }

    pub fn decode(bytes: &[u8]) -> Result<RpcFrame, String> {
        let mut reader = Reader { bytes, position: 0 };
        let version = reader.u8()?;
        if version != VERSION {
            return Err(format!("Unsupported Wirespec Aeron protocol version: {version}"));
        }
        let kind = reader.u8()?;
        let correlation_id = reader.i64()?;
        let method = reader.utf8()?;
        match kind {
            KIND_REQUEST => Ok(RpcFrame::Request {
                correlation_id,
                method,
                reply_channel: reader.utf8()?,
                reply_stream_id: reader.i32()?,
                payload: reader.payload()?,
            }),
            KIND_RESULT => Ok(RpcFrame::Result { correlation_id, method, payload: reader.payload()? }),
            KIND_ERROR => Ok(RpcFrame::Error { correlation_id, method, payload: reader.payload()? }),
            _ => Err(format!("Unknown Wirespec Aeron frame kind: {kind}")),
        }
    }
}

struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> Reader<'a> {
    fn take(&mut self, count: usize) -> Result<&'a [u8], String> {
        let end = self.position + count;
        let slice = self.bytes.get(self.position..end).ok_or("Truncated Wirespec Aeron frame")?;
        self.position = end;
        Ok(slice)
    }

    fn u8(&mut self) -> Result<u8, String> {
        Ok(self.take(1)?[0])
    }

    fn i32(&mut self) -> Result<i32, String> {
        Ok(i32::from_le_bytes(self.take(4)?.try_into().unwrap()))
    }

    fn i64(&mut self) -> Result<i64, String> {
        Ok(i64::from_le_bytes(self.take(8)?.try_into().unwrap()))
    }

    fn utf8(&mut self) -> Result<String, String> {
        let length = u16::from_le_bytes(self.take(2)?.try_into().unwrap()) as usize;
        String::from_utf8(self.take(length)?.to_vec()).map_err(|e| e.to_string())
    }

    fn payload(&mut self) -> Result<Vec<u8>, String> {
        let length = u32::from_le_bytes(self.take(4)?.try_into().unwrap()) as usize;
        Ok(self.take(length)?.to_vec())
    }
}
