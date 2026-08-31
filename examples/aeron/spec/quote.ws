type Quote {
  symbol: String,
  price: Number,
  currency: String
}

type QuoteError {
  code: String,
  message: String
}

type Watchlist {
  symbols: String[]
}

type QuoteList {
  quotes: Quote[]
}

rpc GetQuote {
  symbol: String
} -> Quote ! QuoteError

rpc Ping {} -> String

rpc GetWatchlist {} -> Watchlist

rpc GetWatchlistQuotes {} -> QuoteList
