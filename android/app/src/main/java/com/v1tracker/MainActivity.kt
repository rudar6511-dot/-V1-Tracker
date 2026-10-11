package com.v1tracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var project: EditText
    private lateinit var anon: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var deviceName: EditText
    private lateinit var keyView: TextView
    private lateinit var status: TextView
    private var accessToken = ""

    private val prefs by lazy { getSharedPreferences("v1tracker", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF07111F.toInt())
        }
        fun field(hint: String, secret: Boolean = false): EditText {
            return EditText(this).apply {
                this.hint = hint
                setSingleLine(true)
                setTextColor(0xFFFFFFFF.toInt())
                setHintTextColor(0xFF9BB0C8.toInt())
                if (secret) inputType = 129
                root.addView(this, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        fun button(label: String, action: () -> Unit) {
            root.addView(Button(this).apply { text = label; setOnClickListener { action() } },
                ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(TextView(this).apply {
            text = "V1 TRACKER\nFind your own phone from your laptop"
            textSize = 22f; setTextColor(0xFF58E3DC.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Use only on your own device. Location sharing stays visible in an ongoing notification."
            setTextColor(0xFFB6C7D9.toInt())
        })
        project = field("Supabase project URL (https://....supabase.co)")
        anon = field("Supabase anon/public key")
        email = field("Account email")
        password = field("Account password", true)
        deviceName = field("Device name (e.g. My phone)")
        project.setText(prefs.getString("project_url", ""))
        anon.setText(prefs.getString("anon_key", ""))
        deviceName.setText(prefs.getString("device_name", "My Android phone"))
        button("Create account") { authenticate(signUp = true) }
        button("Log in and enroll this phone") { authenticate(signUp = false) }
        button("Show / create this phone's Secret Key") { enrollDevice() }
        keyView = TextView(this).apply {
            text = if ((prefs.getString("tracking_key", "") ?: "").isNotBlank()) "Secret key is stored on this phone. Use Show key to reveal it; keep it private." else "This phone is not enrolled yet."
            setTextColor(0xFF83F0C5.toInt()); textSize = 14f
            root.addView(this)
        }
        button("Show Secret Key") {
            val k = prefs.getString("tracking_key", "") ?: ""
            keyView.text = if (k.isBlank()) "No key yet. Log in, then enroll this phone." else "SECRET KEY (keep private):\n$k"
        }
        button("Start visible location sharing") { startSharing() }
        button("Stop location sharing") {
            startService(Intent(this, LocationShareService::class.java).setAction("STOP"))
            setStatus("Location sharing stopped.")
        }
        status = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); text = "Ready."; setPadding(0, 20, 0, 0); root.addView(this) }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun base(): String = project.text.toString().trim().trimEnd('/')
    private fun functionUrl(): String = base() + "/functions/v1/v1-tracker"

    private fun authenticate(signUp: Boolean) {
        val p = base()
        val publicKey = anon.text.toString().trim()
        val e = email.text.toString().trim()
        val pass = password.text.toString()
        if (!p.startsWith("https://") || publicKey.isBlank() || e.isBlank() || pass.isBlank()) {
            setStatus("Fill in project URL, anon key, email and password first."); return
        }
        saveConfig()
        setStatus("Contacting Supabase…")
        worker.execute {
            try {
                val route = if (signUp) "/auth/v1/signup" else "/auth/v1/token?grant_type=password"
                val response = requestJson(p + route, JSONObject().put("email", e).put("password", pass), publicKey)
                val token = response.optString("access_token")
                if (token.isBlank()) {
                    runOnUiThread { setStatus(if (signUp) "Account request sent. If email confirmation is enabled, confirm your email, then log in." else "No session token returned. Check login details and email confirmation.") }
                    return@execute
                }
                val previousAccount = prefs.getString("account_email", "") ?: ""
                val existingKeyAccount = prefs.getString("tracking_key_email", "") ?: ""
                // Never reuse a key on this installation after switching accounts.
                if ((previousAccount.isNotBlank() && !previousAccount.equals(e, ignoreCase = true)) ||
                    (existingKeyAccount.isNotBlank() && !existingKeyAccount.equals(e, ignoreCase = true)) ||
                    (existingKeyAccount.isBlank() && (prefs.getString("tracking_key", "") ?: "").isNotBlank())) {
                    startService(Intent(this@MainActivity, LocationShareService::class.java).setAction("STOP"))
                    prefs.edit().remove("tracking_key").remove("tracking_key_email").apply()
                }
                accessToken = token
                prefs.edit().putString("access_token", token).putString("account_email", e).apply()
                runOnUiThread {
                    setStatus("Signed in as $e. This account will get its own phone Secret Key.")
                    if ((prefs.getString("tracking_key", "") ?: "").isBlank()) enrollDevice()
                }
            } catch (e: Exception) { runOnUiThread { setStatus("Sign-in failed: " + (e.message ?: "network error")) } }
        }
    }

    private fun enrollDevice() {
        val token = accessToken.ifBlank { prefs.getString("access_token", "") ?: "" }
        if (token.isBlank()) { setStatus("Log in first, then enroll this phone."); return }
        if ((prefs.getString("tracking_key", "") ?: "").isNotBlank()) {
            setStatus("This phone already has its own key. Use Show Secret Key to reveal it, or revoke it before re-enrolling.")
            return
        }
        saveConfig()
        setStatus("Creating a unique key for this phone…")
        worker.execute {
            try {
                val conn = URL(functionUrl()).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.connectTimeout = 15000; conn.readTimeout = 15000; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", anon.text.toString().trim())
                conn.setRequestProperty("Authorization", "Bearer " + token)
                val body = JSONObject().put("action", "register").put("device_name", deviceName.text.toString().trim()).toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val text = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) throw Exception(JSONObject(text).optString("error", "server rejected enrollment"))
                val key = JSONObject(text).optString("tracking_key")
                if (key.isBlank()) throw Exception("server did not return a key")
                val accountEmail = prefs.getString("account_email", "") ?: ""
                prefs.edit().putString("tracking_key", key).putString("tracking_key_email", accountEmail).apply()
                runOnUiThread { keyView.text = "SECRET KEY (copy it now and keep private):\n$key"; setStatus("Phone enrolled for $accountEmail. Save the key somewhere safe; the server will not show it again.") }
            } catch (e: Exception) { runOnUiThread { setStatus("Enrollment failed: " + (e.message ?: "network error")) } }
        }
    }

    private fun startSharing() {
        val key = prefs.getString("tracking_key", "") ?: ""
        val keyAccount = prefs.getString("tracking_key_email", "") ?: ""
        val signedInEmail = prefs.getString("account_email", "") ?: ""
        if (key.isBlank() || keyAccount.isBlank() || !keyAccount.equals(signedInEmail, ignoreCase = true)) {
            setStatus("Log in with the account that owns this phone key, then enroll this phone.")
            return
        }
        saveConfig()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 41)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
        ContextCompat.startForegroundService(this, Intent(this, LocationShareService::class.java))
        setStatus("Location sharing started. A persistent notification is shown while it runs.")
    }

    private fun saveConfig() {
        val url = base()
        prefs.edit().putString("project_url", url).putString("endpoint", url + "/functions/v1/v1-tracker")
            .putString("anon_key", anon.text.toString().trim()).putString("device_name", deviceName.text.toString().trim()).apply()
    }

    private fun requestJson(url: String, body: JSONObject, publicKey: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"; conn.connectTimeout = 15000; conn.readTimeout = 15000; conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("apikey", publicKey)
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (conn.responseCode !in 200..299) throw Exception(JSONObject(text).optJSONObject("msg")?.toString() ?: JSONObject(text).optString("msg", JSONObject(text).optString("error_description", "request rejected")))
        return JSONObject(text)
    }

    private fun setStatus(text: String) { if (::status.isInitialized) status.text = text }
}