import org.example.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueTests {

    @org.junit.jupiter.api.Test
    fun `a single consumer and producer simple test`() {
        val queue = Queue<Int>(1)
        var result : Int = 0

        val tconsumer = thread {
            result = queue.get()
        }

        val tproducer = thread {
            Thread.sleep(1000)
            queue.put(2)
        }

        tproducer.join(3000)
        tconsumer.join(3000)

        assertEquals(2, result)
    }

    @org.junit.jupiter.api.Test
    fun `multiple use with a single consumer and producer test`() {
        val queue = Queue<Int>(10)
        val consumedValues = mutableSetOf<Int>()
        val NVALUES = 100_000

        val tconsumer = Thread {
            while(true) {
                val res = queue.get()
                if (res < 0) break
                consumedValues.add(res)
            }
        }
        tconsumer.start()

        val tproducer = Thread {
            repeat(NVALUES) {
                queue.put(it)
            }
            queue.put(-1)
        }
        tproducer.start()

        tproducer.join(3000)
        tconsumer.join(3000)

        assertEquals(NVALUES, consumedValues.size)
    }

    @Test
    fun `multiple producers and one consumer test`(){
        val queue = Queue<Int>(10)
        val consumedValues = ConcurrentLinkedQueue<Int>()
        val NVALUES = 100_000
        val n = 2
        val count = AtomicInteger(1)

        val tconsumer = thread {
            while(true) {
                val res = queue.get()
                if (res < 0) break
                consumedValues.add(res)
            }
        }

        val tproducer = (1..n).map {
            thread {
                repeat(NVALUES) {
                    queue.put(count.getAndIncrement())
                }
            }
        }

        tproducer.forEach { it.join() }
        queue.put(-1)
        tconsumer.join()

        assertEquals((NVALUES * n), consumedValues.size)
    }

    @Test
    fun `multiple consumers and one producer test`(){
        val queue = Queue<Int>(10)
        val consumedValues = ConcurrentLinkedQueue<Int>()
        val NVALUES = 100_000
        val nConsumers = 2
        //val count = AtomicInteger(1)

        val tconsumer = (1..nConsumers).map {
            thread {
                while(true) {
                    val res = queue.get()
//                    println("get $res")
                    if (res < 0) break
                    consumedValues.add(res)
                }
            }
        }

        val tproducer = thread {
            repeat(NVALUES) {
                queue.put(it)
//                println("put $it")
            }
            repeat(nConsumers){
                queue.put(-1)
            }
        }

        tconsumer.forEach { it.join() }
        tproducer.join()

        assertEquals(NVALUES, consumedValues.size)
    }

    @Test
    fun `multiple consumers and multiple producers test`(){
        val queue = Queue<Int>(10)
        val consumedValues = ConcurrentLinkedQueue<Int>()
        val NVALUES = 100_000
        val nConsumers = 2
        val nProducers = 2
        val count = AtomicInteger(1)


        val tconsumer = (1..nConsumers).map {
            Thread {
                while(true) {
                    val res = queue.get()
                    //println("get $res")
                    if (res < 0) break
                    consumedValues.add(res)
                }
            }
        }
        tconsumer.forEach { it.start() }

        val tproducer = (1 .. nProducers).map {
            Thread {
                repeat(NVALUES){
                    queue.put(count.getAndIncrement())
                    //println("put ${count.get()}")
                }
            }
        }
        tproducer.forEach { it.start() }

        tproducer.forEach { it.join() }
        repeat(nConsumers){
            queue.put(-1)
            //println("put -1")
        }
        tconsumer.forEach { it.join() }

        assertEquals((NVALUES * nProducers), consumedValues.size)
        assertEquals((NVALUES * nProducers), (count.get() - 1))     // -1 porque o count é incrementado cada vez que é lido

    }
}