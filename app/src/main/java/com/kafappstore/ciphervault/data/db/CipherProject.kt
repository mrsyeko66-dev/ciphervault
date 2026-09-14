package com.kafappstore.ciphervault.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cipher_projects")
data class CipherProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String = "Secret Project",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val outputExtension: String = "txt"
)
