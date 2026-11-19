@file:GraphQLQuery(path: { notifications: "NotificationsQuery!" })
@file:GraphQLMutation(path: { notifications: "NotificationsMutation!" })

type Notification {
  id: String,
  text: String
}

@GraphQLQuery
rpc Inbox {
  limit: Integer32,
  locale: String?
} -> Notification[]

@GraphQLQuery
rpc InboxCount {} -> Integer32

@GraphQLMutation
rpc MarkRead {
  id: String
} -> Unit
