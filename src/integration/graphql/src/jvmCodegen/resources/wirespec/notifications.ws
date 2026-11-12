type NotificationsQuery {}

type NotificationsMutation {}

type Notification {
  id: String,
  text: String
}

@GraphQLQuery
rpc Notifications {} -> NotificationsQuery

@GraphQLQuery("NotificationsQuery")
rpc Inbox {
  limit: Integer32,
  locale: String?
} -> Notification[]

@GraphQLQuery("NotificationsQuery")
rpc InboxCount {} -> Integer32

@GraphQLQuery("NotificationsQuery")
rpc Latest {
  @GraphQLDefault("2")
  size: Integer32
} -> Notification[]

@GraphQLMutation
@GraphQLName("notifications")
rpc MutateNotifications {} -> NotificationsMutation

@GraphQLMutation("NotificationsMutation")
rpc MarkRead {
  id: String
} -> Unit
