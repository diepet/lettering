package io.github.diepet.lettering

import android.content.Context
import android.app.Application
import io.github.diepet.lettering.styles.AvailableStyles

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        /* Initialize text styles */
        AvailableStyles.init(applicationContext)
    }
}

// AndroidX PreferenceManager used this private file in earlier releases.
internal val Context.letteringPreferences
    get() = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
