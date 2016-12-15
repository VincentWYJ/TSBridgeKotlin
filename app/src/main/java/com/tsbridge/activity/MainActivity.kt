package com.tsbridge.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobUser
import com.tsbridge.R
import com.tsbridge.adapter.MainAdapter
import com.tsbridge.fragment.BulletinFragment
import com.tsbridge.fragment.LoginFragment
import com.tsbridge.fragment.SendFragment
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_content.*

/** 2016.11.29 开始利用 Kotlin 语言编写 Android */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.showLog("MainActivity onCreate")

        setContentView(R.layout.main_activity)

        initialization()
    }

    private fun initialization() {
        setSupportActionBar(toolBar)
        val mTabNames = resources.getStringArray(R.array.title_array)
        val mFragments = arrayListOf(BulletinFragment(), SendFragment(), LoginFragment())
        val mAdapter = MainAdapter(supportFragmentManager, mTabNames, mFragments)
        viewPager.adapter = mAdapter
        tabLayout.setupWithViewPager(viewPager)
        viewPager.offscreenPageLimit = 3

        Bmob.initialize(this@MainActivity, "49e434b5c094767986f0ba49baa8790f")
        if (BmobUser.getCurrentUser() != null)
            viewPager.currentItem = 0
        else
            viewPager.currentItem = 2
    }

    override fun onRestart() {
        super.onRestart()
        Utils.showLog("MainActivity onRestart")
    }

    /** Broadcast 注册与取消一般放在 onStart-onStop */
    override fun onStart() {
        super.onStart()
        Utils.showLog("MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("MainActivity onResume")
    }

    /** 简单数据存储一般放在 onPause，切记不能做耗时操作 */
    override fun onPause() {
        super.onPause()
        Utils.showLog("MainActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Utils.showLog("MainActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.showLog("MainActivity onDestroy")
    }
}
