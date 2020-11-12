package io.ffem.lite.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ffem.lite.R
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.Toast

class TimerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        val meter = findViewById<Chronometer>(R.id.c_meter)

        //access the button using id
        val btn = findViewById<Button>(R.id.start_timer_fab)
        btn?.setOnClickListener(object : View.OnClickListener {

            var isWorking = false

            override fun onClick(v: View) {

                if (!isWorking) {

                    meter.base = SystemClock.elapsedRealtime()
                    meter.start()
                    isWorking = true
                } else {
                    meter.stop()
                    isWorking = false
                }

                btn.setText(if (!isWorking) R.string.start else R.string.stop)

                Toast.makeText(this@TimerActivity, getString(
                    if (isWorking)
                        R.string.working
                    else
                        R.string.stopped),
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
}