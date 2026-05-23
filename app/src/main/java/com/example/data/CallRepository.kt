package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CallRepository(
    private val contactDao: ContactDao,
    private val recentCallDao: RecentCallDao,
    private val settingDao: SettingDao
) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val allRecentCalls: Flow<List<RecentCall>> = recentCallDao.getAllRecentCalls()

    fun observeSetting(key: String): Flow<String?> = settingDao.observeSetting(key)

    suspend fun getSetting(key: String): String? = settingDao.getSetting(key)

    suspend fun saveSetting(key: String, value: String) {
        settingDao.saveSetting(AppSetting(key, value))
    }

    suspend fun insertContact(contact: Contact): Long = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)

    suspend fun deleteContactById(id: Long) = contactDao.deleteContactById(id)

    suspend fun insertRecentCall(recentCall: RecentCall): Long = recentCallDao.insertRecentCall(recentCall)

    suspend fun deleteRecentCallById(id: Long) = recentCallDao.deleteRecentCallById(id)

    suspend fun clearAllRecentCalls() = recentCallDao.clearAllRecentCalls()

    suspend fun prepopulateIfNeeded() {
        val existing = allContacts.first()
        if (existing.isEmpty()) {
            val devs = listOf(
                Contact(
                    name = "Rashad Rashid",
                    phoneNumber = "+1 (555) 727-4832",
                    email = "rashadrashid000@gmail.com",
                    customVideoUrl = "https://assets.mixkit.co/videos/preview/mixkit-abstract-digital-technology-background-loop-41852-large.mp4",
                    avatarColor = 0xFF1E88E5.toInt()
                ),
                Contact(
                    name = "Rashtech Solutions Support",
                    phoneNumber = "+1 (800) 555-RASH",
                    email = "support@rashtechsolutions.com",
                    customVideoUrl = "https://assets.mixkit.co/videos/preview/mixkit-neon-light-strip-flashing-and-looping-41223-large.mp4",
                    avatarColor = 0xFF43A047.toInt()
                ),
                Contact(
                    name = "Jane Doe (Design)",
                    phoneNumber = "+1 (555) 123-4567",
                    email = "jane.doe@rashtech.com",
                    customVideoUrl = "https://assets.mixkit.co/videos/preview/mixkit-glowing-lines-on-a-circuit-board-background-41484-large.mp4",
                    avatarColor = 0xFFD81B60.toInt()
                ),
                Contact(
                    name = "John Smith",
                    phoneNumber = "+1 (555) 987-6543",
                    email = "john.smith@rashtech.com",
                    customVideoUrl = "",
                    avatarColor = 0xFFFB8C00.toInt()
                )
            )
            for (contact in devs) {
                contactDao.insertContact(contact)
            }

            val sampleRecents = listOf(
                RecentCall(
                    name = "Rashad Rashid",
                    phoneNumber = "+1 (555) 727-4832",
                    callType = "INCOMING",
                    durationSeconds = 145
                ),
                RecentCall(
                    name = "John Smith",
                    phoneNumber = "+1 (555) 987-6543",
                    callType = "MISSED",
                    durationSeconds = 0
                ),
                RecentCall(
                    name = "Jane Doe (Design)",
                    phoneNumber = "+1 (555) 123-4567",
                    callType = "OUTGOING",
                    durationSeconds = 54
                )
            )
            for (call in sampleRecents) {
                recentCallDao.insertRecentCall(call)
            }
        }

        val defaultVideoKey = "default_video_url"
        if (settingDao.getSetting(defaultVideoKey) == null) {
            settingDao.saveSetting(
                AppSetting(
                    defaultVideoKey,
                    "https://assets.mixkit.co/videos/preview/mixkit-abstract-digital-technology-background-loop-41852-large.mp4"
                )
            )
        }
    }
}
