package com.ibrah.abonelik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppIconTest {
    @Test fun icon_key_round_trip() {
        assertEquals("app:com.netflix.mediaclient", IconKey.forApp("com.netflix.mediaclient"))
        assertEquals("com.netflix.mediaclient", IconKey.appPackage("app:com.netflix.mediaclient"))
        assertNull(IconKey.appPackage("music"))
        assertNull(IconKey.appPackage(""))
    }

    private val apps = listOf(
        AppEntry("com.netflix.mediaclient", "Netflix"),
        AppEntry("com.spotify.music", "Spotify"),
        AppEntry("com.google.android.youtube", "YouTube"),
        AppEntry("com.google.android.apps.youtube.music", "YouTube Music"),
        AppEntry("tr.com.turkcell.dijital", "TV+"),
    )

    @Test fun best_match_by_name() {
        assertEquals("com.netflix.mediaclient", AppMatch.best("netflix", apps)?.pkg)
        assertEquals("com.netflix.mediaclient", AppMatch.best("Netflix Standart", apps)?.pkg)
        assertEquals("com.google.android.youtube", AppMatch.best("YouTube Premium", apps)?.pkg)
        assertEquals("com.google.android.apps.youtube.music", AppMatch.best("YouTube Music", apps)?.pkg)
        assertEquals("com.spotify.music", AppMatch.best("SPOTİFY", apps)?.pkg) // Türkçe büyük İ
    }

    @Test fun no_match_for_short_or_unknown() {
        assertNull(AppMatch.best("", apps))
        assertNull(AppMatch.best("Spor salonu", apps))
        assertNull(AppMatch.best("TV", apps)) // çok kısa/belirsiz isimde öneri yok
    }
}
