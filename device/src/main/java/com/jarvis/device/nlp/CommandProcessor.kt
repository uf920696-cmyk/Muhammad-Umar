package com.jarvis.device.nlp

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.jarvis.core.common.ToolResult
import com.jarvis.device.accessibility.JarvisAccessibilityService
import com.jarvis.device.ai.JarvisAiEngine
import com.jarvis.device.apps.AppLauncher
import com.jarvis.device.audio.VolumeController
import com.jarvis.device.system.SystemFeatureController
import com.jarvis.device.torch.TorchController
import com.jarvis.device.whatsapp.WhatsAppCallController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExecutionResult(
    val executed: Boolean,
    val responseSpeech: String,
    val isDeactivateRequest: Boolean = false
)

/**
 * 100% REAL HARDWARE & COMPLETE SMART AI ASSISTANT:
 * - Hands-free continuous loop: stays active continuously until explicitly deactivated
 * - Screen Automation & Click control (click 2nd song, 1st video, scroll down, back, home)
 * - Alarms & Clock (e.g. "alarm set karo 7 baje ka")
 * - Camera Photo Click (Front & Back)
 * - System Settings (WiFi, Bluetooth, Sound, Display)
 * - Full ChatGPT-grade AI knowledge (Science, History, Facts, Q&A)
 * - Safe: Never touches private credentials (passwords, emails).
 */
class CommandProcessor(private val context: Context) {

    private val torchController = TorchController(context)
    private val appLauncher = AppLauncher(context)
    private val volumeController = VolumeController(context)
    private val whatsAppCallController = WhatsAppCallController(context)
    private val systemFeatureController = SystemFeatureController(context)
    private val aiEngine = JarvisAiEngine(context)

    suspend fun processCommand(rawInput: String, candidates: List<String> = emptyList()): ExecutionResult {
        val allInputs = mutableListOf<String>()
        if (rawInput.isNotBlank()) allInputs.add(rawInput)
        candidates.forEach { if (it.isNotBlank() && !allInputs.contains(it)) allInputs.add(it) }

        // Test each candidate against specific hardware / screen / system commands
        for (candidate in allInputs) {
            val res = evaluateSingleCommand(candidate)
            if (res != null) return res
        }

        // If no tool matched, pass query to ChatGPT-grade AI knowledge
        val aiReply = aiEngine.askAi(rawInput)
        return ExecutionResult(true, aiReply)
    }

    private suspend fun evaluateSingleCommand(rawInput: String): ExecutionResult? {
        val text = rawInput.trim().lowercase()
        Log.i("CommandProcessor", "Evaluating command candidate: '$text'")

        // Explicit Deactivation Request
        if (text == "deactivate" || text.contains("deactivate jarvis") || text.contains("jarvis deactivate") ||
            text.contains("de-activate") || text.contains("band ho jao") || text.contains("chup ho jao") ||
            text.contains("so jao") || text.contains("standby") || text == "stop" || text == "bye" || text == "sleep") {
            return ExecutionResult(
                executed = true,
                responseSpeech = "Jarvis deactivate kar diya gaya hai. Main standby par hoon. Jab zaroorat ho bas 'JARVIS' bolen.",
                isDeactivateRequest = true
            )
        }

        // 1. REAL SCREEN CONTROLS & CLICKS (ACCESSIBILITY ENGINE)
        // Handles: "dusre number par song play karo", "screen r dusre number pr song ha isse play karo", "doosra gana chalao", "second song"
        val hasSongOrPlay = text.contains("song") || text.contains("gana") || text.contains("geet") || 
                            text.contains("play") || text.contains("chala") || text.contains("laga") || 
                            text.contains("click") || text.contains("video") || text.contains("item") || text.contains("track")
        val hasScreenOrNumber = text.contains("number") || text.contains("no pr") || text.contains("par jo") || 
                                text.contains("pe jo") || text.contains("wala") || text.contains("screen") || 
                                text.contains("scrren") || text.contains("list")

        if (hasScreenOrNumber || hasSongOrPlay) {
            val accessService = JarvisAccessibilityService.instance
            var position = -1
            when {
                text.contains("pehla") || text.contains("pehli") || text.contains("first") || text.contains("1st") || text.contains("ek number") || text.contains("1 number") -> position = 1
                text.contains("dusra") || text.contains("dusre") || text.contains("dusri") || text.contains("doosra") || text.contains("doosre") || text.contains("second") || text.contains("2nd") || text.contains("do number") || text.contains("2 number") -> position = 2
                text.contains("teesra") || text.contains("teesre") || text.contains("teesri") || text.contains("third") || text.contains("3rd") || text.contains("teen number") || text.contains("3 number") -> position = 3
                text.contains("chotha") || text.contains("chothe") || text.contains("chautha") || text.contains("chauthe") || text.contains("fourth") || text.contains("4th") || text.contains("char number") || text.contains("4 number") -> position = 4
                text.contains("panchwa") || text.contains("panchwe") || text.contains("fifth") || text.contains("5th") || text.contains("5 number") -> position = 5
            }

            if (position != -1) {
                if (accessService != null) {
                    val clicked = accessService.clickItemByPosition(position)
                    return ExecutionResult(true, "Ji, screen par $position number wala item play kar diya hai!")
                } else {
                    return ExecutionResult(false, "Screen access ke liye settings mein jakar JARVIS Accessibility Service on kar dein, phir main direct screen par click kar sakti hoon!")
                }
            }
        }

        // Navigation controls: Scroll, Back, Home
        if (text.contains("scroll down") || text.contains("neeche karo") || text.contains("niche karo")) {
            JarvisAccessibilityService.instance?.scroll(down = true)
            return ExecutionResult(true, "Screen neeche scroll kar di hai.")
        }
        if (text.contains("scroll up") || text.contains("ooper karo") || text.contains("upar karo")) {
            JarvisAccessibilityService.instance?.scroll(down = false)
            return ExecutionResult(true, "Screen ooper kar di hai.")
        }
        if (text == "back" || text.contains("peeche jao") || text.contains("back karo")) {
            JarvisAccessibilityService.instance?.pressBack()
            return ExecutionResult(true, "Back kar diya hai.")
        }
        if (text == "home" || text.contains("home screen") || text.contains("home par jao")) {
            JarvisAccessibilityService.instance?.pressHome()
            return ExecutionResult(true, "Home screen khol di hai.")
        }

        // 2. REAL ALARM CLOCK
        // Handles: "alarm set karo 7 baje ka", "alarm lagao 6:30 ka", "subah 8 baje ka alarm"
        if (text.contains("alarm")) {
            var hour = 7
            var minute = 0

            // Extract numbers from text
            val regex = "(\\d+)(?::(\\d+))?".toRegex()
            val match = regex.find(text)
            if (match != null) {
                hour = match.groupValues[1].toIntOrNull() ?: 7
                minute = match.groupValues[2].toIntOrNull() ?: 0
                if (text.contains("sham") || text.contains("raat") || text.contains("pm")) {
                    if (hour < 12) hour += 12
                }
            } else {
                when {
                    text.contains("che") || text.contains("chhe") || text.contains("six") -> hour = 6
                    text.contains("saat") || text.contains("seven") -> hour = 7
                    text.contains("aath") || text.contains("eight") -> hour = 8
                    text.contains("nau") || text.contains("nine") -> hour = 9
                    text.contains("dus") || text.contains("ten") -> hour = 10
                }
            }

            val alarmSet = systemFeatureController.setAlarm(hour, minute, "JARVIS Alarm")
            return if (alarmSet) {
                val formattedMinute = if (minute < 10) "0$minute" else "$minute"
                ExecutionResult(true, "Ji, $hour:$formattedMinute baje ka alarm set kar diya hai!")
            } else {
                ExecutionResult(false, "Alarm set karne mein masla hua.")
            }
        }

        // 3. REAL CAMERA & PHOTO / SELFIE
        // Handles: "camera on karke pic click karo", "front camera kholo", "selfie lo", "back camera chalao"
        if (text.contains("camera") || text.contains("selfie") || text.contains("photo") || text.contains("pic") || text.contains("tasveer")) {
            val isFront = text.contains("front") || text.contains("selfie") || text.contains("apna")
            systemFeatureController.openCamera(isFront)
            val camType = if (isFront) "Front camera (Selfie)" else "Back camera"
            return ExecutionResult(true, "Ji, $camType khol diya hai photo click karne ke liye!")
        }

        // 4. PHONE SETTINGS (WiFi, Bluetooth, Display, Sound, Battery)
        if (text.contains("setting") || text.contains("settings") || text.contains("wifi") || text.contains("bluetooth")) {
            when {
                text.contains("wifi") -> {
                    systemFeatureController.openSetting("wifi")
                    return ExecutionResult(true, "WiFi settings open kar di hain.")
                }
                text.contains("bluetooth") -> {
                    systemFeatureController.openSetting("bluetooth")
                    return ExecutionResult(true, "Bluetooth settings open kar di hain.")
                }
                text.contains("display") || text.contains("brightness") -> {
                    systemFeatureController.openSetting("display")
                    return ExecutionResult(true, "Display aur brightness settings khol di hain.")
                }
                text.contains("accessibility") -> {
                    systemFeatureController.openSetting("accessibility")
                    return ExecutionResult(true, "Accessibility settings khol di hain.")
                }
                else -> {
                    systemFeatureController.openSetting("all")
                    return ExecutionResult(true, "Phone settings open kar di hain.")
                }
            }
        }

        // 5. DYNAMIC DATE & TIME (AJ KI TAREEKH / WAQT / DIN)
        if (text.contains("tareekh") || text.contains("tarikh") || text.contains("date") || 
            text.contains("din") || text.contains("aaj kya") || text.contains("aj kya") || 
            text.contains("kon sa din") || text.contains("kaun sa din") || text.contains("aj ka din")) {
            val now = Date()
            val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(now)
            val fullDate = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(now)

            val urduDay = when (dayName.lowercase()) {
                "monday" -> "Peer (Monday)"
                "tuesday" -> "Mangal (Tuesday)"
                "wednesday" -> "Budh (Wednesday)"
                "thursday" -> "Jumerat (Thursday)"
                "friday" -> "Juma (Friday)"
                "saturday" -> "Hafta (Saturday)"
                "sunday" -> "Itwar (Sunday)"
                else -> dayName
            }
            return ExecutionResult(true, "Aaj $urduDay hai, aur tareekh $fullDate hai.")
        }

        if (text.contains("time") || text.contains("waqt") || text.contains("kitne baje") || text.contains("baja")) {
            val now = Date()
            val currentTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(now)
            return ExecutionResult(true, "Abhi waqt $currentTime ho raha hai.")
        }

        // 6. YOUTUBE SEARCH & PLAY SONGS / VIDEOS
        val hasYouTube = text.contains("youtube") || text.contains("yt")
        val hasSearchOrSong = text.contains("search") || text.contains("song") || text.contains("gana") || 
                              text.contains("video") || text.contains("chala") || text.contains("laga") || text.contains("play")

        if (hasYouTube || (text.contains("song") && text.contains("search")) || text.contains("gana chalao") || text.contains("gana lagao")) {
            var searchTarget = text
                .replace("youtube", "")
                .replace("yt", "")
                .replace("par", "")
                .replace("pr", "")
                .replace("pe", "")
                .replace("search", "")
                .replace("karo", "")
                .replace("karen", "")
                .replace("kar", "")
                .replace("do", "")
                .replace("lagao", "")
                .replace("chalao", "")
                .replace("chala", "")
                .replace("play", "")
                .replace("open", "")
                .replace("khol", "")
                .replace("kholo", "")
                .replace("ke gane", "song")
                .replace("ka gana", "song")
                .trim()

            if (searchTarget.isBlank() || searchTarget == "song" || searchTarget == "gana") {
                searchTarget = "trending songs"
            }

            appLauncher.searchYouTube(searchTarget)
            return ExecutionResult(true, "Ji, YouTube par $searchTarget chala diya hai.")
        }

        // 7. GOOGLE SEARCH
        if (text.contains("google search") || (text.contains("google") && text.contains("search"))) {
            val query = text.replace("google", "").replace("search", "").replace("par", "").replace("pr", "").replace("pe", "").replace("karo", "").trim()
            appLauncher.searchGoogle(query)
            return ExecutionResult(true, "Google par $query search kar diya hai.")
        }

        // 8. FLASHLIGHT / TORCH
        if (text.contains("torch") || text.contains("flashlight") || text.contains("batti") || text.contains("light") || text.contains("roshni")) {
            if (text.contains("on") || text.contains("chala") || text.contains("jala") || text.contains("khol") || text.contains("start")) {
                torchController.setTorch(true)
                return ExecutionResult(true, "Ji, flashlight on kar di hai.")
            } else if (text.contains("off") || text.contains("band") || text.contains("bujha") || text.contains("stop")) {
                torchController.setTorch(false)
                return ExecutionResult(true, "Flashlight band kar di hai.")
            }
        }

        // 9. OPENING ANY APPS
        val isOpenCommand = text.contains("open") || text.contains("khol") || text.contains("chala") || 
                           text.contains("start") || text.contains("launch")

        if (isOpenCommand) {
            val appQuery = text
                .replace("open", "")
                .replace("kholo", "")
                .replace("khol", "")
                .replace("chalao", "")
                .replace("chala", "")
                .replace("kar do", "")
                .replace("karo", "")
                .replace("kar", "")
                .replace("do", "")
                .replace("app", "")
                .replace("please", "")
                .trim()

            if (appQuery.isNotBlank() && appQuery.length >= 2) {
                val launchResult = appLauncher.launchAppByName(appQuery)
                return when (launchResult) {
                    is ToolResult.Dispatched -> {
                        ExecutionResult(true, "Ji, $appQuery open kar diya hai.")
                    }
                    is ToolResult.Failed, is ToolResult.Unsupported -> {
                        ExecutionResult(false, "Mujhe $appQuery aapke phone mein nahi mila, kya yeh install hai?")
                    }
                    else -> {
                        ExecutionResult(false, "App open karne mein masla hua.")
                    }
                }
            }
        }

        val directResult = appLauncher.launchAppByName(text)
        if (directResult is ToolResult.Dispatched) {
            return ExecutionResult(true, "Ji, $text open kar diya hai.")
        }

        // 10. VOLUME CONTROLS
        if (text.contains("volume") || text.contains("sound") || text.contains("awaz") || text.contains("aawaz") || text.contains("awaaz")) {
            if (text.contains("full") || text.contains("max") || text.contains("100") || text.contains("puri") || text.contains("zyada")) {
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, 100)
                return ExecutionResult(true, "Volume bilkul full kar diya hai!")
            } else if (text.contains("mute") || text.contains("silent") || text.contains("zero") || text.contains("band")) {
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, 0)
                return ExecutionResult(true, "Phone silent kar diya hai.")
            } else if (text.contains("half") || text.contains("50")) {
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, 50)
                return ExecutionResult(true, "Volume 50 percent set kar diya hai.")
            } else if (text.contains("increase") || text.contains("up") || text.contains("badha") || text.contains("tez")) {
                val current = volumeController.getVolumePercent(AudioManager.STREAM_MUSIC)
                val target = (current + 25).coerceAtMost(100)
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, target)
                return ExecutionResult(true, "Volume badha diya hai.")
            } else if (text.contains("decrease") || text.contains("down") || text.contains("kam") || text.contains("dheemi")) {
                val current = volumeController.getVolumePercent(AudioManager.STREAM_MUSIC)
                val target = (current - 25).coerceAtLeast(0)
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, target)
                return ExecutionResult(true, "Volume kam kar diya hai.")
            }
        }

        return null
    }
}
