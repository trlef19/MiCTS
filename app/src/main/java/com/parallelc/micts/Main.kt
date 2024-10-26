package com.parallelc.micts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

private lateinit var module: ModuleMain

class ModuleMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        module = this
    }

    @XposedHooker
    class ContextualSearchIntentHooker : Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                (callback.result as Intent).putExtra("com.android.contextualsearch.flag_secure_found", false)
            }
        }
    }

    class ResourcesHooker {
        companion object {
            private var R: Class<*>? = null

            @SuppressLint("PrivateApi")
            fun hook(param: SystemServerLoadedParam) {
                R = param.classLoader.loadClass("com.android.internal.R\$string")
                module.hook(Resources::class.java.getDeclaredMethod("getString", Int::class.java), GetStringHooker::class.java)
            }

            @XposedHooker
            class GetStringHooker : Hooker {
                companion object {
                    @JvmStatic
                    @BeforeInvocation
                    fun before(callback: BeforeHookCallback) {
                        when (callback.args[0]) {
                            R!!.getField("config_defaultContextualSearchKey").getInt(null) -> {
                                callback.returnAndSkip("omni.entry_point")
                            }

                            R!!.getField("config_defaultContextualSearchPackageName").getInt(null) -> {
                                callback.returnAndSkip("com.google.android.googlequicksearchbox")
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        runCatching {
            ResourcesHooker.hook(param)
            val vims = param.classLoader.loadClass("com.android.server.voiceinteraction.VoiceInteractionManagerService\$VoiceInteractionManagerServiceStub")
            hook(vims.getDeclaredMethod("getContextualSearchIntent", Bundle::class.java), ContextualSearchIntentHooker::class.java)
        }.onFailure { e ->
            log("hook system fail", e)
        }
    }

    @XposedHooker
    class ReturnTrueHooker : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                callback.returnAndSkip(true)
            }
        }
    }

    @XposedHooker
    class ReturnFalseHooker : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                callback.returnAndSkip(false)
            }
        }
    }

    @XposedHooker
    class NavStubViewHooker : Hooker {
        companion object {
            @SuppressLint("PrivateApi")
            private val mCheckLongPress = Runnable {
                runCatching {
                    val bundle = Bundle()
                    bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                    bundle.putInt("omni.entry_point", 1)
                    val iVims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
                    val asInterfaceMethod = iVims.getMethod("asInterface", IBinder::class.java)
                    val getServiceMethod = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
                    val vimsInstance = asInterfaceMethod.invoke(null, getServiceMethod.invoke(null, "voiceinteraction")) ?: return@Runnable
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val showSessionFromSession = vimsInstance.javaClass.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE, String::class.java)
                        showSessionFromSession.invoke(vimsInstance, null, bundle, 7, "hyperOS_home")
                    } else {
                        val showSessionFromSession = vimsInstance.javaClass.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE)
                        showSessionFromSession.invoke(vimsInstance, null, bundle, 7)
                    }
                }.onFailure { e ->
                    module.log("NavStubViewHooker mCheckLongPress fail", e)
                }
            }

            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val view = callback.thisObject as View
                view.removeCallbacks(this.mCheckLongPress)
                runCatching {
                    val mCurrAction = callback.thisObject!!.javaClass.getDeclaredField("mCurrAction")
                    mCurrAction.isAccessible = true
                    if (mCurrAction.getInt(callback.thisObject) == 0) {
                        view.postDelayed(this.mCheckLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                    }
                }.onFailure { e ->
                    module.log("NavStubViewHooker onTouchEvent fail", e)
                }
            }
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