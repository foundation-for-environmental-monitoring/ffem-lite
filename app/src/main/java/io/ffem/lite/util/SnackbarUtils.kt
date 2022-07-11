/*

Copyright 2018 Shobhit
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.ffem.lite.util

import android.view.View
import android.widget.TextView
import com.google.android.material.R
import com.google.android.material.snackbar.Snackbar

object SnackbarUtils {
    private const val DURATION_SHORT = 3500
    private const val DURATION_LONG = 5500

    fun showShortSnackbar(view: View, message: String) {
        showSnackbar(view, message, DURATION_SHORT)
    }

    fun showLongSnackbar(view: View, message: String) {
        showSnackbar(view, message, DURATION_LONG)
    }

    /**
     * Displays snackbar with {@param message}
     * and multi-line message enabled.
     *
     * @param view    The view to find a parent from.
     * @param message The text to show.  Can be formatted text.
     */
    private fun showSnackbar(view: View, message: String, duration: Int) {
        if (message.isEmpty()) {
            return
        }
        val snackbar = Snackbar.make(view, message.trim { it <= ' ' }, duration)
        val textView = snackbar.view.findViewById<TextView>(R.id.snackbar_text)
        textView.isSingleLine = false
        snackbar.show()
    }
}