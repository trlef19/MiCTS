package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_TRIGGER_SERVICE
import com.parallelc.micts.module
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Method

class VIMSHooker {
    companion object {
        private var contextualSearchKey: Int = 0
        private var contextualSearchPackageName: Int = 0

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerLoadedParam) {
            val vims = param.classLoader.loadClass("com.android.server.voiceinteraction.VoiceInteractionManagerService\$VoiceInteractionManagerServiceStub")
            val rString = param.classLoader.loadClass("com.android.internal.R\$string")
            contextualSearchKey = rString.getField("config_defaultContextualSearchKey").getInt(null)
            contextualSearchPackageName = rString.getField("config_defaultContextualSearchPackageName").getInt(null)
            module!!.hook(vims.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE, String::class.java), ShowSessionHooker::class.java)
        }

        @XposedHooker
        class ShowSessionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) : MethodUnhooker<Method>? {
                    return runCatching {
                        if (!(callback.args[1] as Bundle).getBoolean("micts_trigger", false)) return@runCatching null
                        Binder.clearCallingIdentity()
                        module!!.hook(Resources::class.java.getDeclaredMethod("getString", Int::class.java), GetStringHooker::class.java)
                    }.onFailure { e ->
                        module!!.log("hook resources fail", e)
                    }.getOrNull()
                }

                @JvmStatic
                @AfterInvocation
                fun after(callback: AfterHookCallback, unhooker: MethodUnhooker<Method>?) {
                    unhooker?.unhook()
                }
            }
        }

        @XposedHooker
        class GetStringHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    when (callback.args[0]) {
                        contextualSearchKey -> {
                            val triggerService = module!!.getRemotePreferences(CONFIG_NAME).getInt(KEY_TRIGGER_SERVICE, DEFAULT_CONFIG[KEY_TRIGGER_SERVICE] as Int)
                            callback.returnAndSkip(if (triggerService != TriggerService.VIS.ordinal) "omni.entry_point" else "")
                        }
                        contextualSearchPackageName -> {
                            callback.returnAndSkip("com.google.android.googlequicksearchbox")
                        }
                    }
                }
            }
        }
    }
}