package io.github.a13e300.intenttracker.service

class IntentTrackerService {
    companion object {
        const val FLAG_LISTEN_START_ACTIVITY = 1 shl 0
        const val FLAG_GET_ORIGINAL_PARCELABLE = 1 shl 1
        const val FLAG_GET_STACK_TRACE = 1 shl 2

        const val CURRENT_VERSION = 1

        const val ACTION_REQUIRE_SERVICE = "io.github.a13e300.intenttracker.REQUIRE_SERVICE"
        const val KEY_SERVICE_FETCHER = "service_fetcher"
    }
}