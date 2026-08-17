package com.ucsc.codescribe

import android.app.Application

class CodeScribeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
