type Notification {
  id: String,
  text: String
}

@GraphQLQuery(path: { notifications: "NotificationsQuery!" })
rpc Inbox {
  limit: Integer32,
  locale: String?
} -> Notification[]

@GraphQLQuery(path: { notifications: "NotificationsQuery!" })
rpc InboxCount {} -> Integer32

@GraphQLMutation(path: { notifications: "NotificationsMutation!" })
rpc MarkRead {
  id: String
} -> Unit
