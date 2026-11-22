package io.github.diepet.lettering

import android.app.Application
import io.github.diepet.lettering.styles.AvailableStyles

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        /* Initialize text styles */
        AvailableStyles.init(applicationContext)
    }
}
