package com.tsbridge.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.tsbridge.R
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.network_activity.*

class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.showLog("NetworkActivity created")

        setContentView(R.layout.network_activity)

        Utils.mIsBackFromSetNetwork = true

        network_mobile_button.setOnClickListener(this@NetworkActivity)
        network_wifi_button.setOnClickListener(this@NetworkActivity)
    }

    override fun onClick(v: View) {
        val id = v.id
        var intent: Intent? = null
        when (id) {
            R.id.network_mobile_button -> intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            R.id.network_wifi_button -> intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            else -> { }
        }
        if (intent != null)
            startActivity(intent)
    }

    public override fun onResume() {
        super.onResume()
        Utils.showLog("NetworkActivity Resume")

        if (Utils.isNetWorkConnected(this@NetworkActivity))
            finish()
    }

    public override fun onDestroy() {
        super.onDestroy()
        Utils.showLog("NetworkActivity destroyed")
    }
}