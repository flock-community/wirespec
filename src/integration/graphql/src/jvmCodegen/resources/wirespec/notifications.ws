@file:GraphQLQuery(path: { notifications: "NotificationsQuery!" })
@file:GraphQLMutation(path: { notifications: "NotificationsMutation!" })

type Notification {
  id: String,
  text: String,
  related: RelatedNotifications
}

rpc RelatedNotifications {
  first: Integer32? = 10
} -> Notification[]

@GraphQLQuery("NotificationsQuery")
rpc Inbox {
  limit: Integer32,
  locale: String?
} -> Notification[]

@GraphQLQuery("NotificationsQuery")
rpc InboxCount {} -> Integer32

@GraphQLMutation("NotificationsMutation")
rpc MarkRead {
  id: String
} -> Unit
