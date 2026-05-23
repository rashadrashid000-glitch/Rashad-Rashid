package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val customVideoUrl: String? = null, // Custom looping video for this contact
    val avatarColor: Int = 0xFF5C6BC0.toInt() // Color for circular placeholder avatar
) : java.io.Serializable
