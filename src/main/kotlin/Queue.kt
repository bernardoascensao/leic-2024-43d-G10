package org.example

import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Queue<T>(private val capacity : Int) {
    private val list = LinkedList<T>()
    private val mutex = ReentrantLock()
    private val canGet = Semaphore(0)
    private val spaceAvaiable = Semaphore(capacity)

    fun put(elem : T)  {
        spaceAvaiable.acquire()
        mutex.withLock {
            list.add(elem)
        }
        canGet.release()
    }


    fun get() : T {
        canGet.acquire()
        val elem =  mutex.withLock {
            list.removeFirst()
        }.also {
            spaceAvaiable.release()
        }
        return elem
    }
}