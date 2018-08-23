package pitcher.q42.nl.pitcher

import android.os.Bundle
import android.util.Log
import be.tarsos.dsp.AudioEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseSoundActivity() {

    val noteController = NoteController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onPitchEvent(pitchInHz: Float) {
        Log.d("onPitchEvent", "onPitchEvent $pitchInHz")
        noteController.getNote(pitchInHz)?.let { note ->
            runOnUiThread { main_tv.text = note.label }
        }
    }

    override fun onSoundEvent(audioEvent: AudioEvent, currentSPL: Double) {
    }

}
