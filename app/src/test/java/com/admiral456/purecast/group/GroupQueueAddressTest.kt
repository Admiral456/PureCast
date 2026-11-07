package com.admiral456.purecast.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GroupQueueAddressTest {
    @Test
    fun normalizesBareLocalAddressAndHttpUrl() {
        assertEquals("http://192.168.1.20:49152", GroupQueueAddress.normalize("192.168.1.20:49152"))
        assertEquals("http://10.0.0.12:8080", GroupQueueAddress.normalize("http://10.0.0.12:8080/"))
    }

    @Test
    fun rejectsPublicAndLoopbackAddresses() {
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("8.8.8.8:49152")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("127.0.0.1:49152")
        }
    }

    @Test
    fun rejectsUnsupportedSchemesAndPaths() {
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("https://192.168.1.20:49152")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("192.168.1.20:49152/admin")
        }
    }

    @Test
    fun rejectsMissingAndInvalidPorts() {
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("192.168.1.20")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GroupQueueAddress.normalize("192.168.1.20:70000")
        }
    }
}
