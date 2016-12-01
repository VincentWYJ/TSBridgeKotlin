package com.tsbridge.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import cn.bmob.v3.Bmob
import com.tsbridge.R
import com.tsbridge.adapter.MainAdapter
import com.tsbridge.fragment.BulletinFragment
import com.tsbridge.fragment.SendFragment
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/** 2016.11.29 开始利用 Kotlin 语言编写 Android */
class MainActivity: AppCompatActivity() {
    private var mTabNames: Array<String>? = null
    private var mFragments: List<Fragment>? = null
    private var mAdapter: MainAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.showLog("MainActivity onCreate")

        setContentView(R.layout.activity_main)

        initParams()
        initViews()
    }

    private fun initParams() {
        /** 初始化Bmob云数据库的操作环境 */
        Bmob.initialize(this, "49e434b5c094767986f0ba49baa8790f")

        mTabNames = resources.getStringArray(R.array.title_array)
    }

    private fun initViews() {
        setSupportActionBar(toolbar)

        mFragments = arrayListOf(BulletinFragment(), SendFragment())

        mAdapter = MainAdapter(supportFragmentManager, mTabNames, mFragments)

        viewPager.adapter = mAdapter

        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onDestroy() {
        super.onDestroy()

        Utils.showLog("MainActivity onDestroy")
    }
}
