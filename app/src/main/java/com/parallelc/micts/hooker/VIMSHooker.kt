package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
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
            module.hook(vims.getDeclaredMethod("showSessionFromSession", IBinder::class.java, Bundle::class.java, Integer.TYPE, String::class.java), ShowSessionHooker::class.java)
            module.hook(vims.getDeclaredMethod("getContextualSearchIntent", Bundle::class.java), ContextualSearchIntentHooker::class.java)
        }

        @XposedHooker
        class ShowSessionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) : MethodUnhooker<Method>? {
                    runCatching {
                        if ((callback.args[1] as Bundle).getBoolean("micts_trigger", false)) {
                            Binder.clearCallingIdentity()
                        }
                        return module.hook(Resources::class.java.getDeclaredMethod("getString", Int::class.java), GetStringHooker::class.java)
                    }.onFailure { e ->
                        module.log("hook resources fail", e)
                    }
                    return null
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
                            callback.returnAndSkip("omni.entry_point")
                        }
                        contextualSearchPackageName -> {
                            callback.returnAndSkip("com.google.android.googlequicksearchbox")
                        }
                    }
                }
            }
        }

        @XposedHooker
        class ContextualSearchIntentHooker : Hooker {
            companion object {
                @JvmStatic
                @AfterInvocation
                fun after(callback: AfterHookCallback) {
                    (callback.result as Intent?)?.putExtra("com.android.contextualsearch.flag_secure_found", false)
                }
            }
        }
    }
}