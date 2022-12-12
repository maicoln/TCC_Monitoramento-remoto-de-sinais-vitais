package com.example.myapplication.mqtt

class Values {

    val topico_temperatura = "health-monitor/vitals/tempC"
    val topico_cardiaca = "health-monitor/vitals/HR"
    val topico_oxigenio = "health-monitor/vitals/SpO2"
    val topico_pressao = "health-monitor/vitals/BP"

    val temperatura : MutableList<Any> = mutableListOf()
    val pulso : MutableList<Any> = mutableListOf()
    val oxigenio : MutableList<Any> = mutableListOf()
    val pressaoSistolica : MutableList<Any> = mutableListOf()
    val pressaoDiastolica : MutableList<Any> = mutableListOf()

}