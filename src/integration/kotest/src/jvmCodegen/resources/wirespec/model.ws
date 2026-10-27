type Tag = String(/^[a-z]+$/g)

type Token {
    iss: String
}

type Marker {}

type Session {
    token: Token,
    previous: Token?,
    marker: Marker,
    tag: Tag?,
    tags: Tag[],
    tagsByKey: {Tag}
}

type Node {
    name: String,
    children: Node[]
}

endpoint CreateSession POST Session /sessions -> {
    201 -> Session
}

endpoint ImportSessions POST Session[] /sessions/import -> {
    200 -> Session[]
}

channel SessionEvents -> Session
