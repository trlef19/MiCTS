package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.res.Resources
import com.parallelc.micts.module
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

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