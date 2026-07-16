type ProductId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)
type CampaignId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)

enum CampaignStatus {
    DRAFT, ACTIVE, ENDED
}

enum CampaignEventType {
    CREATED, ACTIVATED, ENDED
}

type Product {
    id: ProductId,
    sku: String,
    name: String,
    price: Number
}

type ProductInput {
    sku: String,
    name: String,
    price: Number
}

type Campaign {
    id: CampaignId,
    name: String,
    status: CampaignStatus,
    discountPercentage: Integer,
    productIds: ProductId[]
}

type CampaignInput {
    name: String,
    discountPercentage: Integer,
    productIds: ProductId[]
}

type CampaignEvent {
    campaignId: CampaignId,
    eventType: CampaignEventType,
    discountPercentage: Integer
}

type Error {
    code: Integer,
    message: String
}

channel CampaignEvents -> CampaignEvent

endpoint GetProducts GET /products -> {
    200 -> Product[]
}

endpoint GetProduct GET /products/{id: ProductId} -> {
    200 -> Product
    404 -> Error
}

endpoint CreateProduct POST ProductInput /products -> {
    201 -> Product
}

endpoint GetCampaigns GET /campaigns ? {status: CampaignStatus?} -> {
    200 -> Campaign[]
}

endpoint GetCampaign GET /campaigns/{id: CampaignId} -> {
    200 -> Campaign
    404 -> Error
}

endpoint CreateCampaign POST CampaignInput /campaigns -> {
    201 -> Campaign
}

endpoint UpdateCampaign PUT CampaignInput /campaigns/{id: CampaignId} -> {
    200 -> Campaign
    404 -> Error
}

endpoint ActivateCampaign POST /campaigns/{id: CampaignId}/activate -> {
    200 -> Campaign
    404 -> Error
}

endpoint DeleteCampaign DELETE /campaigns/{id: CampaignId} -> {
    204 -> Unit
    404 -> Error
}
