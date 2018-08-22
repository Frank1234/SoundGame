package pitcher.q42.nl.pitcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlinx.android.synthetic.main.activity_main.*

const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 42

class MainActivity : AppCompatActivity() {

    val noteController = NoteController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tryInitPitchHandler()
    }

    private fun tryInitPitchHandler() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
        } else {
            // granted already
            initPitchHandler()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initPitchHandler()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initPitchHandler() {

        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        val pitchHandler = PitchDetectionHandler { result, event ->
            processPitch(result.pitch)
        }

        val pitchProcessor = PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        Thread(dispatcher, "Audio Dispatcher").start() // TODO find better way
    }

    fun processPitch(pitchInHz: Float) {
        Log.d("processPitch", "processPitch $pitchInHz")
        noteController.getNote(pitchInHz)?.let { note ->
            runOnUiThread { main_tv.text = note.label }
        }
    }

}
