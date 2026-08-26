type StockLevel {
    sku: String,
    available: Integer,
    warehouse: String
}

type StockError {
    reason: String
}

endpoint GetStock GET /stock/{sku: String} -> {
    200 -> StockLevel
    404 -> StockError
}
