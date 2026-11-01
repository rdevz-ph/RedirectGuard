package com.rdevzph.redirectguard.model

data class GuardedApp(
    val packageName: String,
    val appName: String,
    val category: String,
    val isGuarded: Boolean,
    val isPreset: Boolean = false
) {
    companion object {
        val DEFAULT_BROWSER_PRESETS = listOf(
            GuardedApp(
                packageName = "com.android.chrome",
                appName = "Google Chrome",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "org.mozilla.firefox",
                appName = "Firefox",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.brave.browser",
                appName = "Brave Browser",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.opera.browser",
                appName = "Opera Browser",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.opera.mini.native",
                appName = "Opera Mini",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.microsoft.emmx",
                appName = "Microsoft Edge",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.sec.android.app.sbrowser",
                appName = "Samsung Internet",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.transsion.phoenix",
                appName = "Phoenix Browser",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.UCMobile.intl",
                appName = "UC Browser",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.duckduckgo.mobile.android",
                appName = "DuckDuckGo",
                category = "Browser",
                isGuarded = false,
                isPreset = true
            )
        )

        val DEFAULT_SHOPPING_PRESETS = listOf(
            GuardedApp(
                packageName = "com.shopee.ph",
                appName = "Shopee PH",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.shopee.id",
                appName = "Shopee ID",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.shopee.my",
                appName = "Shopee MY",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.shopee.vn",
                appName = "Shopee VN",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.shopee.th",
                appName = "Shopee TH",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.shopee.sg",
                appName = "Shopee SG",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.lazada.android",
                appName = "Lazada",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.einnovation.temu",
                appName = "Temu",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.zzkko",
                appName = "SHEIN",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.alibaba.aliexpresshd",
                appName = "AliExpress",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                category = "Shopping & Social",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.ss.android.ugc.trill",
                appName = "TikTok",
                category = "Shopping & Social",
                isGuarded = true,
                isPreset = true
            ),
            GuardedApp(
                packageName = "com.amazon.mShop.android.shopping",
                appName = "Amazon Shopping",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            )
        )
    }
}
