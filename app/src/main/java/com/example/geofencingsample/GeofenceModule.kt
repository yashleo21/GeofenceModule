package com.example.geofencingsample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class GeofenceModule(val context: Context, val lifecycle: Lifecycle) {
    val DAVINTA_OFFICE_GEOFENCE_KEY = "davintaOffice"

    val geofencingClient = LocationServices.getGeofencingClient(context)
    val geofencingList = mutableListOf<Geofence>()
    var latlng: MutableLiveData<Pair<Double, Double>> = MutableLiveData()

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    init {
        Dexter.withActivity(context as Activity)
            .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        Toast.makeText(context, "Permission for fine location granted", Toast.LENGTH_SHORT).show()
                        pollLocationChanges()
                        return
                    }
                    report?.deniedPermissionResponses?.forEach {
                        if (it.isPermanentlyDenied) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()

        latlng.observe(context as MainActivity, Observer {
            Log.d("Emre1s", "Observer called ${it.first} and ${it.second}")
            createAndMonitorGeofence(it)
            latlng.removeObservers(context)
        })
    }

    @SuppressLint("MissingPermission")
    fun createAndMonitorGeofence(latlng: Pair<Double, Double>) {
        geofencingList.add(Geofence.Builder()
            .setRequestId(DAVINTA_OFFICE_GEOFENCE_KEY)
            .setCircularRegion(latlng.first,latlng.second, 100f) //Lat Lng Radius
            .setExpirationDuration(60000)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()).also { responseType ->
            Log.d("Emre1s", "Whats happening?? Boolean is $responseType")
            geofencingClient?.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.d("Emre1s", "Geofence added")
                    pollLocationChanges()
                }
                addOnFailureListener {
                    Log.d("Emre1s", "Geofence addition failed ${it.localizedMessage}")
                }
            }
        }

    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofencingList)
        }.build()
    }

    @SuppressLint("MissingPermission")
    fun pollLocationChanges() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = createLocationRequest()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    latlng.value = location.latitude to location.longitude
                    Log.d("Emre1s NEW", "New latitude is : " +
                            "${location.latitude} and new longitude is ${location.longitude}")
                }
            }
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null )
        }
    }

    fun createLocationRequest(): LocationRequest? {

        return LocationRequest.create()?.apply {
            interval = 5000  //5s
            fastestInterval = 5000 //5s
            smallestDisplacement = 100f //100m
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
}
