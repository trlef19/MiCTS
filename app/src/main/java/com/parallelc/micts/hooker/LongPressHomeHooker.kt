package com.parallelc.micts.hooker

import android.content.Context
import android.os.Bundle
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_HOME_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Field

class LongPressHomeHooker {
    companion object {
        private lateinit var mContext: Field

        fun hook(param: SystemServerLoadedParam) {
            val shortCutActionsUtils = param.classLoader.loadClass("com.miui.server.input.util.ShortCutActionsUtils")
            module!!.hook(
                shortCutActionsUtils.getDeclaredMethod("triggerFunction", String::class.java, String::class.java, Bundle::class.java, Boolean::class.java, String::class.java),
                TriggerFunctionHooker::class.java
            )
            mContext = shortCutActionsUtils.getDeclaredField("mContext")
            mContext.isAccessible = true
        }

        @XposedHooker
        class TriggerFunctionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    if (callback.args[1] == "long_press_home_key" || callback.args[1] == "long_press_home_key_no_ui") {
                        val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                        if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                            val context = runCatching { mContext.get(callback.thisObject) as? Context }.getOrNull()
                            callback.returnAndSkip(
                                triggerCircleToSearch(
                                    1,
                                    context,
                                    prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}