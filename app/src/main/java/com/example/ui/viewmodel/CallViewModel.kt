package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CallRepository
import com.example.data.Contact
import com.example.data.RecentCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CallState {
    object Idle : CallState()
    data class Incoming(
        val contactName: String,
        val contactNumber: String,
        val videoUrl: String,
        val contactId: Long? = null
    ) : CallState()
    
    data class Ongoing(
        val contactName: String,
        val contactNumber: String,
        val startTimeMs: Long,
        val callType: String = "INCOMING", // "INCOMING" or "OUTGOING"
        val contactId: Long? = null
    ) : CallState()
}

class CallViewModel(
    application: Application,
    private val repository: CallRepository
) : AndroidViewModel(application) {

    // Contacts & Recent Calls as reactive StateFlows
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentCalls: StateFlow<List<RecentCall>> = repository.allRecentCalls
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Call States
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Call active counter / duration
    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    // Global settings StateFlows
    private val _defaultVideoUrl = MutableStateFlow("https://assets.mixkit.co/videos/preview/mixkit-abstract-digital-technology-background-loop-41852-large.mp4")
    val defaultVideoUrl: StateFlow<String> = _defaultVideoUrl.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true) // Modern theme starts dark by default!
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private var durationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            
            // Observe configuration settings
            repository.observeSetting("default_video_url").collect { saved ->
                if (saved != null) {
                    _defaultVideoUrl.value = saved
                }
            }
        }
        viewModelScope.launch {
            repository.observeSetting("app_theme_dark").collect { savedVal ->
                if (savedVal != null) {
                    _isDarkTheme.value = savedVal.toBoolean()
                }
            }
        }
    }

    // Toggle app theme dark/light mode
    fun setThemeMode(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            repository.saveSetting("app_theme_dark", isDark.toString())
        }
    }

    // Set global default video background url
    fun setDefaultVideoUrl(url: String) {
        _defaultVideoUrl.value = url
        viewModelScope.launch {
            repository.saveSetting("default_video_url", url)
        }
    }

    // Contact actions
    fun addContact(name: String, number: String, email: String, videoUrl: String?) {
        viewModelScope.launch {
            // Pick a random pleasant Material-like color for avatar badge
            val colors = listOf(
                0xFF5C6BC0.toInt(), // Indigo
                0xFF26A69A.toInt(), // Teal
                0xFFAB47BC.toInt(), // Purple
                0xFF42A5F5.toInt(), // Light Blue
                0xFFFFA726.toInt(), // Orange
                0xFF26C6DA.toInt(), // Cyan
                0xFFEC407A.toInt()  // Pink
            )
            val randomColor = colors.random()
            val newContact = Contact(
                name = name,
                phoneNumber = number,
                email = email,
                customVideoUrl = if (videoUrl.isNullOrEmpty()) null else videoUrl,
                avatarColor = randomColor
            )
            repository.insertContact(newContact)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    // Call Simulation functions
    fun simulateIncomingCall(name: String, number: String, contactId: Long? = null, customVideo: String? = null) {
        viewModelScope.launch {
            // Resolve correct calling background video: Custom contact video -> global video -> fallback default
            val targetVideo = customVideo ?: getContactCustomVideo(contactId) ?: defaultVideoUrl.value
            _callState.value = CallState.Incoming(
                contactName = name,
                contactNumber = number,
                videoUrl = targetVideo,
                contactId = contactId
            )
        }
    }

    fun initiateOutgoingCall(name: String, number: String, contactId: Long? = null) {
        _callState.value = CallState.Ongoing(
            contactName = name,
            contactNumber = number,
            startTimeMs = System.currentTimeMillis(),
            callType = "OUTGOING",
            contactId = contactId
        )
        startDurationCounter()
    }

    fun acceptCall() {
        val currentS = _callState.value
        if (currentS is CallState.Incoming) {
            _callState.value = CallState.Ongoing(
                contactName = currentS.contactName,
                contactNumber = currentS.contactNumber,
                startTimeMs = System.currentTimeMillis(),
                callType = "INCOMING",
                contactId = currentS.contactId
            )
            startDurationCounter()
        }
    }

    fun declineCall() {
        val currentS = _callState.value
        if (currentS is CallState.Incoming) {
            // Save missed call to history
            viewModelScope.launch {
                repository.insertRecentCall(
                    RecentCall(
                        contactId = currentS.contactId,
                        name = currentS.contactName,
                        phoneNumber = currentS.contactNumber,
                        callType = "MISSED",
                        durationSeconds = 0
                    )
                )
            }
        }
        stopDurationCounter()
        _callState.value = CallState.Idle
    }

    fun hangupCall() {
        val currentS = _callState.value
        if (currentS is CallState.Ongoing) {
            val duration = _callDurationSeconds.value
            viewModelScope.launch {
                repository.insertRecentCall(
                    RecentCall(
                        contactId = currentS.contactId,
                        name = currentS.contactName,
                        phoneNumber = currentS.contactNumber,
                        callType = currentS.callType,
                        durationSeconds = duration
                    )
                )
            }
        }
        stopDurationCounter()
        _callState.value = CallState.Idle
    }

    private fun startDurationCounter() {
        durationJob?.cancel()
        _callDurationSeconds.value = 0
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun stopDurationCounter() {
        durationJob?.cancel()
        durationJob = null
        _callDurationSeconds.value = 0
    }

    private suspend fun getContactCustomVideo(id: Long?): String? {
        if (id == null) return null
        val fetched = contacts.value.find { it.id == id }
        return if (!fetched?.customVideoUrl.isNullOrEmpty()) fetched?.customVideoUrl else null
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllRecentCalls()
        }
    }

    fun deleteCallLog(id: Long) {
        viewModelScope.launch {
            repository.deleteRecentCallById(id)
        }
    }

    // Helper Factory Creator for ViewModel
    class Factory(
        private val application: Application,
        private val repository: CallRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CallViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
