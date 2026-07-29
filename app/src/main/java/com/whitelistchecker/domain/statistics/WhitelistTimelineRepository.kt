package com.whitelistchecker.domain.statistics

interface WhitelistTimelineRepository {

    suspend fun saveSample(sample: WhitelistTimelineSample)

    suspend fun getSamplesSince(cutoffMillis: Long): List<WhitelistTimelineSample>

    suspend fun countSamples(): Int

    suspend fun replaceAll(samples: List<WhitelistTimelineSample>)

    suspend fun clear()

    suspend fun applyRetentionPolicy(nowMillis: Long): Int
}
