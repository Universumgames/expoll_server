package net.mt32.expoll.helper

fun minmax(num: Int, min: Int, max: Int): Int{
    if(num < min) return min
    if(num > max) return max
    return num
}