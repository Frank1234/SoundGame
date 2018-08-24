package pitcher.q42.nl.pitcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import be.tarsos.dsp.SilenceDetector

const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 42

abstract class BaseSoundActivity : AppCompatActivity() {

    val volumeThreshold = SilenceDetector.DEFAULT_SILENCE_THRESHOLD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            initSoundHandlers()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initSoundHandlers()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initSoundHandlers() {
        initVolumeHandler()
        initPitchHandler()
    }

    private fun initVolumeHandler() {

        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        val silenceDetector = SilenceDetector(volumeThreshold, false)

        dispatcher.addAudioProcessor(silenceDetector) // TODO what does this do?

        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun processingFinished() {
            }

            override fun process(audioEvent: AudioEvent?): Boolean {
//                if (silenceDetector.currentSPL() > volumeThreshold) {
//                Log.d("Sound1", "Sound detected of ${silenceDetector.currentSPL()}dB SPL")
                audioEvent?.let { onSoundEvent(it, silenceDetector.currentSPL()) }
//                } else {
////                    Log.d("Sound2", ".......silent sound detected of ${silenceDetector.currentSPL()}dB SPL")
//                }
                return true
            }
        })

        Thread(dispatcher, "Audio dispatching sound events / volume").start() // TODO find better way
    }

    private fun initPitchHandler() {

        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        val pitchHandler = PitchDetectionHandler { result, event ->
            onPitchEvent(result.pitch)
        }

        val pitchProcessor = PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        Thread(dispatcher, "Audio Dispatcher pitch").start() // TODO find better way
    }

    abstract fun onPitchEvent(pitchInHz: Float)

    abstract fun onSoundEvent(audioEvent: AudioEvent, currentSPL: Double)
}
