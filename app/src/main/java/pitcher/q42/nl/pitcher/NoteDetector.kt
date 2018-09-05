package pitcher.q42.nl.pitcher

class NoteDetector(val silenceVolume: Double, val noteListener: (DetectedNote) -> Unit) { // TODO add foreground volume, if we know it

    // TODO: a 500 length note will now be counted as 2 notes

    companion object {
        val EVENT_LIVETIME = 250L
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

    data class Event(val volume: Double, val pitch: Float, val timeStamp: Double, var duration: Double = 1.toDouble())
    data class DetectedNote(val avarageVolume: Double, val avaragePitch: Double, val timeStamp: Double, var duration: Double = 1.toDouble())

    val events = mutableListOf<Event>()

    fun onNewAudioEvent(volume: Double, pitch: Float, timeStamp: Double) {

        // set duration on last event:
        events.lastOrNull()?.apply {
            duration = timeStamp - this.timeStamp
        }
        // add the new event:
        events.add(Event(volume, pitch, timeStamp))

        // check if we have enough data for a check:
        if (events.first().timeStamp < timeStamp - EVENT_LIVETIME) {

            // remove events that are too old
            cleanUpOldEvents(timeStamp)

            // run the check on the current list
            if (events.size > 0) {
                checkForNote()
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

        noteListener(DetectedNote(averageVolume, events.map { it.pitch }.average(), events.first().timeStamp, 250.0))
    }

    fun checkAvaragePitch(): Boolean {
        val averageWeight = events.map { it.pitch * it.duration }.average()
        val avarageDurationPerItem = EVENT_LIVETIME / events.size
        val firstEvent = events.first()
        val firstEventWeight = firstEvent.pitch * avarageDurationPerItem

        return (averageWeight < firstEventWeight + (MAX_ALLOWED_VARIANTION_PITH * avarageDurationPerItem) &&
                averageWeight > firstEventWeight - (MAX_ALLOWED_VARIANTION_PITH * avarageDurationPerItem))
    }

    /**
     * Checks if average volume is in foreground range.
     */
    fun checkAverageVolume(averageVolume: Double): Boolean = averageVolume > silenceVolume + REQUIRED_VOLUME_DELTA_FROM_SILENCE

    fun checkNoVolumeDrops(averageVolume: Double): Boolean = events.map { it.volume }.min()?.let { it >= averageVolume - REQUIRED_VOLUME_MAX_LOW }
            ?: false

    fun getAverageVolume() = events.map { it.pitch }.average()

    /**
     * Removes old (not needed anymore) events from the list.
     */
    fun cleanUpOldEvents(currentTime: Double) {
        events.removeIf { it.timeStamp < currentTime - EVENT_LIVETIME }
    }
}