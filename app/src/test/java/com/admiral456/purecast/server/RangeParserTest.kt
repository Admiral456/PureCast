package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeParserTest {
    @Test
    fun parsesClosedRange() {
        assertEquals(ByteRange(100, 199), RangeParser.parse("bytes=100-199", 1_000))
    }

    @Test
    fun parsesOpenEndedRange() {
        assertEquals(ByteRange(900, 999), RangeParser.parse("bytes=900-", 1_000))
    }

    @Test
    fun parsesSuffixRange() {
        assertEquals(ByteRange(900, 999), RangeParser.parse("bytes=-100", 1_000))
    }

    @Test
    fun clipsRangeAtEndOfFile() {
        assertEquals(ByteRange(950, 999), RangeParser.parse("bytes=950-1200", 1_000))
    }

    @Test
    fun rejectsRangeStartingAfterEndOfFile() {
        assertNull(RangeParser.parse("bytes=1000-", 1_000))
    }

    @Test
    fun rejectsMultipleRanges() {
        assertNull(RangeParser.parse("bytes=0-1,10-11", 100))
    }
}
