package net.mt32.expoll.helper

import java.util.*
import kotlin.math.pow

private val random = Random()

fun createNonce(): Long{
    val maxValue = Long.MAX_VALUE
    val minValue = 10.0.pow((maxValue.toString().length - 1).toDouble()).toLong()
    return (minValue + (random.nextDouble() * (maxValue - minValue))).toLong()
}