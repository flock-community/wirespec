package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberId
import community.flock.wirespec.examples.spring.generated.model.MemberInput
import community.flock.wirespec.examples.spring.repository.MemberRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MemberService(private val repository: MemberRepository) {

    suspend fun list(): List<Member> = repository.findAll()

    suspend fun get(id: MemberId): Member? = repository.findById(id)

    suspend fun create(input: MemberInput): Member = repository.save(
        Member(
            id = MemberId(UUID.randomUUID().toString()),
            ref = input.ref,
            name = input.name,
            email = input.email,
        ),
    )
}
