package com.hexoskin.astroskinhexoskindemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This activity is handling the UI side of the screen, the bluetooth interactions between the phone and the device are handled in DeviceManager.
 * @see DeviceManager
 */
class DeviceListActivity : AppCompatActivity(), DeviceManager.ConnectionListener {

    private lateinit var deviceListRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val adapter = DeviceListAdapter()

    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        progressBar = findViewById(R.id.progressBar)
        deviceListRecyclerView = findViewById(R.id.deviceListRecyclerView)
        deviceListRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        deviceListRecyclerView.adapter = adapter

        deviceManager = (application as DemoApplication).deviceManager
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        adapter.setItemsList(deviceManager.getPairedDevices())
    }

    /**
     * Check if bluetooth is enabled and enable bluetooth if it's not.
     */
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    deviceManager.setConnectionListener(this)
                }else{
                    Toast.makeText(this, getString(R.string.permission_not_accepted), Toast.LENGTH_LONG).show()
                }
            }
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!permissions.values.contains(false)) {
                deviceManager.setConnectionListener(this)
            } else {
                Toast.makeText(this, getString(R.string.permission_not_accepted), Toast.LENGTH_LONG).show()
            }
        }

    /**
     * Open connection to the selected device.
     * Make sure you run any UI instructions in the UI thread to avoid raising an exception.
     */
    override fun onDeviceConnected() {
        runOnUiThread {
            progressBar.visibility = View.GONE
            deviceManager.removeConnectionListener()
            startActivity(Intent(this, RespActivity::class.java))
        }
    }

    override fun onDeviceFailedToOpen() {
        runOnUiThread {
            Toast.makeText(this, getString(R.string.device_open_failed), Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    private inner class DeviceListAdapter :
        RecyclerView.Adapter<DeviceListAdapter.DeviceListItemViewHolder>() {

        private var items: List<BluetoothDevice> = emptyList()

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DeviceListItemViewHolder =
            DeviceListItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.device_list_item, parent, false)
            )

        override fun onBindViewHolder(holder: DeviceListItemViewHolder, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size

        fun setItemsList(deviceList: List<BluetoothDevice>) {
            items = deviceList
            notifyDataSetChanged()
        }

        private inner class DeviceListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(device: BluetoothDevice) {
                val deviceNameTextView = itemView.findViewById<TextView>(R.id.deviceName)
                val deviceAddressTextView = itemView.findViewById<TextView>(R.id.deviceAddress)

                deviceNameTextView.text = device.name
                deviceAddressTextView.text = device.address

                itemView.setOnClickListener {
                    deviceManager.connectToDevice(device)
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}
