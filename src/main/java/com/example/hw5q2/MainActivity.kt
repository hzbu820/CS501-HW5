package com.example.hw5q2

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Compass heading state (in degrees)
    private var heading by mutableStateOf(0f)
    // Digital level states (pitch & roll from gyro integration)
    private var pitch by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)

    // Gravity & geomagnetic arrays for compass calculation
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    // For simple gyroscope integration
    private var lastGyroTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            CompassAndLevelUI(heading = heading, pitch = pitch, roll = roll)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravity[0] = event.values[0]
                gravity[1] = event.values[1]
                gravity[2] = event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic[0] = event.values[0]
                geomagnetic[1] = event.values[1]
                geomagnetic[2] = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                val currentTime = event.timestamp
                if (lastGyroTimestamp != 0L) {
                    val dt = (currentTime - lastGyroTimestamp) * 1e-9f
                    val factor = (180f / PI).toFloat() // rad/s to deg/s
                    pitch += event.values[0] * dt * factor
                    roll += event.values[1] * dt * factor
                }
                lastGyroTimestamp = currentTime
            }
        }
        updateCompassHeading()
    }

    private fun updateCompassHeading() {
        val rMat = FloatArray(9)
        val iMat = FloatArray(9)
        if (SensorManager.getRotationMatrix(rMat, iMat, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rMat, orientation)
            // orientation[0] is azimuth in radians; convert to degrees
            val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            heading = (azimuthDeg + 360) % 360
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}

@Composable
fun CompassAndLevelUI(heading: Float, pitch: Float, roll: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Compass needle image
        Image(
            painter = painterResource(id = R.drawable.compass_needle),
            contentDescription = "Compass Needle",
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    rotationZ = -heading
                }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(text = "Heading: ${heading.roundToInt()}°")
            Text(text = "Pitch: ${pitch.roundToInt()}°")
            Text(text = "Roll: ${roll.roundToInt()}°")
        }
    }
}
