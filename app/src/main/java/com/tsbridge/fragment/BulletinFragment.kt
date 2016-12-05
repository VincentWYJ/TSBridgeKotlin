package com.tsbridge.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.tsbridge.R
import com.tsbridge.activity.NetworkActivity
import com.tsbridge.adapter.BulletinAdapter
import com.tsbridge.entity.Bulletin
import com.tsbridge.entity.ReceiveBulletin
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.bulletin_fragment.*
import kotlinx.android.synthetic.main.bulletin_fragment.view.*
import org.jetbrains.anko.support.v4.onRefresh
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BulletinFragment: Fragment() {
    private var mContext: Context? = null

    private var mBulletins: ArrayList<ReceiveBulletin>? = null
    private var mBulletinAdapter: BulletinAdapter? = null

    private var mRootView: View? = null
    private var mRecyclerView: RecyclerView? = null

    private var mIsBackFromNetwork = false

    private var mIsRefreshing = false
    private var mIsRefreshingFromPullDown = false
    private var mBulletinCount = 0

    private val mNames = arrayOf("名字1", "名字2", "名字3", "名字4", "名字5")
    private val mTimes = arrayOf("时间1", "时间2", "时间3", "时间4", "时间5")
    private val mContents = arrayOf("内容1", "内容2", "内容3", "内容4", "内容5")
    private val mImageUrls = arrayOf(null, "www.baidu.com", "www.baidu.com",
            "www.baidu.com", "www.baidu.com")

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
        //bulletin_refresh.setOnRefreshListener(this)
        bulletin_refresh.setProgressViewOffset(false, 0, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24F, resources.displayMetrics).toInt())
        bulletin_refresh.onRefresh() {
            if (mIsRefreshing)
                Utils.showLog("Ignore manually update!")
            else {
                Utils.showLog("Pull down to update!")

                mIsRefreshingFromPullDown = true
                getItemsFromBulletin()
            }
        }

        mRecyclerView = mRootView!!.bulletin_list
        mRecyclerView!!.hasFixedSize()
        mRecyclerView!!.layoutManager = LinearLayoutManager(mContext)
        mRecyclerView!!.itemAnimator = DefaultItemAnimator()
        mRecyclerView!!.adapter = mBulletinAdapter

        getItemsFromBulletin()
    }

    /** 从云上获取数据之前先进行网络判断 */
    private fun getItemsFromBulletin() {
        if (Utils.isNetWorkConnected(mContext!!))
            QueryBulletin()
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

    private fun QueryBulletin() {
        /** 导入数据时进度显示-true，隐藏-false */
        bulletin_refresh.isRefreshing = true
        mIsRefreshing = true
        val query = BmobQuery<Bulletin>()
        query.setLimit(50)
        /**
         * 让最新的数据显示在列表上方，用更新时间作为依据是最准确的
         * 初次加载让结果数据降序排列，下拉刷新升序排列（测试发现默认为升序）
         */
        if (mIsRefreshingFromPullDown)
            query.order("updatedAt")
        else
            query.order("-updatedAt")
        if(mBulletinCount > 0) {
            var lastDate = mBulletins!![0].bulletinTime
            var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            var date: Date? = null
            try {
                date = sdf.parse(lastDate)
                date.seconds += 1
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            query.addWhereGreaterThan("updatedAt", BmobDate(date))
        }
        query.findObjects(object: FindListener<Bulletin>() {
            override fun done(`object`: List<Bulletin>, e: BmobException?) {
                if (e == null) {
                    Utils.showLog("查询成功: 共" + `object`.size + "条数据")

                    for (bulletin in `object`) {
                        var bulletinT = ReceiveBulletin(bulletin.teacherName,
                                bulletin.updatedAt,
                                bulletin.bulletinContent,
                                bulletin.bulletinImage?.fileUrl)
                        if (mIsRefreshingFromPullDown)
                            mBulletins!!.add(0, bulletinT)
                        else
                            mBulletins!!.add(bulletinT)
                    }
                    if (mBulletins!!.size > mBulletinCount) {
                        mBulletinCount = mBulletins!!.size

                        mBulletinAdapter!!.notifyDataSetChanged()
                    }
                } else
                    Utils.showLog("失败: " + e.message + "," + e.errorCode)
                bulletin_refresh.isRefreshing = false
                mIsRefreshing = false
                mIsRefreshingFromPullDown = false
            }
        })
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