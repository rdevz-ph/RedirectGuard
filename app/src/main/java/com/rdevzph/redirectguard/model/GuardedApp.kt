package com.rdevzph.redirectguard.model

data class GuardedApp(
    val packageName: String,
    val appName: String,
    val category: String,
    val isGuarded: Boolean,
    val isPreset: Boolean = false
) {
    companion object {
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
                packageName = "com.amazon.mShop.android.shopping",
                appName = "Amazon Shopping",
                category = "Shopping",
                isGuarded = true,
                isPreset = true
            )
        )
    }
}
