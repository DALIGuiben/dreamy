package com.dreamy.mobile.ui.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.ChildNote
import com.dreamy.mobile.domain.model.NoteInputMethod
import com.dreamy.mobile.domain.model.OrganizedSuggestions
import com.dreamy.mobile.navigation.ChildDetailRoute
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import com.dreamy.mobile.util.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

const val MAX_RECORDING_SECONDS = 90

enum class Busy { Transcription, Save, Organize, Approve, Delete }

enum class SuggestionField(val label: String) {
    Interests("Centres d’intérêt"),
    Comfort("Confort"),
    Gifts("Souhaits de cadeaux"),
    Themes("Thèmes d’anniversaire"),
}

/** State of the private notes panel (`components/notes.tsx`). */
data class NotesState(
    val content: String = "",
    val editingId: String? = null,
    val method: NoteInputMethod = NoteInputMethod.TEXT,
    val language: String = "fr",
    val recording: Boolean = false,
    val seconds: Int = 0,
    val audio: File? = null,
    val busy: Busy? = null,
    val error: String? = null,
    /** Action that produced [error]; profile deletion errors are shown next to the delete button. */
    val errorFrom: Busy? = null,
    /** AI suggestions being reviewed, as editable comma-separated text. */
    val suggestions: Map<SuggestionField, String>? = null,
)

@HiltViewModel
class ChildDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChildRepository,
    private val recorder: VoiceRecorder,
) : ViewModel() {
    private val childId = savedStateHandle.toRoute<ChildDetailRoute>().id

    private val _child = MutableStateFlow<Load<Child>>(Load.Loading)
    val child: StateFlow<Load<Child>> = _child.asStateFlow()

    private val _notes = MutableStateFlow(NotesState())
    val notes: StateFlow<NotesState> = _notes.asStateFlow()

    private var timer: Job? = null

    fun refresh() {
        if (_child.value is Load.Failed) _child.value = Load.Loading
        viewModelScope.launch {
            val result = repository.child(childId)
            // Keep showing the profile if a background refresh fails.
            if (result.isSuccess || _child.value !is Load.Ready) _child.value = result.toLoad()
        }
    }

    fun deleteChild(onDeleted: () -> Unit) = run(Busy.Delete) {
        repository.delete(childId).onSuccess { onDeleted() }
    }

    // --- Note editing ---

    fun onContent(value: String) = _notes.update { it.copy(content = value) }
    fun onLanguage(value: String) = _notes.update { it.copy(language = value) }

    fun edit(note: ChildNote) = _notes.update {
        it.copy(editingId = note.id, content = note.content, method = note.inputMethod, error = null)
    }

    fun cancelEdit() = _notes.update { it.copy(editingId = null, content = "", method = NoteInputMethod.TEXT) }

    fun saveNote() {
        val state = _notes.value
        if (state.content.isBlank()) return
        run(Busy.Save) {
            repository.saveNote(childId, state.editingId, state.content.trim(), state.method).onSuccess {
                _notes.update { it.copy(content = "", editingId = null, method = NoteInputMethod.TEXT) }
                refresh()
            }
        }
    }

    fun deleteNote(noteId: String) = run(Busy.Save) {
        repository.deleteNote(childId, noteId).onSuccess { refresh() }
    }

    // --- Voice dictation ---

    fun startRecording() {
        discardAudio()
        try {
            recorder.start()
        } catch (e: Exception) {
            microphoneUnavailable()
            return
        }
        _notes.update { it.copy(recording = true, seconds = 0, error = null) }
        timer = viewModelScope.launch {
            while (_notes.value.seconds < MAX_RECORDING_SECONDS) {
                delay(1_000)
                _notes.update { it.copy(seconds = it.seconds + 1) }
            }
            stopRecording()
        }
    }

    fun stopRecording() {
        timer?.cancel()
        val file = recorder.stop()
        _notes.update { it.copy(recording = false, audio = file) }
        if (file == null) microphoneUnavailable()
    }

    fun microphoneUnavailable() = _notes.update {
        it.copy(recording = false, error = "Microphone refusé ou indisponible. Vous pouvez écrire la note.", errorFrom = null)
    }

    fun discardAudio() {
        _notes.value.audio?.delete()
        _notes.update { it.copy(audio = null) }
    }

    fun transcribe() {
        val state = _notes.value
        val audio = state.audio ?: return
        if (!audio.exists()) {
            _notes.update {
                it.copy(audio = null, error = "L’enregistrement est introuvable. Enregistrez à nouveau.", errorFrom = null)
            }
            return
        }
        run(Busy.Transcription) {
            repository.transcribe(childId, audio, state.seconds.coerceAtLeast(1), state.language).onSuccess { text ->
                audio.delete()
                _notes.update { it.copy(content = text, method = NoteInputMethod.VOICE, audio = null) }
            }
        }
    }

    // --- AI suggestions ---

    fun organize() {
        val content = _notes.value.content
        if (content.isBlank()) return
        run(Busy.Organize) {
            repository.organizeNote(childId, content).onSuccess { s ->
                _notes.update {
                    it.copy(
                        suggestions = mapOf(
                            SuggestionField.Interests to s.interests.joinToString(", "),
                            SuggestionField.Comfort to s.comfortPreferences.joinToString(", "),
                            SuggestionField.Gifts to s.giftWishes.joinToString(", "),
                            SuggestionField.Themes to s.birthdayThemes.joinToString(", "),
                        ),
                    )
                }
            }
        }
    }

    fun onSuggestion(field: SuggestionField, value: String) =
        _notes.update { it.copy(suggestions = it.suggestions?.plus(field to value)) }

    fun rejectSuggestions() = _notes.update { it.copy(suggestions = null) }

    fun approveSuggestions() {
        val draft = _notes.value.suggestions ?: return
        fun list(field: SuggestionField) = draft[field].orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)
        val suggestions = OrganizedSuggestions(
            interests = list(SuggestionField.Interests),
            comfortPreferences = list(SuggestionField.Comfort),
            giftWishes = list(SuggestionField.Gifts),
            birthdayThemes = list(SuggestionField.Themes),
        )
        run(Busy.Approve) {
            repository.approveSuggestions(childId, suggestions).onSuccess {
                _notes.update { it.copy(suggestions = null) }
                refresh()
            }
        }
    }

    /** Runs one action at a time and surfaces its error message. */
    private fun run(busy: Busy, block: suspend () -> Result<*>) {
        if (_notes.value.busy != null) return
        _notes.update { it.copy(busy = busy, error = null) }
        viewModelScope.launch {
            val error = block().exceptionOrNull()?.message
            _notes.update { it.copy(busy = null, error = error, errorFrom = busy.takeIf { error != null }) }
        }
    }

    override fun onCleared() {
        timer?.cancel()
        recorder.release()
        _notes.value.audio?.delete()
    }
}
