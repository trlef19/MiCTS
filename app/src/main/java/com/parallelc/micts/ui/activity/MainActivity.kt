package com.parallelc.micts.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.parallelc.micts.config.AppConfig.CONFIG_NAME
import com.parallelc.micts.config.AppConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.AppConfig.KEY_DEFAULT_DELAY
import com.parallelc.micts.config.AppConfig.KEY_TILE_DELAY
import com.parallelc.micts.module
import com.parallelc.micts.R
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(entryPoint: Int): Boolean {
    return runCatching {
        val bundle = Bundle()
        bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
        bundle.putInt("omni.entry_point", entryPoint)
        bundle.putBoolean("micts_trigger", true)
        val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val vis = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction")
        val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, vis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7, "hyperOS_home") as Boolean
        } else {
            HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7) as Boolean
        }
    }.onFailure { e ->
        val errMsg = "triggerCircleToSearch failed: " + e.stackTraceToString()
        if (module != null) {
            module!!.log(errMsg)
        } else {
            Log.e("MiCTS", errMsg)
        }
    }.getOrDefault(false)
}

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = if (intent.getBooleanExtra("from_tile", false)) KEY_TILE_DELAY else KEY_DEFAULT_DELAY
        val delay = getSharedPreferences(CONFIG_NAME, MODE_PRIVATE).getLong(key, DEFAULT_CONFIG[key] as Long)
        if (delay > 0) {
            Thread.sleep(delay)
        }
        if (!triggerCircleToSearch(1)) {
            Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}