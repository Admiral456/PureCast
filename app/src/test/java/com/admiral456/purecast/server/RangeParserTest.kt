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
    fun suffixLongerThanFileReturnsEntireFile() {
        assertEquals(ByteRange(0, 99), RangeParser.parse("bytes=-500", 100))
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

    @Test
    fun rejectsEmptyRange() {
        assertNull(RangeParser.parse("bytes=-", 100))
    }

    @Test
    fun rejectsUnsupportedRangeUnit() {
        assertNull(RangeParser.parse("items=0-10", 100))
    }

    @Test
    fun rejectsZeroLengthResource() {
        assertNull(RangeParser.parse("bytes=0-", 0))
    }
    @Test
    fun parsesCaseInsensitiveRangeUnit() {
        assertEquals(ByteRange(10, 19), RangeParser.parse("BYTES=10-19", 100))
    }

    @Test
    fun rejectsNumbersOutsideLongRange() {
        assertNull(RangeParser.parse("bytes=9223372036854775808-", 100))
        assertNull(RangeParser.parse("bytes=-9223372036854775808", 100))
    }

    @Test
    fun maximumValidRangeLengthDoesNotOverflow() {
        assertEquals(
            Long.MAX_VALUE,
            RangeParser.parse("bytes=0-${Long.MAX_VALUE}", Long.MAX_VALUE)?.length
        )
    }

}
