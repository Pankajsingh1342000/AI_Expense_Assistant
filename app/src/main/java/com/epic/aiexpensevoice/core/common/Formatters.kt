package com.epic.aiexpensevoice.core.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Double.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(this)

fun nowLabel(): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
