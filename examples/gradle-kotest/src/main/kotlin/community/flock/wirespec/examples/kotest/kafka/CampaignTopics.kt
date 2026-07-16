package community.flock.wirespec.examples.kotest.kafka

/**
 * The Kafka topic backing the generated `CampaignEvents` channel. The channel scenario
 * tests point their `WirespecChannelContext` at the same name via `defaultTopic`, so a
 * message published by the app is the one the DSL's `listen { expecting { … } }` receives.
 */
const val CAMPAIGN_EVENTS_TOPIC = "campaign-events"
