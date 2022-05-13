package com.neeva.app.cookiecutter

import com.neeva.app.browsing.getBrowserIfAlive
import kotlinx.coroutines.flow.MutableStateFlow
import org.chromium.weblayer.Tab

class TrackingData(
    val stats: Map<String, Int>?
) {
    val numTrackers: Int = stats?.values?.sum() ?: 0
    val numDomains: Int = stats?.keys?.count() ?: 0
    val trackingEntities: HashMap<TrackingEntity, Int> = HashMap<TrackingEntity, Int>()
}

class CookieCutterTabModel(
    val tab: Tab,
    val trackingDataFlow: MutableStateFlow<TrackingData?>
) {
    // TODO: listen to enableTrackingProtection in CookieCutterModel and refresh tab
    private var stats: Map<String, Int>? = null
        set(value) {
            field = value
            if (tab.getBrowserIfAlive()?.activeTab == tab) {
                trackingDataFlow.value = TrackingData(stats)
            }
        }

    fun whoIsTrackingYouHosts(trackingData: TrackingData): Map<TrackingEntity, Int> {
        return trackingData.trackingEntities
            .toList()
            .sortedBy { (_, value) -> value }
            .take(3)
            .toMap()
    }

    fun currentTrackingData(): TrackingData? {
        return TrackingData(stats)
    }

    fun resetStat() {
        stats = null
    }

    fun updateStats(stats: Map<String, Int>?) {
        this.stats = stats
    }
}
