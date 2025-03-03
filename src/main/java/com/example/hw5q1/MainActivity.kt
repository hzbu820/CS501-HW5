package com.example.hw5q1

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.pow

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var altitudeText: TextView
    private lateinit var backgroundLayout: ConstraintLayout

    // For simulation
    private val handler = Handler()
    private var simulatedPressure = 1013.25f // sea-level reference
    private val simulateRunnable = object : Runnable {
        override fun run() {
            // Randomly vary pressure
            simulatedPressure += (Math.random().toFloat() - 0.5f)
            updateAltitude(simulatedPressure)
            handler.postDelayed(this, 1000) // update every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        altitudeText = findViewById(R.id.altitudeText)
        backgroundLayout = findViewById(R.id.backgroundLayout)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        handler.post(simulateRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Register real sensor listener
        pressureSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(simulateRunnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            updateAltitude(pressure)
        }
    }

    private fun updateAltitude(pressure: Float) {
        val altitude = 44330f * (1 - (pressure / 1013.25f).pow(1f / 5.255f))
        altitudeText.text = String.format("%.2f m", altitude)

        // Darker background at higher altitude
        val factor = (altitude / 10000f).coerceIn(0f, 1f)
        val gray = (255 - (factor * 255)).toInt().coerceIn(0, 255)
        backgroundLayout.setBackgroundColor(android.graphics.Color.rgb(gray, gray, gray))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}
