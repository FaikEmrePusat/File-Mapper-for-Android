package com.example.filemapper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with @HiltAndroidApp.
 * This is required for Hilt to work and must be registered in AndroidManifest.xml.
 */
@HiltAndroidApp
class FileMapperApplication : Application()
