package com.example.trnberechnung.ui

/** Pure state used by the last onboarding page and covered by unit tests. */
data class OnboardingState(
    val page: Int = 0,
    val disclaimerAccepted: Boolean = false
) {
    val canFinish: Boolean get() = page == LAST_PAGE && disclaimerAccepted

    companion object {
        const val LAST_PAGE = 2
    }
}
