package com.parallelc.micts

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(entryPoint: Int): Boolean {
    val bundle = Bundle()
    bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
    bundle.putInt("omni.entry_point", entryPoint)
    bundle.putBoolean("micts_trigger", true)
    val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
    val vis = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction")
    val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, vis)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7, "hyperOS_home") as Boolean
    } else {
        HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7) as Boolean
    }
}

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("from_tile", false)) {
            Thread.sleep(400)
        }
        triggerCircleToSearch(1)
        finish()
    }
}