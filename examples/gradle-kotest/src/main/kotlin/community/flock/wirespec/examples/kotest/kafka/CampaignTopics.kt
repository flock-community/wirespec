package community.flock.wirespec.examples.kotest.kafka

/**
 * The Kafka topic backing the generated `CampaignEvents` channel. The scenario tests use the same
 * name via the channel extension's `defaultTopic`.
 */
const val CAMPAIGN_EVENTS_TOPIC = "campaign-events"
