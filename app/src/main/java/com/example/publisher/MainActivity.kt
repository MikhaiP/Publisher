package com.example.publisher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
//import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID
import com.google.gson.Gson
import org.json.JSONObject
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest



class MainActivity : AppCompatActivity() {
    private var client: Mqtt5BlockingClient? = null
    private var startPublishing: Boolean = false
    private lateinit var studentIDEditText: EditText
    private var studentID: String = ""
    private var id: UInt = 0u
    private var ms_time: Long = 0
    private var speed: Double = 0.0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var locationAction: LocationCallback

    data class LocationData(
        val StudentID: String,
//        val ID: String,
        val Time: Long,
//        val Speed: String,
        val Latitude: Double,
        val Longitude: Double
    )
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var fusedLocationClient: FusedLocationProviderClient



    private val request = 1234

    @RequiresApi(Build.VERSION_CODES.S)
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

        locationAction = object : LocationCallback() {
            override fun onLocationResult(locationR: LocationResult) {
                runPublishLocation(locationR)
            }
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                Log.d("locationlistener", "Location changed: $location")
//                updateLocationData(location)
                publishLocationToBroker(location)
            }

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                request
            )
        } else {
            // Start location updates if permissions are already granted
            startLocationUpdates()
        }
//        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//        if (!isGPSEnabled) {
//            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
//            Log.d("LocationUpdates", "GPS provider is disabled")
//        }

    }
    fun runPublishLocation(locationResult: LocationResult){
        locationResult.locations.forEach{location ->
            publishLocationToBroker(location)
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startLocationUpdates() {
//        var locationRequest: LocationRequest

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(5000)
                .setMinUpdateDistanceMeters(5f)
                .setMinUpdateIntervalMillis(2000)
                .build()


        fusedLocationClient.requestLocationUpdates(locationRequest,locationAction,Looper.getMainLooper())
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,  // Update interval in milliseconds
                1f,     // Minimum distance in meters
                locationListener
            )
            Log.d("LocationUpdates", "Started location updates")


    }

    fun publishToBroker(view: View?) {
        if (startPublishing) {
            Toast.makeText(this, "Already publishing to the broker", Toast.LENGTH_SHORT).show()
            return
        }
        startPublishing = true
        try {
            client?.connect()
            Log.d("Connect", "Connected to the broker")

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                ActivityCompat.requestPermissions(this, permissions, request)
                return
            }

            // Start location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
            Toast.makeText(this, "Started publishing to the broker", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred when connecting to the broker", Toast.LENGTH_SHORT).show()
        }
//        if (startPublishing){
//            Toast.makeText(this, "Started publishing to broker already", Toast.LENGTH_SHORT).show()
//            return
//        }
//        startPublishing = true
//        try {
//            client?.connect()
//            Log.d("Connect", "Started publishing to client")
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED) {
//
//                // Publish the current location data
//                publishLocationToBroker()
//            } else {
//                Log.d("PermissionCheck", "Location permissions not granted")
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
//        }
//        Toast.makeText(this, "Started publishing to broker", Toast.LENGTH_SHORT).show()



//
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                )== PackageManager.PERMISSION_GRANTED
//            ) {
//                var perm = arrayOf(
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//
//                ActivityCompat.requestPermissions(this,perm,request)
//                return
//            }
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
//            publishLocationToBroker()
//
//        } catch (e:Exception){
//            e.printStackTrace()
//            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
//        }
//        Toast.makeText(this, "Started publishing to broker", Toast.LENGTH_SHORT).show()
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
//    private fun updateLocationData(location: Location){
//        id += 1u
//        ms_time = System.currentTimeMillis()
//        val speedKMH = location.speed * 3.6
//        speed = speedKMH.toDouble()
//        latitude = location.latitude
//        longitude = location.longitude
//
//        Log.d("updateLocationData", "Updated: ID = $id, Time = $ms_time, Speed = $speed, Latitude = $latitude, Longitude = $longitude")
//    }

    private fun publishLocationToBroker(location: Location){
        studentID = studentIDEditText.text.toString()
////        val locationData = "StudentID: $studentID, ID: $id, Time: $ms_time, Speed: $speed km/h, Latitude: $latitude, Longitude: $longitude"
////        Log.d("Data", "StudentID: $studentID, ID: $id, Time: $ms_time, Speed: $speed km/h, Latitude: $latitude, Longitude: $longitude")
//
//        id += 1u
        ms_time = System.currentTimeMillis()
//        speed = location.speed * 3.6
        latitude = location.latitude
        longitude = location.longitude

        Log.d("Location Update", "$studentID, $ms_time, $latitude, $longitude")
        val locationData = LocationData(
            StudentID = studentID,
//            ID = "$id",
            Time = ms_time,
//            Speed = "$speed km/h",
            Latitude = latitude,
            Longitude = longitude
        )
        val test = "Test"
        val gson = Gson()
        val locationDataJson = gson.toJson(locationData)

        Log.d("Data", locationDataJson)

        try{
            client?.publishWith()?.topic("assignment/location")?.payload(locationDataJson.toByteArray(Charsets.UTF_8))?.send()
            Log.d("Send", "Data sent $locationDataJson")
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when sending a message to the broker", Toast.LENGTH_SHORT).show()
        }

    }




}