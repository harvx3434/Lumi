package com.lumi.assistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.Executors

class LumiService : Service(), TextToSpeech.OnInitListener {
    companion object { var inst: LumiService? = null }

    private val main = Handler(Looper.getMainLooper())
    private val bg = Executors.newSingleThreadExecutor()
    private lateinit var tts: TextToSpeech
    private var sr: SpeechRecognizer? = null
    private var speaking = false
    private var awakeUntil = 0L          // "Lumi" bolne ke baad 25 sec tak bina naam ke sunegi
    private var context = ""             // aaya hua call/message ka context
    private var running = true

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate(); inst = this
        val ch = NotificationChannel("lumi", "Lumi", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n = Notification.Builder(this, "lumi").setContentTitle("Lumi sun rahi hai 💗")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        tts = TextToSpeech(this, this)
        NotifService.onMessage = { who, text ->
            context = "Naya message aaya. Bhejne wala: $who. Message: $text"
            main.post { say("Jaan, $who ka message aaya hai. Wo keh raha hai: $text. Kya jawab du?") }
        }
    }

    override fun onInit(s: Int) {
        tts.language = Locale("hi", "IN")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onError(id: String?) { speaking = false; listen() }
            override fun onDone(id: String?) { speaking = false; main.post { listen() } }
        })
        say("Hi jaan, main Lumi. Bolo kya karun?")
    }

    private fun say(t: String) {
        speaking = true; sr?.cancel()
        awakeUntil = System.currentTimeMillis() + 25_000
        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "u${System.nanoTime()}")
    }

    private fun listen() {
        if (!running || speaking) return
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this)
            sr!!.setRecognitionListener(object : RecognitionListener {
                override fun onResults(r: Bundle) {
                    val t = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    handle(t); if (!speaking) main.postDelayed({ listen() }, 300)
                }
                override fun onError(e: Int) { main.postDelayed({ listen() }, 600) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
        }
        sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        })
    }

    private fun handle(text: String) {
        if (text.isBlank()) return
        val called = text.contains("lumi", true) || text.contains("लुमी")
        if (!called && System.currentTimeMillis() > awakeUntil) return   // naam ke bina ignore
        val ctx = context; context = ""
        bg.execute {
            val out = Brain.think(text, ctx)
            val a = out.optJSONObject("action")
            var say = out.optString("say")
            val name = a?.optString("name") ?: ""; val msg = a?.optString("text") ?: ""
            val ok = when (a?.optString("type")) {
                "call" -> PhoneActions.call(this, name)
                "sms" -> PhoneActions.sms(this, name, msg)
                "reply" -> NotifService.reply(this, name, msg)
                "answer_call" -> { PhoneActions.answer(this); true }
                "reject_call" -> PhoneActions.reject(this)
                else -> true
            }
            if (!ok) say = "Jaan, ye nahi ho paya. Naam ya contact check karo na."
            main.post { say(say) }
        }
    }

    fun onIncomingCall(who: String) {
        context = "Incoming call aa rahi hai: $who"
        main.post { say("Jaan, $who ka call aa raha hai. Uthaun ya kaat du?") }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY

    override fun onDestroy() {
        running = false; inst = null; NotifService.onMessage = null
        sr?.destroy(); tts.shutdown(); super.onDestroy()
    }
}
