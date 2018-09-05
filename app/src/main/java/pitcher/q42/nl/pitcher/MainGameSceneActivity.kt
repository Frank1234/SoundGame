package pitcher.q42.nl.pitcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.android.synthetic.main.main_scene.*
import android.widget.RelativeLayout




class MainGameSceneActivity : AppCompatActivity() {

    var notesArray = listOf(Note.A, Note.B, Note.C, Note.D, Note.G)
    var currentIndex = 0

    val noteController = NoteController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_scene)

        tryInitPitchHandler()

        current_note_tv.text = notesArray[currentIndex].label
    }

    private fun tryInitPitchHandler() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
        } else {
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

        val pitchProcessor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        Thread(dispatcher, "Audio Dispatcher").start() // TODO find better way
    }

    fun processPitch(pitchInHz: Float) {
        Log.d("processPitch", "processPitch $pitchInHz")
        noteController.getNote(pitchInHz)?.let { note ->
            if (note == notesArray[currentIndex]) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    val layoutParams = current_note_tv.getLayoutParams() as ConstraintLayout.LayoutParams
                    layoutParams.topMargin =  layoutParams.topMargin + 30
                    current_note_tv.setLayoutParams(layoutParams)

                    updateForNextNote()

                })
            }
        }
    }

    fun updateForNextNote() {
        if ( currentIndex < notesArray.size - 1) {
            currentIndex++
            val layoutParams = current_note_tv.getLayoutParams() as  ConstraintLayout.LayoutParams
            layoutParams.topMargin =  layoutParams.topMargin - 30
            current_note_tv.setLayoutParams(layoutParams)
            current_note_tv.text = notesArray[currentIndex].label
        }
    }

}
