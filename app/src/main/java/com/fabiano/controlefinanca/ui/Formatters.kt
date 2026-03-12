package com.fabiano.controlefinanca.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val brCurrency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

fun Double.toBrl(): String = brCurrency.format(this)

fun Long.toDateLabel(): String = dayFormat.format(Date(this))
