package pitcher.q42.nl.pitcher

class NoteController() {

    fun getNote(pitchInHz: Float) = Note.values().filter { pitchInHz in it.range }.firstOrNull()
}