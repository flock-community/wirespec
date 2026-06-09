type MeetupId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)
type AttendeeId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)

enum MeetupStatus {
    SCHEDULED, CANCELLED, COMPLETED
}

type Venue {
    name: String,
    address: String,
    capacity: Integer
}

type Attendee {
    @Seed id: AttendeeId,
    name: String,
    @Generator("email") email: String
}

type Meetup {
    @Seed id: MeetupId,
    title: String,
    description: String,
    status: MeetupStatus,
    venue: Venue,
    attendees: Attendee[]
}

type CreateMeetup {
    title: String,
    description: String,
    venue: Venue
}

type MeetupError {
    code: Integer,
    message: String
}

endpoint GetMeetups GET /api/meetups ? {status: MeetupStatus?} -> {
    200 -> Meetup[]
}

endpoint GetMeetupById GET /api/meetups/{id: MeetupId} -> {
    200 -> Meetup
    404 -> MeetupError
}

endpoint PostMeetup POST CreateMeetup /api/meetups -> {
    201 -> Meetup
    400 -> MeetupError
}
