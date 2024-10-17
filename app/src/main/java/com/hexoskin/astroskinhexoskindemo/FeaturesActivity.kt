package com.hexoskin.astroskinhexoskindemo

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.illposed.osc.OSCSerializeException
import com.illposed.osc.transport.udp.OSCPortOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.net.*
import java.util.*
import com.illposed.osc.*
import java.io.IOException


/**
 * This activity handles mostly the UI. See DeviceManager for the interactions between the SDK and the device.
 * @see DeviceManager
 */
class FeaturesActivity : AppCompatActivity(), DeviceManager.DataListener {

    private lateinit var heartRateValueView: TextView
    private lateinit var breathingRateValueView: TextView
    private lateinit var deviceInfoButton: Button
    private lateinit var deviceStateButton: Button
    private lateinit var stopSessionButton: Button
    private lateinit var getECGButton: Button
    private lateinit var getTimeButton: Button
    private lateinit var setTimeButton: Button

    private lateinit var getRespButton: Button
    private lateinit var deviceManager: DeviceManager

    // instance variables for drawing visualization to tablet screen
//    private lateinit var thoracicValueView: TextView
//    private lateinit var abdominalValueView: TextView
//    private lateinit var circleView : CanvasView

     // instance of OSC Port for sending OSC messages
     private lateinit var oscPortOut : OSCPortOut
     private val oscScope = CoroutineScope(Dispatchers.IO)
     private val myIP = "35.3.1.126" // Macbook IP address
     private val myPort = 17000 // OSC port

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_features)

        // initialize osc port
        try {
            oscPortOut = OSCPortOut(InetAddress.getByName(myIP), myPort)
            Log.d("2Inspire", "OSC Port created")
        }
        catch (e: UnknownHostException) {
            Log.d("2Inspire", "OSC Port - Unknown Host Exception")
        }
        catch(e: SocketException) {
            Log.d("2Inspire", "OSC Port - Socket Exception")
        }

        setupView()

        deviceManager = (application as DemoApplication).deviceManager
        deviceManager.setDataListener(this)
        deviceManager.enableHeartRateData()
        deviceManager.enableBreathingRateData()
    }

    override fun onDestroy() {
        deviceManager.removeDataListener()
        deviceManager.closeRecorder()
        super.onDestroy()
    }

    private fun setupView() {
        setSupportActionBar(findViewById(R.id.toolbar))
        heartRateValueView = findViewById(R.id.featuresHeartRateValue)
        breathingRateValueView = findViewById(R.id.featuresBreathingRateValue)
        deviceInfoButton = findViewById(R.id.featuresDeviceInfoButton)
        deviceStateButton = findViewById(R.id.featuresStateButton)
        stopSessionButton = findViewById(R.id.featuresStopSessionButton)
        getECGButton = findViewById(R.id.featuresGetECGButton)
        getTimeButton = findViewById(R.id.featuresGetTimeButton)
        setTimeButton = findViewById(R.id.featuresSetTimeButton)

        getRespButton = findViewById(R.id.featuresGetRespButton)


        deviceInfoButton.setOnClickListener {
            lifecycleScope.launch {
                val deviceInfo = deviceManager.getDeviceInfo()
                deviceInfo?.let {
                    val deviceInfoText = StringBuffer()
                    deviceInfoText.append(getString(R.string.device_info_model, it.model))
                    deviceInfoText.append(
                        getString(
                            R.string.device_info_firmware,
                            it.firmwareRevision
                        )
                    )
                    deviceInfoText.append(getString(R.string.device_info_serial, it.serial))
                    showDeviceData(getString(R.string.device_info), deviceInfoText.toString())
                }
            }
        }

        deviceStateButton.setOnClickListener {
            lifecycleScope.launch {
                val deviceState = deviceManager.getState()
                deviceState?.let {
                    val deviceStateText = StringBuffer()
                    deviceStateText.append(
                        getString(
                            R.string.battery_charge_level,
                            it.battery.chargeLevel.toString()
                        )
                    )
                    deviceStateText.append(
                        getString(
                            R.string.recording_status,
                            it.device.recording.toString()
                        )
                    )
                    showDeviceData(getString(R.string.device_state), deviceStateText.toString())
                }
            }
        }

        stopSessionButton.setOnClickListener {
            deviceManager.stopSession()
            heartRateValueView.text = getString(R.string.value_placeholder)
            breathingRateValueView.text = getString(R.string.value_placeholder)
            Toast.makeText(this, getString(R.string.session_stopped), Toast.LENGTH_LONG).show()
        }

        getECGButton.setOnClickListener {
            deviceManager.enableECGData()
            Toast.makeText(this, getString(R.string.ecg_enabled), Toast.LENGTH_LONG).show()
        }

        getRespButton.setOnClickListener {
            deviceManager.enableRespData()
            Toast.makeText(this, getString(R.string.resp_enabled), Toast.LENGTH_LONG).show()
        }

        getTimeButton.setOnClickListener {
            lifecycleScope.launch {
                val time = deviceManager.getTime()
                time?.let {
                    showDeviceData(getString(R.string.device_state), it.toLocaleString())
                }
            }
        }

        setTimeButton.setOnClickListener {
            deviceManager.setTime()
            Toast.makeText(this, getString(R.string.time_set), Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeviceData(title: String, message: String) {
        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.close)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setCancelable(false)
            .create()
        alertDialog.show()
    }

    override fun onHeartRateUpdate(value: Int) {
        runOnUiThread {
            heartRateValueView.text = value.toString()
        }
    }

    override fun onBreathingRateUpdate(value: Float) {
        runOnUiThread {
            breathingRateValueView.text = value.toString()
        }
    }

    /**
     * Here we show only the first channel of the ECG
     */
    override fun onECGUpdate(data: Array<FloatArray>) {
        runOnUiThread {
            val ecgDataText = StringBuffer()
            for (ecgValue in data[0]) {
                ecgDataText.append("$ecgValue \n")
            }
            val alertDialog: AlertDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.ecg_data_label))
                .setMessage(ecgDataText)
                .setPositiveButton(getString(R.string.close)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .setCancelable(false)
                .create()
            alertDialog.show()
        }
    }

    /**
     * Here we show only the first data of the Respiration
     */
    override fun onRespUpdate(data: Array<IntArray>) { // 8 values in each IntArray
        val thoracic = data[0][0]
        val abdominal = data[1][0]

        val message = OSCMessage("/respiration", listOf(thoracic, abdominal))


        try {
            oscPortOut.send(message)
            Log.d("2Inspire", "OSC message sent")
        }
        catch(e: Exception) {
            Log.d("2Inspire", "Error")
            e.printStackTrace()
        }
            catch(e: IOException) {
                Log.d("2Inspire", "IO Exception")
                e.printStackTrace()
            }
            catch(e: OSCSerializeException) {
                Log.d("2Inspire", "Serialize Exception")
            }

        /* Sample Code from Hexoskin Demo
        runOnUiThread {
            val respDataText = StringBuffer()
            for (respValue in data[0]) {
                respDataText.append("$respValue, ")
            }

            val alertDialog: AlertDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.resp_data_label))
                .setMessage(respDataText)
                .setPositiveButton(getString(R.string.close)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .setCancelable(false)
                .create()
            alertDialog.show()
        }
         */
    }
}
