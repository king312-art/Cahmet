package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_packages")
data class PricePackageEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val priceText: String,
  val diamondsText: String,
  val sortOrder: Int = 0,
) {
  fun toFormattedBullet(): String = "- $priceText — $diamondsText"

  companion object {
    fun defaultPackages(): List<PricePackageEntity> = listOf(
      PricePackageEntity(priceText = "PKR 750", diamondsText = "15,000 diamonds", sortOrder = 1),
      PricePackageEntity(priceText = "PKR 990", diamondsText = "20,000 diamonds", sortOrder = 2),
      PricePackageEntity(priceText = "PKR 1,480", diamondsText = "30,000 diamonds", sortOrder = 3),
      PricePackageEntity(priceText = "PKR 2,480", diamondsText = "50,000 diamonds", sortOrder = 4),
      PricePackageEntity(priceText = "PKR 4,850", diamondsText = "100,000 diamonds", sortOrder = 5),
    )
  }
}
