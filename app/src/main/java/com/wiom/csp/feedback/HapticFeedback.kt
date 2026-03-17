package com.wiom.csp.feedback

import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedback {

    fun standard(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun urgent(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
