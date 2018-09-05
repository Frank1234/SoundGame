package pitcher.q42.nl.pitcher

import android.util.Log

class NoteDetector(val silenceVolume: Double, val noteListener: (DetectedNote) -> Unit) { // TODO add foreground volume, if we know it

    // TODO: a 500 length note will now be counted as 2 notes

    companion object {
        val EVENT_LIVETIME = 550L
        /**
         * Average volume must be higher then this + the silence.
         */
        val REQUIRED_VOLUME_DELTA_FROM_SILENCE = 10
        /**
         * Lowest recorded volume must be higher then avarage - this.
         */
        val REQUIRED_VOLUME_MAX_LOW = 6

        val MAX_ALLOWED_VARIANTION_PITH = 3.0f
    }

    private data class Event(val volume: Double, val pitch: Float, val timeStamp: Long, var duration: Long = 0L)
    data class DetectedNote(val avarageVolume: Double, val avaragePitch: Double, val timeStamp: Long, var duration: Long = 0L)

    private val events = mutableListOf<Event>()

    fun onNewAudioEvent(volume: Double, pitch: Float, timeStamp: Long) {

        // set duration on last event:
        events.lastOrNull()?.apply {
            duration = timeStamp - this.timeStamp
        }
        // add the new event:
        events.add(Event(volume, pitch, timeStamp))
//        Log.d("AAA", "SSS PITCH ${pitch} , VOL " + volume)

        // check if we have enough data for a check:
        if (events.first().timeStamp < timeStamp - EVENT_LIVETIME) {

            // remove events that are too old
            cleanUpOldEvents(timeStamp)

            // run the check on the current list
            if (events.size > 0) {
                if (events.size > 2 ) {
                    checkForNote()
                } else {
                    //TODO Only one note detected for the last EVENT_LIVETIME
                }
            }
        }
    }

    fun checkForNote() {

        if (!checkAvaragePitch()) {
            return
        }
        val averageVolume = getAverageVolume()
        if (!checkAverageVolume(averageVolume)) {
            return
        }
        if (!checkNoVolumeDrops(averageVolume)) {
            return
        }

        noteListener(DetectedNote(averageVolume, events.map { it.pitch }.average(), events.first().timeStamp, 250L))
    }

    fun checkAvaragePitch(): Boolean {
        var currentEventList = events.toMutableList()
        currentEventList.removeAt(currentEventList.size - 1)
        val averageWeight = currentEventList.map { it.pitch * it.duration }.average()
        val avarageDurationPerItem =  currentEventList.map { it.duration }.sum() / currentEventList.size
        val firstEvent = currentEventList.first()
        val firstEventWeight = firstEvent.pitch * avarageDurationPerItem

        Log.d("DIFF", "AVG PITCH ${averageWeight} , First Pitch " + firstEventWeight)
        return (averageWeight < firstEventWeight + (MAX_ALLOWED_VARIANTION_PITH * avarageDurationPerItem) &&
                averageWeight > firstEventWeight - (MAX_ALLOWED_VARIANTION_PITH * avarageDurationPerItem))
    }

    /**
     * Checks if average volume is in foreground range.
     */
    fun checkAverageVolume(averageVolume: Double): Boolean = averageVolume > silenceVolume + REQUIRED_VOLUME_DELTA_FROM_SILENCE

    fun checkNoVolumeDrops(averageVolume: Double): Boolean = events.map { it.volume }.min()?.let { it >= averageVolume - REQUIRED_VOLUME_MAX_LOW }
            ?: false

    fun getAverageVolume() = events.map { it.volume }.average()

    /**
     * Removes old (not needed anymore) events from the list.
     */
    fun cleanUpOldEvents(currentTime: Long) {
        events.removeIf { it.timeStamp < currentTime - EVENT_LIVETIME }
    }
}