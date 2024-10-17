package com.hexoskin.astroskinhexoskindemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.hexoskin.hxsdk.HexoskinException
import com.hexoskin.hxsdk.Recorder
import com.hexoskin.hxsdk.hexoskinOnDataType
import com.hexoskin.hxsdk.model.RawSignalType
import com.hexoskin.hxsdk.model.RtDataModeType
import kotlinx.coroutines.*
import java.util.*

/**
 * Handles the connection between the phone and the device and wraps the calls to the HxSDK's Recorder.
 * @see Recorder hosts the API to communicate with the Astroskin/Hexoskin device. It needs an application context in order to work.
 * Please DO NOT use an Activity or Service context as they might be destroyed at some point in your app lifecycle.
 * We strongly recommend to make sure that calls to Recorder are done on a separate thread to avoid blocking UI.
 * Here, it is done through coroutines on the IO coroutine context.
 * @see Dispatchers.IO
 * We also recommend to use a Dependency Injection (DI) framework like Dagger or Koin to manage dependencies required by the SDK.
 * Here it is done simply with the constructor to keep the demo simple enough.
 * In case of connection errors, Recorder raises a HexoskinException with a root cause message in it.
 * @see HexoskinException
 * More specific types of exceptions and error messages will be implemented in the future.
 */
class DeviceManager(private val context: Context) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var recorder: Recorder? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionListener: ConnectionListener? = null
    private var dataListener: DataListener? = null

    fun setConnectionListener(connectionListener: ConnectionListener) {
        this.connectionListener = connectionListener
    }

    fun removeConnectionListener() {
        connectionListener = null
    }

    fun setDataListener(dataListener: DataListener) {
        this.dataListener = dataListener
    }

    fun removeDataListener() {
        dataListener = null
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        val pairedDevices = ArrayList<BluetoothDevice>()
        bluetoothAdapter?.let {
            it.bondedDevices.forEach {
                if (it.name.startsWith(ASTROSKIN_DEVICE_NAME_PREFIX) ||
                    it.name.startsWith(HEXOSKIN_DEVICE_NAME_PREFIX)) {
                    pairedDevices.add(it)
                }
            }
        }
        return pairedDevices
    }

    /**
     * Open a connection to the device through the Recorder.
     * We close any previous connection beforehand.
     * It is important to close the Recorder connection when you dispose of the connection with the device.
     * You can call close() from the Recorder in a BroadcastReceiver or when the app is closed, this will prevent memory leaks.
     * @see Recorder.close
     * We launch those tasks in a coroutine to avoid blocking UI changes. Make sure you run UI updates in the UI thread though.
     * Don't forget to catch any HexoskinException that might happen. A message indicating the possible root cause is set in it.
     */
    fun connectToDevice(device: BluetoothDevice) {
        coroutineScope.launch {
            try {
                closeRecorder()
                openRecorder(device)
            } catch (e: HexoskinException) {
                e.printStackTrace()
                connectionListener?.onDeviceFailedToOpen()
            }
        }
    }

    private fun openRecorder(device: BluetoothDevice) {
        recorder = Recorder(context, bluetoothManager, device)
        recorder?.let {
            try {
                it.open { isOpen ->
                    if (isOpen) {
                        connectionListener?.onDeviceConnected()
                    } else {
                        connectionListener?.onDeviceFailedToOpen()
                    }
                }
            } catch (e: HexoskinException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * This function closes the connection between the SDK and the device.
     * It removes all listeners and free the memory used by the SDK for the previous connection.
     * You might want to cancel all your related coroutines after closing the Recorder.
     */
    fun closeRecorder() = coroutineScope.launch {
        recorder?.close()
    }


    /**
     * This function returns the device state and signal quality.
     */
    suspend fun getState() = coroutineScope.async {
        recorder?.state()
    }.await()

    /**
     * This function returns the device info such as serial number, firmware version and model name.
     */
    suspend fun getDeviceInfo() = coroutineScope.async {
        recorder?.deviceInfo()
    }.await()

    /**
     * The device starts recording when it is plugged to an clothing.
     * This function makes the device stop recording.
     */
    fun stopSession() = coroutineScope.launch {
        recorder?.stopSession()
    }

    /**
     * Returns the time in a Date object.
     * @see Date
     */
    suspend fun getTime() = coroutineScope.async {
        recorder?.getTime()
    }.await()

    /**
     * Recorder can set time on the device. Everything is done in the SDK so you don't have anything to provide.
     * It relies on the phone current date and time so it might depend on the phone Locale.
     * Note: In order to set the time, you have to stop the recording.
     */
    fun setTime() = coroutineScope.launch {
        recorder?.setTime()
    }

    /**
     * Recorder allows you to subscribe to several data streams like heart rate, breathing rate, ECG and sensors accordingly to the device capabilities.
     * Here we subscribe to heart rate data stream. All data streams functions take a callback as a parameter that will be called for each update.
     * The callback function contains a RecorderPacket object that wraps the data in a 2D array.
     * Here the heart rate value is located at the first element of the 2D array.
     * Same for the breathing rate.
     */
    fun enableHeartRateData() {
        coroutineScope.launch {
            recorder?.onData?.heartRate { _, value ->
                dataListener?.onHeartRateUpdate(value)
            }
        }
    }

    fun enableBreathingRateData() {
        coroutineScope.launch {
            recorder?.onData?.respirationRate { _ , value ->
                dataListener?.onBreathingRateUpdate(value)
            }
        }
    }

    /**
     * Subscribes to the ECG data stream. The data channels are in the three first lines of the 2D array.
     *
     * Here we unsubscribe right after the first result to avoid prompting a dialog at each update.
     * To unsubscribe to a data stream, just call the same function than subscribing but pass null in order to remove any listener.
     * All listener references are removed when Recorder.close() is called (which is why it is important to call it at some point).
     *
     * For Hexoskin BLE devices there can only be one low level data stream (ECG, ACC, RESP). These streams must be manually enabled.
     */
    fun enableECGData() {
        coroutineScope.launch {
            recorder?.onData?.ecg { _ , values ->
                dataListener?.onECGUpdate(arrayOf(values[0]))
                recorder?.onData?.ecg(null)
            }

            if (recorder?.recorderType() == Recorder.RecorderType.HEXOSKIN) {
                recorder?.selectRawSignal(RawSignalType.ECG)
            }
        }
    }

    fun enableRespData() {
        coroutineScope.launch {
            recorder?.onData?.respiration { _ , values ->
                dataListener?.onRespUpdate(values)
                // removed this line of code in order to receive a continuous stream of data
                // recorder?.onData?.respiration(null)
            }
            if (recorder?.recorderType() == Recorder.RecorderType.HEXOSKIN) {
                recorder?.selectRawSignal(RawSignalType.RESP)
            }

        }
    }

    interface ConnectionListener {
        fun onDeviceConnected()
        fun onDeviceFailedToOpen()
    }

    interface DataListener {
        fun onHeartRateUpdate(value: Int)
        fun onBreathingRateUpdate(value: Float)
        fun onECGUpdate(data: Array<FloatArray>)
        fun onRespUpdate(data: Array<IntArray>)
    }

    companion object {
        const val ASTROSKIN_DEVICE_NAME_PREFIX = "BIOMON"
        const val HEXOSKIN_DEVICE_NAME_PREFIX = "HX"
    }
}