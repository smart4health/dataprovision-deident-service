package com.healthmetrix.deident.statistics.api

data class StatsResponse(
    val user: UserStats,
    val global: GlobalStats,
) {
    data class GlobalStats(
        val usersCount: Int,
        val resourcesUploadedCount: Int,
    )

    data class UserStats(
        val resourcesUploadedCount: Int,
    )
}
