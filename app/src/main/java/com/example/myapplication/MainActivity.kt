package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.myapplication.mqtt.MqttClientHelper
import com.example.myapplication.mqtt.Values
import com.github.aachartmodel.aainfographics.aachartcreator.*
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_oxigenio.*
import kotlinx.android.synthetic.main.content_pressao.*
import kotlinx.android.synthetic.main.content_pulso.*
import kotlinx.android.synthetic.main.content_temperatura.*
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

class MainActivity : AppCompatActivity() {

    private val mqttClient by lazy {
        MqttClientHelper(this)
    }
    var values = Values()

    private lateinit var viewTemperatura : View
    private lateinit var viewPulso : View
    private lateinit var viewPressao : View
    private lateinit var viewRespiratoria : View
    private lateinit var aaChartModel: AAChartModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        setMqttCallBack()

        arterial.setOnClickListener {
            alternaTela("pressao")
        }

        respiratoria.setOnClickListener {
            alternaTela("oxigenio")
        }

        pulso.setOnClickListener {
            alternaTela("cardiaca")
        }

        temperatura.setOnClickListener {
           alternaTela("temperatura")
        }

        button_sub.setOnClickListener { view ->
        sub(view)
        }

        button_menu_temperatura.setOnClickListener { view ->
            voltaMenu()
        }
        button_menu_pressao.setOnClickListener { view ->
            voltaMenu()
        }
        button_menu_pulso.setOnClickListener { view ->
            voltaMenu()
        }
        button_menu_respiratoria.setOnClickListener { view ->
            voltaMenu()
        }

    }
    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                val snackbarMsg = "Connected to host:\n'$MQTT_HOST'."
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            override fun connectionLost(throwable: Throwable) {
                val snackbarMsg = "Connection to host lost:\n'$MQTT_HOST'"
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                val recebePressaoSistolica : String
                val recebePressaoDiastolica : String
                val recebePulso : String
                val recebeRespiratoria : String
                val recebeTemperatura : String
                when (topic) {
                    "health-monitor/vitals/HR" -> {
                        recebePulso = mqttMessage.toString()
                        textPulso.text = mqttMessage.toString() + " bpm"
                        preenchePulso(recebePulso)
                        atualiza(values.pulso,"cardiaca")
                    }
                    "health-monitor/vitals/tempC" -> {
                        recebeTemperatura = mqttMessage.toString()
                        val convert = recebeTemperatura.toDouble()
                        val formatTemp = String.format("%.2f", convert)
                        textTemperatura.text = formatTemp + " ºC"
                        preencheTemperatura(recebeTemperatura)
                        atualiza(values.temperatura,"temperatura")
                    }
                    "health-monitor/vitals/BP" -> {
                        val pressao = mqttMessage.toString()
                        val delimiter = "/"
                        val total = pressao.split(delimiter)
                        recebePressaoSistolica = total.get(0)
                        recebePressaoDiastolica = total.get(1)
                        textPressao.text = mqttMessage.toString() + " mmHg"
                        preenchePressao(recebePressaoSistolica,recebePressaoDiastolica)
                        atualizaPressao(values.pressaoSistolica, values.pressaoDiastolica)
                    }
                    "health-monitor/vitals/SpO2" -> {
                        recebeRespiratoria = mqttMessage.toString()
                        val convert2 = recebeRespiratoria.toDouble()
                        val formatSp02 = String.format("%.2f", convert2)
                        textRespiratoria.text = formatSp02 + " %SpO2"
                        preencheRespiratoria(recebeRespiratoria)
                        atualiza(values.oxigenio,"oxigenio")
                    }
                }
            }
            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w("Debug", "Message published to host '$MQTT_HOST'")
            }
        })
    }
    override fun onDestroy() {
        mqttClient.destroy()
        super.onDestroy()
    }

    private fun sub (view: View){
        var snackbarMsg : String = "Cannot subscribe to empty topic!"
        snackbarMsg = try {
            mqttClient.subscribe(values.topico_temperatura)
            mqttClient.subscribe(values.topico_cardiaca)
            mqttClient.subscribe(values.topico_oxigenio)
            mqttClient.subscribe(values.topico_pressao)
            "Conectado ao paciente"
        } catch (ex: MqttException) {
            "Erro ao se conectar ao paciente"
        }
        Snackbar.make(view, snackbarMsg, Snackbar.LENGTH_SHORT)
            .setAction("Action", null).show()
    }

    private fun grafico(valor: MutableList<Any>, tipo : String){

        aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .animationType(AAChartAnimationType.EaseOutCirc)
            .zoomType(AAChartZoomType.None)
            .title("Monitoramento")
            .backgroundColor("#EAEAEA")
            .dataLabelsEnabled(true)
            .dataLabelsStyle(AAStyle())
            .animationDuration(10)
            .stacking(AAChartStackingType.Normal)
            .series(arrayOf(AASeriesElement()
                .data(valor.toTypedArray()).name("Medições").color("#950C76")))
        when (tipo) {
            "temperatura" -> {
                viewTemperatura = findViewById<AAChartView>(R.id.aa_chart_view_temperatura)
                (viewTemperatura as AAChartView?)?.aa_drawChartWithChartModel(aaChartModel)
            }
            "cardiaca" -> {
                viewPulso = findViewById<AAChartView>(R.id.aa_chart_view_pulso)
                (viewPulso as AAChartView?)?.aa_drawChartWithChartModel(aaChartModel)
            }
            "oxigenio" -> {
                viewRespiratoria = findViewById<AAChartView>(R.id.aa_chart_view_respiratoria)
                (viewRespiratoria as AAChartView?)?.aa_drawChartWithChartModel(aaChartModel)
            }
        }
    }

    private fun graficoPressao(valor: MutableList<Any>, valor2: MutableList<Any>){
        aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .animationType(AAChartAnimationType.EaseOutCirc)
            //.zoomType(AAChartZoomType.None)
            .title("Monitoramento")
            .backgroundColor("#EAEAEA")
            .dataLabelsEnabled(true)
            .dataLabelsStyle(AAStyle())
            .animationDuration(10)
            .series(arrayOf(AASeriesElement()
                .data(valor.toTypedArray()).name("Pressão sistólica").color("#950C76"),
                AASeriesElement()
                    .name("Pressão diastólica")
                    .data(valor2.toTypedArray()).color("#E3C610")))
                viewPressao = findViewById<AAChartView>(R.id.aa_chart_view_pressao)
                (viewPressao as AAChartView?)?.aa_drawChartWithChartModel(aaChartModel)
    }

    fun atualiza (valor: MutableList<Any>, name : String){
        when (name) {
            "temperatura" -> {
                (viewTemperatura as AAChartView?)?.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(AASeriesElement().data(valor.toTypedArray())),animation = true)
            }
            "cardiaca" -> {
                (viewPulso as AAChartView?)?.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(AASeriesElement().data(valor.toTypedArray())),animation = true)
            }
            "oxigenio" -> {
                (viewRespiratoria as AAChartView?)?.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(AASeriesElement().data(valor.toTypedArray())),animation = true)
            }
            "pressao" -> {
                (viewPressao as AAChartView?)?.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(AASeriesElement().data(valor.toTypedArray())),animation = true)
            }
        }
    }
    fun atualizaPressao (valor: MutableList<Any>, valor2 : MutableList<Any>){
        (viewPressao as AAChartView?)?.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(
            AASeriesElement().data(valor.toTypedArray()),
            AASeriesElement().data(valor2.toTypedArray())),animation = true)
    }



    fun preenchePressao(convert:String, convert2:String){
        if(values.pressaoSistolica.size<11 && values.pressaoDiastolica.size<11) {
            values.pressaoSistolica.add(convert.toInt())
            values.pressaoDiastolica.add(convert2.toInt())
            atualizaPressao(values.pressaoSistolica, values.pressaoDiastolica)
        }else if(values.pressaoSistolica.size>10){
            values.pressaoSistolica.removeAt(0)
            values.pressaoDiastolica.removeAt(0)
            values.pressaoSistolica.add(convert.toInt())
            values.pressaoDiastolica.add(convert2.toInt())
            atualizaPressao(values.pressaoSistolica, values.pressaoDiastolica)
        }
    }
    fun preenchePulso(convert:String){
        if(values.pulso.size<11) {
            values.pulso.add(convert.toInt())
            atualiza(values.pulso,"cardiaca")
        }else if(values.pulso.size>10){
            values.pulso.removeAt(0)
            values.pulso.add(convert.toInt())
            atualiza(values.pulso,"cardiaca")
        }
    }
    fun preencheRespiratoria(convert:String){
        if(values.oxigenio.size<11) {
            values.oxigenio.add(convert.toDouble())
            atualiza(values.oxigenio,"oxigenio")
        }else if(values.oxigenio.size>10){
            values.oxigenio.removeAt(0)
            values.oxigenio.add(convert.toDouble())
            atualiza(values.oxigenio,"oxigenio")
        }
    }
    fun preencheTemperatura(convert:String){
        if(values.temperatura.size<11) {
            values.temperatura.add(convert.toDouble())
            atualiza(values.temperatura,"temperatura")
        }else if(values.temperatura.size>10){
            values.temperatura.removeAt(0)
            values.temperatura.add(convert.toDouble())
            atualiza(values.temperatura,"temperatura")
        }
    }

    private fun alternaTela(tela: String){
        when (tela) {
            "temperatura" -> {
                layout_temperatura.isVisible = true
                layout_menu.isInvisible = true
                layout_pulso.isInvisible = true
                layout_pressao.isInvisible = true
                layout_respiratoria.isInvisible = true
                grafico(values.temperatura, "temperatura")
            }
            "cardiaca" -> {
                layout_temperatura.isInvisible = true
                layout_menu.isInvisible = true
                layout_pulso.isVisible = true
                layout_pressao.isInvisible = true
                layout_respiratoria.isInvisible = true
                grafico(values.pulso, "cardiaca")

            }
            "pressao" -> {
                layout_temperatura.isInvisible = true
                layout_menu.isInvisible = true
                layout_pulso.isInvisible = true
                layout_pressao.isVisible = true
                layout_respiratoria.isInvisible = true
                graficoPressao(values.pressaoSistolica, values.pressaoDiastolica)

            }
            "oxigenio" -> {
                layout_temperatura.isInvisible = true
                layout_menu.isInvisible = true
                layout_pulso.isInvisible = true
                layout_pressao.isInvisible = true
                layout_respiratoria.isVisible = true
                grafico(values.oxigenio, "oxigenio")
            }
        }

    }

    fun voltaMenu(){
        layout_temperatura.isInvisible = true
        layout_menu.isVisible = true
        layout_pulso.isInvisible = true
        layout_pressao.isInvisible = true
        layout_respiratoria.isInvisible = true
    }
}
