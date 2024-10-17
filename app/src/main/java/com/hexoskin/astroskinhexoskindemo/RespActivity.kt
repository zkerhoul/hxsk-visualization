package com.hexoskin.astroskinhexoskindemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.hexoskin.astroskinhexoskindemo.databinding.ActivityRespBinding


class RespActivity : AppCompatActivity(), DeviceManager.DataListener {
    private lateinit var thoracicValView: TextView
    private lateinit var abdominalValView: TextView
    private lateinit var binding: ActivityRespBinding
    private lateinit var respView : CanvasView
    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRespBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.pdfView.fromAsset("sample.pdf")
            .swipeHorizontal(true)
            .load()

        setupView()

        deviceManager = (application as DemoApplication).deviceManager
        deviceManager.setDataListener(this)
        deviceManager.enableRespData()
    }

    private fun setupView(){
        // different views for different types of visualization
        respView = findViewById(R.id.respCanvas)
        thoracicValView = findViewById(R.id.respThoracicValue)
        abdominalValView = findViewById(R.id.respAbdominalValue)
    }

    override fun onHeartRateUpdate(value: Int) {
        TODO("Not yet implemented")
    }

    override fun onBreathingRateUpdate(value: Float) {
        TODO("Not yet implemented")
    }

    override fun onECGUpdate(data: Array<FloatArray>) {
        TODO("Not yet implemented")
    }

    override fun onRespUpdate(data: Array<IntArray>) {
        runOnUiThread {
            respView.visibility = View.VISIBLE

            // access thoracic and abdominal data from each channel
            val thoracic = data[0][0]
            val abdominal = data[1][0]

            thoracicValView.text = thoracic.toString()
            abdominalValView.text = abdominal.toString()

            // draw circle to screen
            respView.update(thoracic, abdominal)
        }
    }

}