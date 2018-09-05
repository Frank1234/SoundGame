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
var foregroundTestRecordings: List<Recording> = listOf()

class VolumeCheckActivity : BaseSoundActivity() {

    companion object {
        /**
         * Start DB of which a sound qualifies as "foreground" noise, compared to the background noise.
         */
        val RANGE_TO_START_DETECT_FOREGROUND = 10
        /**
         * Minimum time we say a note should last.
         */
        val MIN_NOTE_LENGTH_MS = 350
        /**
         * range to stop detecting the foreground (relative to the first recorded sound).
         */
        var VOLUME_RANGE_TO_STOP_DETECT_FOREGROUND = 5
        /**
         * Maximum pitch a note may change for us to still regard it as a single note.
         */
        val MAX_DELTA_PITCH_FOR_NOTE = 4
    }

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

    var foregroundNoteBeingTracked: Recording? = null
    val recordedForegroundNotes = mutableListOf<Recording>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume)

        avarageBackgroundVolume = null
        avarageForegroundVolume = null
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

                avarageForegroundVolume = recordedForegroundNotes.map { it.volume }.average()
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

    fun recordForegroundVolume(audioEvent: AudioEvent, currentVolume: Double, currentPitchInHz: Float) {

        Log.d("SSSS", "SSSS Sound detected of ${currentVolume}dB SPL with pitch $currentPitchInHz")

        val backgroundAvarage = receivedBackgroundVolumes.average()
        val lastOne = foregroundNoteBeingTracked

        val isForegroundVolume = Math.abs(currentVolume - backgroundAvarage) >= RANGE_TO_START_DETECT_FOREGROUND
        val isSameVolumeAsCurrentNote = lastOne != null && Math.abs(currentVolume - lastOne.volumes[0]) <= VOLUME_RANGE_TO_STOP_DETECT_FOREGROUND
        val isSamePitchAsCurrent = lastOne != null && Math.abs(currentPitchInHz - lastOne.pitches[0]) <= MAX_DELTA_PITCH_FOR_NOTE

        if (isForegroundVolume) {
            // there is quite a volume change compared to the background noise:

            if (lastOne == null) { //  || !lastOne.isRoughlyTheSame(currentVolume)
                // start tracking this note/sound:
                foregroundNoteBeingTracked = Recording(currentVolume, System.currentTimeMillis(), currentPitchInHz)
                foregroundNoteBeingTracked?.volumes?.add(currentVolume)
                foregroundNoteBeingTracked?.pitches?.add(currentPitchInHz)
                Log.d("SSSS", "start tracking this note/sound $foregroundNoteBeingTracked")
            } else {

                if (!isSameVolumeAsCurrentNote || !isSamePitchAsCurrent) {
                    Log.d("SSS", "STOPPED vol $isSameVolumeAsCurrentNote pitch $isSamePitchAsCurrent")
                    foregroundNoteBeingTracked = null
                } else if (System.currentTimeMillis() - lastOne.startTs > MIN_NOTE_LENGTH_MS
                        && !(recordedForegroundNotes.lastOrNull() == lastOne)) {
                    // played for quite some time, this must be a note.
                    Log.d("SSSS", "played for quite some time, this must be a note. ${System.currentTimeMillis() - lastOne.startTs}")
                    foregroundNoteBeingTracked?.let { recordedForegroundNotes.add(it) }
                    if (recordedForegroundNotes.size >= notesToRecord) changeState(State.STATE_DONE)
                    else updateView()
                } else {
                    // continue last recording, it is the same and hasn't played long enough or we already saved it.
                    lastOne.volumes.add(currentVolume)
                    lastOne.pitches.add(currentPitchInHz)
                }
            }
        }
    }
}
