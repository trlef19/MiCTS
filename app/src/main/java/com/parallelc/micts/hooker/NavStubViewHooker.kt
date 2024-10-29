package com.parallelc.micts.hooker

import android.view.View
import android.view.ViewConfiguration
import com.parallelc.micts.module
import com.parallelc.micts.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

@XposedHooker
class NavStubViewHooker : Hooker {
    companion object {
        private val mCheckLongPress = Runnable {
            runCatching {
                triggerCircleToSearch()
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