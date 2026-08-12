package community.flock.wirespec.examples.spring.repository

import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class MemberRepository {

    private val mutex = Mutex()
    private val store = mutableMapOf<MemberId, Member>()

    suspend fun findAll(): List<Member> = mutex.withLock { store.values.toList() }

    suspend fun findById(id: MemberId): Member? = mutex.withLock { store[id] }

    suspend fun save(member: Member): Member = mutex.withLock {
        store[member.id] = member
        member
    }
}
