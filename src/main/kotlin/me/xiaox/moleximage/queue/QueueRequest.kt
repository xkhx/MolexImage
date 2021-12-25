package me.xiaox.moleximage.queue

abstract class QueueRequest<E> {

    abstract val index: Int

    abstract suspend fun onComplete(result: Result<E>)

}