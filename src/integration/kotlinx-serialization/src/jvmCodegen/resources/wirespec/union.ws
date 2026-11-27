@KotlinxSerializationDiscriminator("kind")
type Animal = Cat | Bird

type Cat {
    kind: "cat",
    lives: Integer
}

type Bird {
    kind: "bird",
    name: String
}

type Owner {
    name: String,
    animal: Animal
}

type Account = Password | Token

type Password {
    password: String
}

type Token {
    token: String
}
