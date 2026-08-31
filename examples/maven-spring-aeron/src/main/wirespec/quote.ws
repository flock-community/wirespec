type Quote {
  symbol: String,
  price: Number,
  currency: String
}

type QuoteError {
  code: String,
  message: String
}

rpc GetQuote {
  symbol: String
} -> Quote ! QuoteError

rpc Ping {} -> String
