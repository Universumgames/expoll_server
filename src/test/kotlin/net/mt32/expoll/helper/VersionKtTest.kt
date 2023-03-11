package net.mt32.expoll.helper

import kotlin.test.Test
import kotlin.test.assertFalse

class VersionKtTest{
    @Test
    fun testExactVersionMatch() {
        val mismatchSameLength = checkVersionExact("2.6.0", "2.5.8")
        val mismatchDiffLength = checkVersionExact("2.6", "3.7.8")
        val matchW1Build = checkVersionExact("2.6.0build6", "2.6.0")
        val matchW2Build = checkVersionExact("2.6.0build35", "2.6.0build35")
        assertFalse(mismatchSameLength)
        assertFalse(mismatchDiffLength)
        assertFalse(matchW1Build)
        assert(matchW2Build)
    }

    @Test
    fun testExactVersionMatchIgnoreBuild() {
        val mismatchSameLength = checkVersionExactIgnoreBuild("2.6.0", "2.5.8")
        val mismatchDiffLength = checkVersionExactIgnoreBuild("2.6", "3.7.8")
        val matchW1Build = checkVersionExactIgnoreBuild("2.6.0build6", "2.6.0")
        val matchW2Build = checkVersionExactIgnoreBuild("2.6.0build35", "2.6.0build35")
        assertFalse(mismatchSameLength)
        assertFalse(mismatchDiffLength)
        assert(matchW1Build)
        assert(matchW2Build)
    }

    @Test
    fun testClosedRangeVersionMatch(){
        val mismatchSameLength = checkVersionClosedRange("2.6.0", "2.5.8", "2.5.9")
        val mismatchDiffLength = checkVersionClosedRange("2.6", "3.7.8", "4.0")
        val matchW1Build = checkVersionClosedRange("2.6.0build6", "2.6.0", "2.7.0")
        val matchW2Build = checkVersionClosedRange("2.6.0build35", "2.6.0build35", "2.7build35")
        assertFalse(mismatchSameLength)
        assertFalse(mismatchDiffLength)
        assert(matchW1Build)
        assert(matchW2Build)
    }

    @Test
    fun testOpenRangeVersionMatch(){
        val mismatchSameLength = checkVersionOpenRange("2.6.0", to = "2.5.9")
        val mismatchDiffLength = checkVersionOpenRange("2.6", "3.7.8")
        val matchW1Build = checkVersionOpenRange("2.6.0build6", "2.6.0")
        val matchW2Build = checkVersionOpenRange("2.6.0build35", "2.6.0build35", "2.7build35")
        assertFalse(mismatchSameLength)
        assertFalse(mismatchDiffLength)
        assert(matchW1Build)
        assert(matchW2Build)
    }
}