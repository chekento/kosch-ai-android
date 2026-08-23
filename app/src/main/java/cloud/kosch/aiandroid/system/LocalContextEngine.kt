package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import cloud.kosch.aiandroid.model.ContextSnapshot
import cloud.kosch.aiandroid.model.SceneId
import java.time.LocalTime

class LocalContextEngine(private val context: Context) {
    fun snapshot(): ContextSnapshot {
        val time = LocalTime.now()
        val battery = batteryState()
        val network = hasNetwork()
        val personalAudio = hasPersonalAudioOutput()

        val suggestedScene = when {
            time.hour >= 22 || time.hour < 6 -> SceneId.EVENING
            personalAudio -> SceneId.STUDIO
            time.hour in 8..16 && network -> SceneId.WORK
            else -> SceneId.AI
        }

        val reasons = buildList {
            add("${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')} lokal")
            battery.percent?.let { add("Akku $it %${if (battery.charging) ", lädt" else ""}") }
            add(if (network) "Netz verfügbar" else "Offline")
            if (personalAudio) add("Persönliche Audioausgabe aktiv")
        }

        return ContextSnapshot(
            hour = time.hour,
            minute = time.minute,
            batteryPercent = battery.percent,
            isCharging = battery.charging,
            hasNetwork = network,
            hasPersonalAudioOutput = personalAudio,
            suggestedScene = suggestedScene,
            reasons = reasons,
        )
    }

    private fun batteryState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else null
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryState(percent, charging)
    }

    private fun hasNetwork(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun hasPersonalAudioOutput(): Boolean {
        val manager = context.getSystemService(AudioManager::class.java)
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            device.type in personalAudioTypes
        }
    }

    private data class BatteryState(
        val percent: Int?,
        val charging: Boolean,
    )

    private companion object {
        val personalAudioTypes = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
        )
    }
}
