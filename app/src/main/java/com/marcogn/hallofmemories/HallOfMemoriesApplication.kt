package com.marcogn.hallofmemories

import android.app.Application
import com.marcogn.hallofmemories.data.update.ApkDownloader
import com.marcogn.hallofmemories.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class HallOfMemoriesApplication : Application() {

    @Inject lateinit var apkDownloader: ApkDownloader
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Phase 7: update APKs from a previous process (installed, refused or interrupted).
        applicationScope.launch { apkDownloader.deleteLeftovers() }
    }
}
