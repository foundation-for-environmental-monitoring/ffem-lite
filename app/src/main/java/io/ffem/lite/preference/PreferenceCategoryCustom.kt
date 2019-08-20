package io.ffem.lite.preference

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceCategory
import android.util.AttributeSet
import io.ffem.lite.R

/**
 * A custom category style for the preferences screen.
 */
@Suppress("unused")
@SuppressLint("PrivateResource")
class PreferenceCategoryCustom : PreferenceCategory {

    constructor(context: Context) : super(context) {
        layoutResource = R.layout.preference_category
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        layoutResource = R.layout.preference_category
    }
}
