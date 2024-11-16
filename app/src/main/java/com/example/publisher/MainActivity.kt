package com.example.publisher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID

class MainActivity : AppCompatActivity(){
    private var client: Mqtt5BlockingClient? = null
    private var startPublishing: Boolean = false
    private lateinit var studentIDEditText: EditText
    private var studentID: String = ""
    private var id: UInt = 0u
    private var ms_time: Long = 0
    private var speed: Double = 0.0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private val request = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        studentIDEditText = findViewById(R.id.studentID)
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816035889.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                updateLocationData(location)
            }

        }
    }

    fun publishToBroker(view: View?) {
        if (startPublishing){
            Toast.makeText(this, "Started publishing to broker already", Toast.LENGTH_SHORT).show()
            return
        }
        startPublishing = true
        try {
            client?.connect()
            Log.d("Connect", "Started publishing to client")

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                var perm = arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

                ActivityCompat.requestPermissions(this,perm,request)
                return
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
            publishLocationToBroker()

        } catch (e:Exception){
            e.printStackTrace()
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, "Started publishing to broker", Toast.LENGTH_SHORT).show()
    }

    fun stopPublish(view: View?) {
        if (!startPublishing){
            Toast.makeText(this, "Connection to broker already cut", Toast.LENGTH_SHORT).show()
            return
        }
        startPublishing = false
        try {
            client?.disconnect()
            Log.d("Disconnect", "Disconnect from the broker ")

            locationManager.removeUpdates(locationListener)
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when disconnecting from broker", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, "Disconnecting from broker", Toast.LENGTH_SHORT).show()
    }
    private fun updateLocationData(location: Location){
        id += 1u
        ms_time = System.currentTimeMillis()
        val speedKMH = location.speed * 3.6
        speed = speedKMH.toDouble()
        latitude = location.latitude
        longitude = location.longitude
    }

    private fun publishLocationToBroker(){
        studentID = studentIDEditText.text.toString()
        val locationData = "StudentID: $studentID, ID: $id, Time: $ms_time, Speed: $speed km/h, Latitude: $latitude, Longitude: $longitude"
        Log.d("Data", "StudentID: $studentID, ID: $id, Time: $ms_time, Speed: $speed km/h, Latitude: $latitude, Longitude: $longitude")

        try{
            client?.publishWith()?.topic("assignment/location")?.payload(locationData.toByteArray())?.send();
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when sending a message to the broker", Toast.LENGTH_SHORT).show()
        }
    }




}