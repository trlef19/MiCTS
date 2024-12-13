package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import com.parallelc.micts.module
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Method

class CSMSHooker {
    companion object {
        private var enforcePermission: Method? = null
        private var getContextualSearchPackageName: Method? = null
        private var contextualSearchPackageName: Int = 0

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerLoadedParam) {
            val rString = param.classLoader.loadClass("com.android.internal.R\$string")
            contextualSearchPackageName = rString.getField("config_defaultContextualSearchPackageName").getInt(null)
            val systemServer = param.classLoader.loadClass("com.android.server.SystemServer")
            module!!.hook(systemServer.getDeclaredMethod("deviceHasConfigString", Context::class.java, Int::class.java), DeviceHasConfigStringHooker::class.java)

            val csms = param.classLoader.loadClass("com.android.server.contextualsearch.ContextualSearchManagerService")
            enforcePermission = csms.getDeclaredMethod("enforcePermission", String::class.java)
            getContextualSearchPackageName = csms.getDeclaredMethod("getContextualSearchPackageName")
        }

        @SuppressLint("PrivateApi")
        fun startContextualSearch(entryPoint: Int): Boolean {
            var unhookers = mutableListOf<MethodUnhooker<Method>>()
            return runCatching {
                unhookers += module!!.hook(enforcePermission!!, EnforcePermissionHooker::class.java)
                unhookers += module!!.hook(getContextualSearchPackageName!!, GetCSPackageNameHooker::class.java)

                val icsmClass = Class.forName("android.app.contextualsearch.IContextualSearchManager")
                val cs = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "contextual_search")
                val icsm = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, cs)
                icsmClass.getDeclaredMethod("startContextualSearch", Int::class.java).invoke(icsm, entryPoint)
            }.onFailure { e ->
                module!!.log("invoke startContextualSearch fail", e)
            }.also {
                unhookers.forEach { unhooker -> unhooker.unhook() }
            }.isSuccess
        }

        @XposedHooker
        class DeviceHasConfigStringHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    if (callback.args[1] == contextualSearchPackageName) {
                        callback.returnAndSkip(true)
                    }
                }
            }
        }

        @XposedHooker
        class EnforcePermissionHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    callback.returnAndSkip(null)
                }
            }
        }

        @XposedHooker
        class GetCSPackageNameHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    callback.returnAndSkip("com.google.android.googlequicksearchbox")
                }
            }
        }
    }
}