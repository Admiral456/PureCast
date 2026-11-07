package com.admiral456.purecast.dlna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpnpTimeTest {
    @Test
    fun parsesWholeSecondTime() {
        assertEquals(3_723L, UpnpTime.parseSeconds("01:02:03"))
    }

    @Test
    fun parsesFractionalSeconds() {
        assertEquals(12L, UpnpTime.parseSeconds("00:00:12.850"))
    }

    @Test
    fun rejectsUnimplementedTime() {
        assertNull(UpnpTime.parseSeconds("NOT_IMPLEMENTED"))
    }

    @Test
    fun rejectsInvalidMinuteAndSecondValues() {
        assertNull(UpnpTime.parseSeconds("00:61:00"))
        assertNull(UpnpTime.parseSeconds("00:00:60"))
    }

    @Test
    fun formatsTimeForDlnaSeek() {
        assertEquals("01:02:03", UpnpTime.formatSeconds(3_723L))
    }

    @Test
    fun clampsNegativeTimesToZero() {
        assertEquals("00:00:00", UpnpTime.formatSeconds(-5L))
    }
}
