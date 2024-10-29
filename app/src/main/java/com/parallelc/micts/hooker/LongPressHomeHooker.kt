package com.parallelc.micts.hooker

import android.os.Bundle
import com.parallelc.micts.module
import com.parallelc.micts.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Method

class LongPressHomeHooker {
    companion object {
        private var hookFunction = mutableMapOf<String, Method?>()

        fun hook(param: SystemServerLoadedParam) {
            val shortCutActionsUtils = param.classLoader.loadClass("com.miui.server.input.util.ShortCutActionsUtils")
            hookFunction["launch_voice_assistant"] = shortCutActionsUtils.declaredMethods.firstOrNull { method: Method -> method.name == "launchVoiceAssistant" }
            hookFunction["launch_google_search"] = shortCutActionsUtils.declaredMethods.firstOrNull { method: Method -> method.name == "launchGoogleSearch" }
            module.hook(
                shortCutActionsUtils.getDeclaredMethod("triggerFunction", String::class.java, String::class.java, Bundle::class.java, Boolean::class.java, String::class.java),
                TriggerFunctionHooker::class.java
            )
        }

        @XposedHooker
        class LaunchFunctionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    triggerCircleToSearch()
                    callback.returnAndSkip(true)
                }
            }
        }

        @XposedHooker
        class TriggerFunctionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) : MethodUnhooker<Method>? {
                    if (callback.args[1] == "long_press_home_key" || callback.args[1] == "long_press_home_key_no_ui") {
                        hookFunction[callback.args[0]]?.let { return module.hook(it, LaunchFunctionHooker::class.java) }
                    }
                    return null
                }

                @JvmStatic
                @AfterInvocation
                fun after(callback: AfterHookCallback, unhook: MethodUnhooker<Method>?) {
                    unhook?.unhook()
                }
            }
        }
    }
}