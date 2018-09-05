package pitcher.q42.nl.pitcher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.SilenceDetector

data class Recording(val volume: Double,
                     val startTs: Long,
                     val startPitch: Float,
                     val volumes: MutableList<Double> = mutableListOf(),
                     val pitches: MutableList<Float> = mutableListOf())

var avarageBackgroundVolume: Double? = null
var avarageForegroundVolume: Double? = null
var foregroundTestRecordings: List<NoteDetector.DetectedNote> = listOf()

class VolumeCheckActivity : BaseSoundActivity() {

    enum class State {
        STATE_INITIAL,
        STATE_RECORD_BACKGROUND_NOISE, // show message: please be quiet for 3...2...1. That's the background noise.
        STATE_RECORD_FOREGROUND, // then message: please play 5 notes slowly. X more to go...
        STATE_DONE // thank you
    }

    val notesToRecord = 6
    var state: State = State.STATE_INITIAL

    /**
     * Volumes of which we think it's background noise.
     */
    val receivedBackgroundVolumes = mutableListOf<Double>(SilenceDetector.DEFAULT_SILENCE_THRESHOLD)

    var noteDetector: NoteDetector? = null
    val recordedForegroundNotes = mutableListOf<NoteDetector.DetectedNote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume)

        avarageBackgroundVolume = null
        avarageForegroundVolume = null
        noteDetector = null
        foregroundTestRecordings = listOf()

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
                messageTv.visibility = View.VISIBLE
                messageTv.text = "Go to the room where you want to play, grab your guitar and press start."
                button.setOnClickListener { changeState(State.STATE_RECORD_BACKGROUND_NOISE) }
            }
            State.STATE_RECORD_BACKGROUND_NOISE -> {
                button.visibility = View.GONE
                messageTv.visibility = View.INVISIBLE
                var x = 4
                var runnable: Runnable? = null
                runnable = Runnable {
                    x--
                    if (x >= 1) {
                        messageTv.visibility = View.VISIBLE
                        messageTv.setText("Recording background noise. Please be quiet for... $x")
                        messageTv.postDelayed(runnable, 1000)
                    } else {
                        avarageBackgroundVolume = receivedBackgroundVolumes.average()
                        noteDetector = NoteDetector(avarageBackgroundVolume!!, { onNoteDetected(it) })
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

                avarageForegroundVolume = recordedForegroundNotes.map { it.avarageVolume }.average()
                foregroundTestRecordings = recordedForegroundNotes

                button.visibility = View.VISIBLE
                button.text = "Continue"
                button.setOnClickListener { finish() }
                messageTv.visibility = View.VISIBLE
                messageTv.setText("Thank you!\n\nAvBack: $avarageBackgroundVolume \n\nAvFor: $avarageForegroundVolume")
            }
        }
    }

    override fun onSoundEvent(audioEvent: AudioEvent, currentSPL: Double, pitchInHz: Float) {
        when (state) {
            State.STATE_RECORD_BACKGROUND_NOISE -> recordBackgroundVolume(currentSPL)
            State.STATE_RECORD_FOREGROUND -> recordForegroundVolume(audioEvent, currentSPL, pitchInHz)
            else -> {
            }
        }
    }

    fun recordBackgroundVolume(currentSPL: Double) {

        receivedBackgroundVolumes.add(currentSPL)
    }

    fun onNoteDetected(detectedNote: NoteDetector.DetectedNote) {
        recordedForegroundNotes.add(detectedNote)
        updateView()
    }

    fun recordForegroundVolume(audioEvent: AudioEvent, volume: Double, pitch: Float) {

        noteDetector?.onNewAudioEvent(volume, pitch, System.currentTimeMillis())
    }
}
