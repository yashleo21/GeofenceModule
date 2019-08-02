package com.example.geofencingsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("Emre1s", "broadcast ccalled")
        val geoFenceEvent = GeofencingEvent.fromIntent(intent)
        if (geoFenceEvent.hasError()) {
            val errorMessage = geoFenceEvent.errorCode
            Log.e("Emre1s", "Error code is $errorMessage")
            return
        }

        val geofenceTransition = geoFenceEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val triggeringGeofences = geoFenceEvent.triggeringGeofences
            Log.d("Emre1s", "Transition triggered: Type: $geofenceTransition Geofence: ${triggeringGeofences[0].requestId}")

        } else {
            Log.e("Emre1s", "Don't need to monitor this transition $geofenceTransition")
        }
    }

}