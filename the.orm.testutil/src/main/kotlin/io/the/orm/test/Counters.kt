package io.the.orm.test

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.measureTime

class Timer {
    fun add(time: Duration) {
        durations += time
    }

    suspend fun add(block: suspend () -> Unit) {
        add(measureTime {
            block()
        })
    }

    private fun totalTime() = durations.fold(Duration.ZERO) { acc, duration -> acc.plus(duration) }
    private val durations = CopyOnWriteArrayList<Duration>()
    override fun toString(): String = "total: ${totalTime()} entries:${durations.size} (durations: $durations)"
}

object Counters {
    val createSchema = Timer()
    val createDatabase = Timer()
    override fun toString(): String {
        return "Counters(createSchema=$createSchema, createDatabase=$createDatabase)"
    }
}
