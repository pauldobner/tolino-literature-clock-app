package com.example.literatureclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.literatureclock.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: QuoteRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var updateReceiver: BroadcastReceiver
    private lateinit var alarmManager: AlarmManager

    private var currentLang = "both" // en, de, both
    private var showClock = false
    private var useRootSuspend = false
    private var currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    private val orientations = intArrayOf(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide status bar and nav bar for immersive experience
        setFullscreen()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("literature_clock_prefs", Context.MODE_PRIVATE)
        loadSettings()

        repository = QuoteRepository(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Setup UI
        binding.btnSettings.setOnClickListener { showSettings() }
        
        // Load Data
        lifecycleScope.launch {
            binding.tvQuote.text = getString(R.string.loading)
            repository.loadQuotes()
            updateUI()
        }

        // Receiver for Alarm updates
        updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.literatureclock.UI_UPDATE_NEEDED") {
                    updateUI()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateReceiver, IntentFilter("com.example.literatureclock.UI_UPDATE_NEEDED"),
                Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, IntentFilter("com.example.literatureclock.UI_UPDATE_NEEDED"))
        }
        
        setFullscreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        unregisterReceiver(updateReceiver)
    }

    private fun setFullscreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun loadSettings() {
        currentLang = prefs.getString("language", "both") ?: "both"
        showClock = prefs.getBoolean("show_clock", false)
        useRootSuspend = prefs.getBoolean("root_suspend", false)
        currentOrientation = prefs.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        applySettings()
    }

    private fun applySettings() {
        binding.tvSmallClock.visibility = if (showClock) View.VISIBLE else View.GONE
        requestedOrientation = currentOrientation
    }

    private fun showSettings() {
        val popup = PopupMenu(this, binding.btnSettings)
        popup.menu.add(0, 1, 0, "Language: $currentLang")
        popup.menu.add(0, 2, 0, if (showClock) "Hide Clock" else "Show Clock")
        popup.menu.add(0, 3, 0, if (useRootSuspend) "Disable Root Suspend" else "Enable Root Suspend")
        popup.menu.add(0, 4, 0, "Rotate Screen")
        popup.menu.add(0, 5, 0, "Suspend Now")
        popup.menu.add(0, 7, 0, "Battery Log")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    currentLang = when (currentLang) {
                        "en" -> "de"
                        "de" -> "both"
                        else -> "en"
                    }
                    prefs.edit().putString("language", currentLang).apply()
                    updateUI()
                }
                2 -> {
                    showClock = !showClock
                    prefs.edit().putBoolean("show_clock", showClock).apply()
                    applySettings()
                    updateUI()
                }
                3 -> {
                    if (!useRootSuspend && !ShellUtils.hasRootAccess()) {
                        Toast.makeText(this, "Root access not detected!", Toast.LENGTH_SHORT).show()
                    } else {
                        useRootSuspend = !useRootSuspend
                        prefs.edit().putBoolean("root_suspend", useRootSuspend).apply()
                        if (useRootSuspend) {
                            Toast.makeText(this, "Root Suspend Enabled.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                4 -> {
                    val currentIndex = orientations.indexOf(currentOrientation)
                    val nextIndex = (currentIndex + 1) % orientations.size
                    currentOrientation = orientations[nextIndex]
                    prefs.edit().putInt("orientation", currentOrientation).apply()
                    applySettings()
                }
                5 -> {
                    if (ShellUtils.hasRootAccess()) {
                        Toast.makeText(this, "Suspending in 3 seconds...", Toast.LENGTH_SHORT).show()
                        binding.root.postDelayed({
                            performRootSuspend()
                        }, 3000)
                    } else {
                        Toast.makeText(this, "Root needed for suspend", Toast.LENGTH_SHORT).show()
                    }
                }
                7 -> {
                    showBatteryLog()
                }
            }
            true
        }
        popup.show()
    }

    private fun checkAndLogBattery() {
        try {
            val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                
                val lastLevel = prefs.getInt("last_battery_level", -1)
                val lastLogTime = prefs.getLong("last_battery_log_time", 0)
                val now = System.currentTimeMillis()
                
                // Log if: Level changed OR it's been more than 1 hour (3600000 ms)
                if (batteryPct != lastLevel || (now - lastLogTime) > 3600000) {
                    val logFile = java.io.File(filesDir, "battery_log.txt")
                    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
                    logFile.appendText("$timestamp: Bat $batteryPct%\n")
                    
                    prefs.edit()
                        .putInt("last_battery_level", batteryPct)
                        .putLong("last_battery_log_time", now)
                        .apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBatteryLog() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val logFile = java.io.File(filesDir, "battery_log.txt")
            val logContent = if (logFile.exists()) logFile.readText() else "Log is empty."
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val builder = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Battery Log")
                builder.setMessage(logContent)
                builder.setPositiveButton("Close", null)
                builder.setNegativeButton("Clear Log") { _, _ ->
                    logFile.writeText("")
                    Toast.makeText(this@MainActivity, "Log cleared", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            }
        }
    }

    private fun performRootSuspend() {
        // Clear screen flag to allow kernel suspend
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        Thread {
            // Give window manager a moment to process the flag change
            Thread.sleep(200)
            
            // 2. Construct optimized shell script
            // This combines all operations into one SU call to minimize overhead and awake time.
            // It turns off Wifi, unlocks system wakelocks, suspends to RAM,
            // and immediately upon waking (next line of script) restores locks.
            // We DO NOT touch the backlight, allowing it to stay on if the user desires.
            val suspendScript = """
                svc wifi disable
                echo PowerManagerService.Display > /sys/power/wake_unlock
                echo PowerManagerService.WakeLocks > /sys/power/wake_unlock
                echo mem > /sys/power/state
                echo PowerManagerService.Display > /sys/power/wake_lock
                echo PowerManagerService.WakeLocks > /sys/power/wake_lock
            """.trimIndent()

            ShellUtils.executeRootCommand(suspendScript)
            
            runOnUiThread {
                // Re-enable screen flag
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }.start()
    }

    private fun scheduleNextUpdate() {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.example.literatureclock.UPDATE_TIME"
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))

        // Calculate start of next minute
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MINUTE, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun updateUI() {
        checkAndLogBattery()

        if (!repository.isLoaded) return

        val now = Date()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = sdf.format(now)

        binding.tvSmallClock.text = timeStr

        val quote = repository.getQuote(timeStr, currentLang)

        if (quote != null) {
            // Highlight time phrase
            var html = quote.quote
            if (quote.quoteTimePhrase.isNotEmpty() && html.contains(quote.quoteTimePhrase)) {
                html = html.replaceFirst(quote.quoteTimePhrase, "<b><u>${quote.quoteTimePhrase}</u></b>")
            }
            
            html = html.replace("\n", "<br>")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.tvQuote.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                binding.tvQuote.text = Html.fromHtml(html)
            }
            binding.tvTitle.text = quote.title
            binding.tvAuthor.text = quote.author
            
            binding.tvAuthor.visibility = if (quote.author.isNotEmpty()) View.VISIBLE else View.GONE
        } else {
            binding.tvQuote.text = "Time is $timeStr.\nNo literary quote found for this exact minute."
            binding.tvTitle.text = ""
            binding.tvAuthor.visibility = View.GONE
        }

        // Always schedule next update
        scheduleNextUpdate()

        // If Root Suspend is enabled, force sleep after a short delay to allow UI to render
        if (useRootSuspend) {
            binding.root.postDelayed({
                performRootSuspend()
            }, 2000) // 2s delay to ensure screen refresh finishes
        }
    }
}
