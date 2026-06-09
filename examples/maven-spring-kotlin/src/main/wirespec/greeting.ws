type Greeting {
  message: String
}

endpoint GetGreeting GET /greeting -> {
  200 -> Greeting
}
