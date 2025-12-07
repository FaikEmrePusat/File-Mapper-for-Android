package com.example.filemapper.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Utility object for handling storage permissions.
 * Supports both legacy (READ_EXTERNAL_STORAGE) and modern (MANAGE_EXTERNAL_STORAGE) permissions.
 */
object PermissionHelper {
    
    /**
     * Check if storage permission is granted.
     * On Android 11+, checks for MANAGE_EXTERNAL_STORAGE.
     * On Android 10 and below, checks for READ_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE for broad file access
            Environment.isExternalStorageManager()
        } else {
            // Android 10 and below use READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get the required permission string for the current Android version.
     * Returns null if MANAGE_EXTERNAL_STORAGE is needed (requires Settings intent instead).
     */
    fun getRequiredPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // MANAGE_EXTERNAL_STORAGE requires a Settings intent, not runtime permission
            null
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    /**
     * Create an intent to open the "All Files Access" settings page.
     * Only applicable for Android 11+.
     */
    fun createManageStorageIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        } else {
            null
        }
    }
    
    /**
     * Get list of permissions to request at runtime.
     * Returns empty list if Settings intent is required instead.
     */
    fun getPermissionsToRequest(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses Settings intent for MANAGE_EXTERNAL_STORAGE
            emptyList()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 use runtime permission
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 5 and below don't need runtime permission
            emptyList()
        }
    }
    
    /**
     * Check if we should show rationale for storage permission.
     */
    fun shouldShowRationale(): Boolean {
        // This would typically be checked in an Activity context
        // Placeholder for future implementation
        return false
    }
}
