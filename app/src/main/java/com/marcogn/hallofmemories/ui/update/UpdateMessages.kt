package com.marcogn.hallofmemories.ui.update

import android.content.Context
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.update.ApkRejection
import com.marcogn.hallofmemories.domain.update.UpdateFailure

/** A short, user-facing reason for [this]; the detail strings of the system/exception are appended only where useful. */
fun UpdateFailure.toMessage(context: Context): String = when (this) {
    is UpdateFailure.Network -> context.getString(R.string.update_error_network)
    UpdateFailure.RateLimited -> context.getString(R.string.update_error_rate_limited)
    is UpdateFailure.Http -> context.getString(R.string.update_error_http, code)
    is UpdateFailure.InvalidResponse -> context.getString(R.string.update_error_invalid_response)
    is UpdateFailure.UnusableRelease -> context.getString(R.string.update_error_unusable_release)
    UpdateFailure.Storage -> context.getString(R.string.update_error_storage)
    UpdateFailure.SizeMismatch, UpdateFailure.ChecksumMismatch -> context.getString(R.string.update_error_corrupted)
    is UpdateFailure.VerificationFailed -> context.getString(
        when (reason) {
            ApkRejection.UNREADABLE -> R.string.update_error_unreadable
            ApkRejection.DIFFERENT_PACKAGE -> R.string.update_error_different_package
            ApkRejection.NOT_NEWER -> R.string.update_error_not_newer
            ApkRejection.DIFFERENT_SIGNATURE -> R.string.update_error_different_signature
        },
    )
    is UpdateFailure.InstallFailed ->
        if (message.isNullOrBlank()) context.getString(R.string.update_error_install)
        else context.getString(R.string.update_error_install_detail, message)
}
