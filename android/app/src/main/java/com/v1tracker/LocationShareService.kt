package com.v1tracker

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class LocationShareService : Service() {
    private lateinit var client: FusedLocationProviderClient
    private val worker = Executors.newSingleThreadExecutor()
    private var callback: LocationCallback? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); client = LocationServices.getFusedLocationProviderClient(this); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopUpdates(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        startForeground(4107, notification())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5 * 60 * 1000L).setMinUpdateIntervalMillis(60 * 1000L).build()
        callback = object : LocationCallback() { override fun onLocationResult(result: LocationResult) { result.lastLocation?.let { upload(it) } } }
        try { client.requestLocationUpdates(request, callback!!, mainLooper) } catch (_: SecurityException) { stopSelf() }
        return START_STICKY
    }
    private fun upload(location: Location) {
        val prefs = getSharedPreferences("v1tracker", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "") ?: ""
        val key = prefs.getString("tracking_key", "") ?: ""
        val anon = prefs.getString("anon_key", "") ?: ""
        if (endpoint.isBlank() || key.isBlank()) return
        worker.execute { try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.connectTimeout = 15000; conn.readTimeout = 15000; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (anon.isNotBlank()) conn.setRequestProperty("apikey", anon)
            val body = JSONObject().put("action", "update_location").put("tracking_key", key).put("latitude", location.latitude).put("longitude", location.longitude).put("accuracy_m", location.accuracy).toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            stream?.close(); conn.disconnect()
        } catch (_: Exception) { /* Next permitted location update will retry. */ } }
    }
    private fun stopUpdates() { callback?.let { client.removeLocationUpdates(it) }; callback = null }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { val channel = NotificationChannel("v1_location", "V1 Tracker location sharing", NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(channel) } }
    private fun notification(): Notification {
        val stopIntent = Intent(this, LocationShareService::class.java).setAction("STOP")
        val pending = PendingIntent.getService(this, 4108, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "v1_location").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("V1 Tracker is sharing location").setContentText("Location updates are enabled. Tap Stop to end sharing.").setOngoing(true).addAction(0, "Stop sharing", pending).build()
    }
    override fun onDestroy() { stopUpdates(); worker.shutdown(); super.onDestroy() }
}