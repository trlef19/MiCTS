package com.parallelc.micts

import android.content.Context
import android.os.Build
import com.parallelc.micts.hooker.InvokeOmniHooker
import com.parallelc.micts.hooker.LongPressHomeHooker
import com.parallelc.micts.hooker.NavStubViewHooker
import com.parallelc.micts.hooker.VIMSHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

lateinit var module: ModuleMain

class ModuleMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        module = this
    }

    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                VIMSHooker.hook(param)
            }.onFailure { e ->
                log("hook VIMS fail", e)
            }
        }
        if (Build.MANUFACTURER == "Xiaomi") {
            runCatching {
                LongPressHomeHooker.hook(param)
            }.onFailure { e ->
                log("hook LongPressHome fail", e)
            }
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        if (!param.isFirstPackage) return

        when (param.packageName) {
            "com.miui.home", "com.mi.android.globallauncher" -> {
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
            "com.google.android.googlequicksearchbox" -> {
                val buildClass = param.classLoader.loadClass("android.os.Build")
                val MANUFACTURER = buildClass.getDeclaredField("MANUFACTURER")
                MANUFACTURER.isAccessible = true
                MANUFACTURER.set(null, "Google")
                val BRAND = buildClass.getDeclaredField("BRAND")
                BRAND.isAccessible = true
                BRAND.set(null, "google")
                val MODEL = buildClass.getDeclaredField("MODEL")
                MODEL.isAccessible = true
                MODEL.set(null, "Pixel 8 Pro")
                val DEVICE = buildClass.getDeclaredField("DEVICE")
                DEVICE.isAccessible = true
                DEVICE.set(null, "husky")
            }
        }
    }
}