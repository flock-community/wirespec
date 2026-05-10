import { kotestWirespecGenerator } from '@flock/wirespec/generator';
import { MeetupGenerator } from './gen/generator/MeetupGenerator';
import { AttendeeGenerator } from './gen/generator/AttendeeGenerator';

// 1. Default usage — preinstalled `email` and `ipAddress` arbs apply.
const gen = kotestWirespecGenerator(42);
const meetup = MeetupGenerator.generate(gen, []);
console.log('Meetup (seed=42):\n', JSON.stringify(meetup, null, 2));

const attendeeSeed = AttendeeGenerator.generate(gen, [meetup.attendees[0].id]);
console.log('Attendee id:\n', meetup.attendees[0].id);
console.log('Attendee name:\n', meetup.attendees[0].name, attendeeSeed.name);
console.log('Attendee (seed=42):\n', JSON.stringify(attendeeSeed, null, 2));

// 2. Determinism — same seed produces identical output.
const replay = MeetupGenerator.generate(kotestWirespecGenerator(42), []);
console.log(
    'Deterministic replay matches:',
    JSON.stringify(meetup) === JSON.stringify(replay),
);

// 3. Custom registration — override @Generator("email") with a domain-specific
//    factory. Names are case-insensitive.
const customGen = kotestWirespecGenerator(7, {
    email: (s) => `demo+${s}@example.com`,
});
const attendee = AttendeeGenerator.generate(customGen, []);
console.log('Attendee with custom email factory:\n', JSON.stringify(attendee, null, 2));
