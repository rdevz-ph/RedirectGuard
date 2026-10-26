package com.rdevzph.redirectguard.model

enum class InterceptionMode {
    CONFIRMATION_DIALOG,
    AUTO_RETURN
}

enum class InterceptionStrategy {
    AUTO_KILL_SHIZUKU
}

enum class RedirectAction {
    ALLOWED,
    BLOCKED,
    PROMPTED
}

