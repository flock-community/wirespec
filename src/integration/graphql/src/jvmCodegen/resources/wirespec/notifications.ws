@file:GraphQLQuery("Query", path: { notifications: "NotificationsQuery!" })
@file:GraphQLMutation("Mutation", path: { notifications: "NotificationsMutation!" })

type Notification {
  id: String,
  text: String
}

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
