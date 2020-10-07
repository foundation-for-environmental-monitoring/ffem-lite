package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CalibrationValue(
    var value: Double = 0.0,
    var calibrate: Boolean = false,
    var color: Int = 0
) : Parcelable