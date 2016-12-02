package com.tsbridge.fragment

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tsbridge.R
import com.tsbridge.activity.NetworkActivity
import com.tsbridge.adapter.BulletinAdapter
import com.tsbridge.entity.ReceiveBulletin
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.bulletin_fragment.view.*
import java.util.*

class BulletinFragment: Fragment() {
    private var mContext: Context? = null

    private var mBulletins: ArrayList<ReceiveBulletin>? = null
    private var mBulletinAdapter: BulletinAdapter? = null

    private var mRootView: View? = null
    private var mRecyclerView: RecyclerView? = null

    private var mIsBackFromNetwork = false

    private val mImageUrls = arrayOf("www.baidu.com", "www.baidu.com", "www.baidu.com",
                                    "www.baidu.com", "www.baidu.com")
    private val mNames = arrayOf("名字1", "名字2", "名字3", "名字4", "名字5")
    private val mTimes = arrayOf("时间1", "时间2", "时间3", "时间4", "时间5")
    private val mContents = arrayOf("内容1", "内容2", "内容3", "内容4", "内容5")

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Utils.showLog("BulletinFragment onCreateView")

        mRootView = inflater!!.inflate(R.layout.bulletin_fragment, container, false)
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.showLog("BulletinFragment onActivityCreated")

        initParams()
        initViews()
    }

    private fun initParams() {
        mContext = activity
        mBulletins = ArrayList<ReceiveBulletin>()
        mBulletinAdapter = BulletinAdapter(mContext!!, mBulletins)
    }

    private fun initViews() {
        mRecyclerView = mRootView!!.bulletin_recyclerview
        mRecyclerView!!.adapter = mBulletinAdapter
        mRecyclerView!!.layoutManager = LinearLayoutManager(mContext)

        getItemsFromBulletin()
    }

    /** 从云上获取数据之前先进行网络判断 */
    private fun getItemsFromBulletin() {
        if (Utils.isNetWorkConnected(mContext!!))
            QueryBulletinTask().execute("")
        else {
            mIsBackFromNetwork = true
            startActivity(Intent(mContext, NetworkActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("BulletinFragment onResume")

        if (mIsBackFromNetwork && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetwork = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(mContext!!))
                getItemsFromBulletin()
            else
                Utils.showToast(mContext!!,
                        mContext!!.getString(R.string.no_connected_network))
        }
    }

    private inner class QueryBulletinTask: AsyncTask<String, Void, List<ReceiveBulletin>>() {
        override fun doInBackground(vararg params: String): List<ReceiveBulletin> {
            val length = mImageUrls.size
            for (i in 0..length - 1) {
                var bulletin = ReceiveBulletin(mNames[i], mTimes[i], mContents[i], mImageUrls[i])
                mBulletins!!.add(bulletin)
            }
            return mBulletins!!
        }

        override fun onPostExecute(result: List<ReceiveBulletin>) {
            if (result.size > 0)
                mBulletinAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils.showLog("BulletinFragment onDestroyView")

        if (mBulletins != null) {
            mBulletins!!.clear()
            mBulletins = null
        }
    }
}