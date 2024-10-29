package com.parallelc.micts.hooker

import android.content.Intent
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

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