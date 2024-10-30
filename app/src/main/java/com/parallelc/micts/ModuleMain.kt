package com.parallelc.micts

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.MotionEvent
import com.parallelc.micts.hooker.ContextualSearchIntentHooker
import com.parallelc.micts.hooker.LongPressHomeHooker
import com.parallelc.micts.hooker.NavStubViewHooker
import com.parallelc.micts.hooker.ResourcesHooker
import com.parallelc.micts.hooker.ReturnFalseHooker
import com.parallelc.micts.hooker.ReturnTrueHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(): Boolean {
    val bundle = Bundle()
    bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
    bundle.putInt("omni.entry_point", 1)
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            ResourcesHooker.hook(param)
            val vims = param.classLoader.loadClass("com.android.server.voiceinteraction.VoiceInteractionManagerService\$VoiceInteractionManagerServiceStub")
            hook(vims.getDeclaredMethod("getContextualSearchIntent", Bundle::class.java), ContextualSearchIntentHooker::class.java)
        }.onFailure { e ->
            log("hook system fail", e)
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        if (param.packageName != "com.miui.home" || !param.isFirstPackage) return

        runCatching {
            val circleToSearchHelper = param.classLoader.loadClass("com.miui.home.recents.cts.CircleToSearchHelper")
            hook(circleToSearchHelper.getDeclaredMethod("isSceneForbid", Context::class.java, Int::class.java), ReturnFalseHooker::class.java)
            hook(circleToSearchHelper.getDeclaredMethod("hasCtsFeature", Context::class.java), ReturnTrueHooker::class.java)
            hook(circleToSearchHelper.getDeclaredMethod("isSettingsLongPressHomeAssistantEnabled", Context::class.java), ReturnTrueHooker::class.java)
            hook(circleToSearchHelper.getDeclaredMethod("isThirdHome", Context::class.java), ReturnFalseHooker::class.java)
            return
        }.onFailure { e ->
            log("hook CircleToSearchHelper fail", e)
        }

        runCatching {
            val navStubView = param.classLoader.loadClass("com.miui.home.recents.NavStubView")
            runCatching { navStubView.getDeclaredField("mCheckLongPress") }
                .onSuccess { throw Exception("mCheckLongPress exists") }
                .onFailure {
                    hook(navStubView.getDeclaredMethod("onTouchEvent", MotionEvent::class.java), NavStubViewHooker::class.java)
                }
        }.onFailure { e ->
            log("hook NavStubView fail", e)
        }
    }
}