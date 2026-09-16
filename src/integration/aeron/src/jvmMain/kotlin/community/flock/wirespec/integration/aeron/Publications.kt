package community.flock.wirespec.integration.aeron

import io.aeron.Publication
import org.agrona.DirectBuffer
import org.agrona.concurrent.BackoffIdleStrategy
import java.util.concurrent.TimeUnit

/**
 * Offer, retrying through back pressure and the window in which a freshly added
 * publication is not yet connected. Throws when the publication is closed, has
 * reached max position, or [timeoutNs] passes without a successful offer.
 */
internal fun Publication.offerFully(buffer: DirectBuffer, timeoutNs: Long = TimeUnit.SECONDS.toNanos(10)) {
    val idle = BackoffIdleStrategy()
    val deadline = System.nanoTime() + timeoutNs
    while (true) {
        val position = offer(buffer)
        when {
            position >= 0 -> return
            position == Publication.CLOSED -> error("Aeron publication closed")
            position == Publication.MAX_POSITION_EXCEEDED -> error("Aeron publication reached max position")
            System.nanoTime() >= deadline -> error("Timed out offering to Aeron publication (result $position)")
            else -> idle.idle()
        }
    }
}
