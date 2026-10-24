package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val senderTitle: String,
  val messageSnippet: String,
  val statusBadge: String,
  val timestamp: Long = System.currentTimeMillis(),
)
