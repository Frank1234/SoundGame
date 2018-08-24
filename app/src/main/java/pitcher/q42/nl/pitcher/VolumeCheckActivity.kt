package pitcher.q42.nl.pitcher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.SilenceDetector

var avarageBackgroundVolume: Double? = null
var avarageForegroundVolume: Double? = null

class VolumeCheckActivity : BaseSoundActivity() {

    enum class State {
        STATE_INITIAL,
        STATE_RECORD_BACKGROUND_NOISE, // show message: please be quiet for 3...2...1. That's the background noise.
        STATE_RECORD_FOREGROUND, // then message: please play 5 notes slowly. X more to go...
        STATE_DONE // thank you
    }

    data class Recording(val volume: Double, val startTs: Long) {
        val rangeToBeSameVolume = 2
        fun isRoughlyTheSame(otherVolume: Double) = Math.abs(volume - otherVolume) < rangeToBeSameVolume
    }

    val notesToRecord = 6
    var state: State = State.STATE_INITIAL

    /**
     * Volumes of which we think it's background noise.
     */
    val receivedBackgroundVolumes = mutableListOf<Double>(SilenceDetector.DEFAULT_SILENCE_THRESHOLD)

    /**
     * Start DB of which a sound qualifies as "foreground" noise, compared to the background noise.
     */
    var rangeToDetectForeground = 10
    /**
     * range to stop detecting the foreground note when we are already tracking one.
     */
    var rangeToStopDetectForeground = 20

    var foregroundNoteBeingTracked: Recording? = null
    val recordedForegroundNotes = mutableListOf<Recording>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume)

        avarageBackgroundVolume = null
        avarageForegroundVolume = null

        updateView()
    }

    override fun onPitchEvent(pitchInHz: Float) {
    }

    fun changeState(state: State) {
        this.state = state
        updateView()
    }

    fun updateView() = runOnUiThread {
        val button = findViewById<TextView>(R.id.start_button)
        val messageTv = findViewById<TextView>(R.id.vol_tv)

        when (state) {
            State.STATE_INITIAL -> {
                button.visibility = View.VISIBLE
                button.text = "Start"
                messageTv.visibility = View.GONE
                button.setOnClickListener { changeState(State.STATE_RECORD_BACKGROUND_NOISE) }
            }
            State.STATE_RECORD_BACKGROUND_NOISE -> {
                button.visibility = View.GONE
                messageTv.visibility = View.VISIBLE
                var x = 4
                var runnable: Runnable? = null
                runnable = Runnable {
                    x--
                    if (x >= 1) {
                        messageTv.setText("Please be quiet for... $x")
                        messageTv.postDelayed(runnable, 1000)
                    } else {
                        Log.d("SSS", "AVARAGE BACKGROUND IS $avarageBackgroundVolume")
                        changeState(State.STATE_RECORD_FOREGROUND)
                    }
                }
                messageTv.postDelayed(runnable, 1000)
            }
            State.STATE_RECORD_FOREGROUND -> {
                button.visibility = View.GONE
                messageTv.visibility = View.VISIBLE
                messageTv.setText("Volume check\n\nplease play six different notes...\n\n${notesToRecord - recordedForegroundNotes.size} to go!")
            }
            State.STATE_DONE -> {

                avarageBackgroundVolume = receivedBackgroundVolumes.average()
                avarageForegroundVolume = recordedForegroundNotes.map { it.volume }.average()

                button.visibility = View.VISIBLE
                button.text = "Continue"
                button.setOnClickListener { finish() }
                messageTv.visibility = View.VISIBLE
                messageTv.setText("Thank you!\n\nAvBack: $avarageBackgroundVolume \n\nAvFor: $avarageForegroundVolume")
            }
        }
    }

    override fun onSoundEvent(audioEvent: AudioEvent, currentSPL: Double) {
        when (state) {
            State.STATE_RECORD_BACKGROUND_NOISE -> recordBackgroundVolume(currentSPL)
            State.STATE_RECORD_FOREGROUND -> recordForegroundVolume(audioEvent, currentSPL)
            else -> {
            }
        }
        audioEvent.frameLength
    }

    fun recordBackgroundVolume(currentSPL: Double) {

        receivedBackgroundVolumes.add(currentSPL)
    }

    fun recordForegroundVolume(audioEvent: AudioEvent, currentVolume: Double) {

        Log.d("Sound1", "Sound detected of ${currentVolume}dB SPL")

        val backgroundAvarage = receivedBackgroundVolumes.average()
        val lastOne = foregroundNoteBeingTracked

        if (currentVolume - backgroundAvarage >= rangeToDetectForeground) {
            // there is quite a volume change compared to the background noise:

            if (lastOne == null) { //  || !lastOne.isRoughlyTheSame(currentVolume)
                // start tracking this note/sound:
                foregroundNoteBeingTracked = Recording(currentVolume, System.currentTimeMillis())
                Log.d("SSS", "start tracking this note/sound $foregroundNoteBeingTracked")
            } else if (System.currentTimeMillis() - lastOne.startTs > 250
                    && !(recordedForegroundNotes.lastOrNull() == lastOne)) {
                // played for quite some time, this must be a note.
                Log.d("SSS", "played for quite some time, this must be a note. ${System.currentTimeMillis() - lastOne.startTs}")
                foregroundNoteBeingTracked?.let { recordedForegroundNotes.add(it) }
                if (recordedForegroundNotes.size >= notesToRecord) changeState(State.STATE_DONE)
                else updateView()
            } else {
                // continue last recording, it is the same and hasn't played long enough or we already saved it.
            }
        } else if (currentVolume - backgroundAvarage <= rangeToStopDetectForeground) {
            // sound is no longer loud enough, quit tracking the current note if there is one:
            foregroundNoteBeingTracked?.let { Log.d("SSS", "sound is no longer loud enough, quit tracking the current note if there is one: $foregroundNoteBeingTracked") }
            foregroundNoteBeingTracked = null
        }
    }
}
