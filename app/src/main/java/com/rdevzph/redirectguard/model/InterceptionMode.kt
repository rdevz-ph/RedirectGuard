package com.rdevzph.redirectguard.model

enum class InterceptionMode {
    CONFIRMATION_DIALOG,
    AUTO_RETURN
}

enum class RedirectAction {
    ALLOWED,
    BLOCKED,
    PROMPTED
}
