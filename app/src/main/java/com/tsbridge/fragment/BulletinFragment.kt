package com.tsbridge.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
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
import org.jetbrains.anko.support.v4.onRefresh
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BulletinFragment: Fragment() {
    private val mBulletins = ArrayList<ReceiveBulletin>()
    private var mBulletinAdapter: BulletinAdapter? = null

    private var mIsBackFromNetwork = false
    private var mIsRefreshing = false
    private var mIsRefreshingFromPullDown = false
    private var mBulletinCount = 0

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Utils.showLog("BulletinFragment onCreateView")

        return inflater.inflate(R.layout.bulletin_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.showLog("BulletinFragment onActivityCreated")

        initialization()
    }

    private fun initialization() {
        bulletin_list.hasFixedSize()
        bulletin_list.itemAnimator = DefaultItemAnimator()
        bulletin_list.layoutManager = LinearLayoutManager(activity)
        mBulletinAdapter = BulletinAdapter(activity, mBulletins)
        bulletin_list.adapter = mBulletinAdapter
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

        getItemsFromBulletin()
    }

    /** 从云上获取数据之前先进行网络判断 */
    private fun getItemsFromBulletin() {
        if (Utils.isNetWorkConnected(activity))
            QueryBulletin()
        else {
            mIsBackFromNetwork = true
            startActivity(Intent(activity, NetworkActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("BulletinFragment onResume")

        if (mIsBackFromNetwork && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetwork = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(activity))
                QueryBulletin()
            else {
                Utils.showToast(activity,
                        activity.getString(R.string.no_connected_network))
                bulletin_refresh.isRefreshing = false
                mIsRefreshing = false
                mIsRefreshingFromPullDown = false
            }
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
            var lastDate = mBulletins[0].bulletinTime
            var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            var date: Date? = null
            try {
                date = sdf.parse(lastDate)
                /**
                 * 由于createdAt、updatedAt是服务器自动生成的时间，
                 * 在服务器保存的是精确到微秒值的时间，所以，基于时间类型的比较的值要加1秒
                 * Date 的 getter/setter 方法都不支持了，建议是用 Calendar.get(Calendar.SECOND)
                 */
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
                            mBulletins.add(0, bulletinT)
                        else
                            mBulletins.add(bulletinT)
                    }
//                    `object`.map {
//                        var bulletinT = ReceiveBulletin(it.teacherName,
//                                it.updatedAt,
//                                it.bulletinContent,
//                                it.bulletinImage?.fileUrl)
//                        if (mIsRefreshingFromPullDown)
//                            mBulletins.add(0, bulletinT)
//                        else
//                            mBulletins.add(bulletinT)
//                    }
                    if (mBulletins.size > mBulletinCount) {
                        mBulletinCount = mBulletins.size
                        mBulletinAdapter?.notifyDataSetChanged()
                    }
                    if (mBulletins.size == 0 && bulletin_empty.visibility == View.GONE)
                        bulletin_empty.visibility = View.VISIBLE
                    else if (mBulletins.size > 0 && bulletin_empty.visibility == View.VISIBLE)
                        bulletin_empty.visibility = View.GONE
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

        mBulletins.clear()
    }
}