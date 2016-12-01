package com.tsbridge.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.tsbridge.utils.Utils

/**
 * mTabNames--标签名称数组
 * mFragments--页面对象列表
 * fragmentManager--Fragment管理器
 * 注：形参用 val 防止后续被修改
 */
class MainAdapter(mFragmentManager: FragmentManager,
                  private val mTabNames: Array<String>?,
                  private val mFragments: List<Fragment>?)
                   : FragmentStatePagerAdapter(mFragmentManager) {
    init {
        Utils.showLog("Create a MainAdapter object")
    }

    override fun getItem(position: Int): Fragment {
        return mFragments!![position]
    }

    override fun getCount(): Int {
        return mFragments!!.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return mTabNames!![position]
    }
}
