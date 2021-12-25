package me.xiaox.moleximage.queue

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.xiaox.moleximage.MolexImage
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class QueueManager<V, E : QueueRequest<V>> {

    abstract val frequency: Duration

    private val queue = ConcurrentLinkedQueue<E>()
    private val job: Job = MolexImage.launch worker@{
        while (isActive) {
            queue.poll()?.apply { onComplete(runCatching { handle() }.onFailure { it.printStackTrace() }) }
            delay(frequency)
        }
    }

    var index: Int = 0
        private set
    val size: Int
        get() = queue.size

    fun index(): Int = (index + 1).also { index = it }

    operator fun plusAssign(request: E) {
        queue.offer(request)
    }

    abstract suspend fun E.handle(): V

}