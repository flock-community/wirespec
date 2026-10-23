type Money {
  amount: Number,
  currency: String
}

type Venue {
  mic: String,
  city: String
}

type Tick {
  at: String,
  price: Money
}

type Quote {
  symbol: String,
  last: Money,
  previousClose: Money?,
  venue: Venue,
  history: Tick[]
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
