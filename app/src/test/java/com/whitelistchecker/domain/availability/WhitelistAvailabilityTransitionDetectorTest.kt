package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityTransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistAvailabilityTransitionDetectorTest {

    private val detector = WhitelistAvailabilityTransitionDetector()

    @Test
    fun `unavailable to available is became available`() {
        val transition = detector.detect(
            WhitelistAvailabilityState.UNAVAILABLE,
            WhitelistAvailabilityState.AVAILABLE,
        )
        assertEquals(WhitelistAvailabilityTransitionType.BECAME_AVAILABLE, transition)
        assertTrue(detector.isSignificantTransition(transition))
    }

    @Test
    fun `available to unavailable is became unavailable`() {
        val transition = detector.detect(
            WhitelistAvailabilityState.AVAILABLE,
            WhitelistAvailabilityState.UNAVAILABLE,
        )
        assertEquals(WhitelistAvailabilityTransitionType.BECAME_UNAVAILABLE, transition)
        assertTrue(detector.isSignificantTransition(transition))
    }

    @Test
    fun `available to available is not significant`() {
        val transition = detector.detect(
            WhitelistAvailabilityState.AVAILABLE,
            WhitelistAvailabilityState.AVAILABLE,
        )
        assertEquals(WhitelistAvailabilityTransitionType.STAYED_AVAILABLE, transition)
        assertFalse(detector.isSignificantTransition(transition))
    }

    @Test
    fun `unknown to available is significant`() {
        val transition = detector.detect(
            WhitelistAvailabilityState.UNKNOWN,
            WhitelistAvailabilityState.AVAILABLE,
        )
        assertEquals(WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE, transition)
        assertTrue(detector.isSignificantTransition(transition))
    }

    @Test
    fun `error state from dns does not become unavailable`() {
        val fromStatus = WhitelistAvailabilityStateMapper.fromCheckTargetStatus(
            com.whitelistchecker.domain.model.history.CheckTargetResultStatus.DNS_ERROR,
        )
        assertEquals(WhitelistAvailabilityState.ERROR, fromStatus)
        val transition = detector.detect(WhitelistAvailabilityState.AVAILABLE, fromStatus)
        assertEquals(WhitelistAvailabilityTransitionType.ERROR_STATE, transition)
        assertFalse(detector.isSignificantTransition(transition))
    }
}
