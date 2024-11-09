package com.parallelc.micts

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import com.parallelc.micts.hooker.InvokeOmniHooker
import com.parallelc.micts.hooker.LongPressHomeHooker
import com.parallelc.micts.hooker.NavStubViewHooker
import com.parallelc.micts.hooker.VIMSHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(entryPoint: Int): Boolean {
    val bundle = Bundle()
    bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
    bundle.putInt("omni.entry_point", entryPoint)
    val iVims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
    val asInterfaceMethod = iVims.getMethod("asInterface", IBinder::class.java)
    val getServiceMethod = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
    val vimsInstance = asInterfaceMethod.invoke(null, getServiceMethod.invoke(null, "voiceinteraction")) ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val showSessionFromSession = vimsInstance.javaClass.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE, String::class.java)
        return showSessionFromSession.invoke(vimsInstance, null, bundle, 7, "hyperOS_home") as Boolean
    } else {
        val showSessionFromSession = vimsInstance.javaClass.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE)
        return showSessionFromSession.invoke(vimsInstance, null, bundle, 7) as Boolean
    }
}

lateinit var module: ModuleMain

class ModuleMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        module = this
    }

    @SuppressLint("PrivateApi")
    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        runCatching {
            LongPressHomeHooker.hook(param)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                VIMSHooker.hook(param)
            }
        }.onFailure { e ->
            log("hook system fail", e)
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        if ((param.packageName != "com.miui.home" && param.packageName != "com.mi.android.globallauncher") || !param.isFirstPackage) return

        runCatching {
            val circleToSearchHelper = param.classLoader.loadClass("com.miui.home.recents.cts.CircleToSearchHelper")
            hook(circleToSearchHelper.getDeclaredMethod("invokeOmni", Context::class.java, Int::class.java, Int::class.java), InvokeOmniHooker::class.java)
            return
        }.onFailure { e ->
            log("hook CircleToSearchHelper fail", e)
        }

        runCatching {
            NavStubViewHooker.hook(param)
        }.onFailure { e ->
            log("hook NavStubView fail", e)
        }
    }
}