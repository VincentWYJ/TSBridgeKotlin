package com.tsbridge.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bmob.v3.datatype.BmobFile
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UploadFileListener
import com.bumptech.glide.Glide
import com.tsbridge.R
import com.tsbridge.activity.NetworkActivity
import com.tsbridge.activity.PermissionActivity
import com.tsbridge.entity.Bulletin
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.send_fragment.view.*
import java.io.File

class SendFragment: Fragment(), View.OnClickListener {
    /**
     * 网上说 Kotlin 有 platformStatic 关键字模拟 Java static
     * public platformStatic val NUMBER : Int = 1
     * 虽然目前没有找到，不过下面这种方式已经能满足需求了
     * SendFragment.Companion.getPicturePath()
     * 注：companion 类型变量一般在需对其他类公开成员时使用
     */
    companion object {
        var picturePath: String? = null
    }

    private val SELECT_PIC_LOW = 121
    private val SELECT_PIC_KITKAT = 122

    private var mContext: Context? = null

    private var sendName: String? = null
    private var sendContent: String? = null
    private var sendImageUri: Uri? = null

    private var mRootView: View? = null

    private var mIsBackFromNetwork = false
    private var mIsBackFromPermission = false

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
                                : View? {
        Utils.showLog("SendFragment onCreateView")

        mRootView = inflater!!.inflate(R.layout.send_fragment, container, false)
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.showLog("SendFragment onActivityCreated")

        initParams()
        initViews()
    }

    private fun initParams() {
        mContext = activity
    }

    private fun initViews() {
        mRootView!!.send_image_sel.setOnClickListener(this@SendFragment)
        mRootView!!.send_image_del.setOnClickListener(this@SendFragment)
        mRootView!!.send_btn.setOnClickListener(this@SendFragment)
    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.send_image_sel -> selectImageBtn()
            R.id.send_image_del -> clearImage()
            R.id.send_btn -> sendBulletinBtn()
            else -> { }
        }
    }

    /**
     * 选择图片: Intent.ACTION_GET_CONTENT、ACTION_OPEN_DOCUMENT、ACTION_PICK
     * 相机拍照：MediaStore.ACTION_IMAGE_CAPTURE
     */
    private fun selectImageBtn() {
        var storagePermission = Utils.checkPermission(mContext!!,
                Utils.EXTERNAL_STORAGE_PERMISSION)
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Utils.showLog("Has not External storage Permission")

            var permissionAccessTimes = Utils.getPermissionAccessTimes(mContext!!,
                    Utils.EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY,
                    1)
            if (permissionAccessTimes > 1 &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION)) {
                /** 用户拒绝了权限申请并选择了不再显示 */
                mIsBackFromPermission = true
                val intent = Intent(mContext, PermissionActivity::class.java)
                intent.putExtra(Utils.PERMISSION_TITLE,
                        getString(R.string.permission_external_storage_title))
                intent.putExtra(Utils.PERMISSION_EXPLAIN,
                        getString(R.string.permission_external_storage_explain))
                intent.putExtra(Utils.PERMISSION_NAME, Utils.EXTERNAL_STORAGE_PERMISSION)
                startActivity(intent)
                return
            }
            if (permissionAccessTimes == 1) {
                permissionAccessTimes++
                Utils.setPermissionAccessTimes(mContext!!,
                        Utils.EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY,
                        permissionAccessTimes)
            }
            Utils.showLog("Show system dialog to allow access storage")

            requestPermissions(arrayOf(Utils.EXTERNAL_STORAGE_PERMISSION),
                    Utils.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION)
            return
        } else {
            selectImage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            Utils.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utils.showLog("Manifest.permission.WRITE_EXTERNAL_STORAGE access succeed")

                    selectImage()
                } else {
                    Utils.showLog("Manifest.permission.WRITE_EXTERNAL_STORAGE access failed")
                    Utils.showToast(mContext!!, mContext!!.getString(R.string.permission_denied))
                }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun selectImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PIC_KITKAT)
        } else {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PIC_LOW)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Utils.showLog("Back to onActivityResult from getting image")

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SELECT_PIC_KITKAT, SELECT_PIC_LOW -> {
                    sendImageUri = intent!!.data
                    if (sendImageUri != null)
                        imagePreview(sendImageUri!!)
                    else
                        Utils.showLog("Uri with returned intent is null")
                }
                else -> { }
            }
        } else {
            Utils.showLog("No selected image")
            Utils.showToast(mContext!!, mContext!!.getString(R.string.no_selected_image))
        }
    }

    private fun imagePreview(uri: Uri) {
        picturePath = Utils.getPath(mContext!!, uri)
        Utils.showLog(picturePath!!)
        Glide.with(mContext).load(picturePath).into(mRootView!!.send_image)
    }

    private fun clearImage() {
        if (sendImageUri != null) {
            sendImageUri = null
            picturePath = null
            mRootView!!.send_image.setImageResource(R.drawable.black)
        }
    }

    /** 添加数据到云上之前先进行网络判断 */
    private fun sendBulletinBtn() {
        if (Utils.isNetWorkConnected(mContext!!))
            insertItem()
        else {
            mIsBackFromNetwork = true
            startActivity(Intent(mContext, NetworkActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("SendFragment onResume")

        /** 新打开的 Activity 在当前 onResume 执行后才开始创建 */
        if (mIsBackFromNetwork && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetwork = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(mContext!!))
                insertItem()
            else
                Utils.showToast(mContext!!,
                        mContext!!.getString(R.string.no_connected_network))
        }

        if (mIsBackFromPermission && Utils.mIsBackFromSetPermission) {
            mIsBackFromPermission = false
            Utils.mIsBackFromSetPermission = false
            var storagePermission = Utils.checkPermission(mContext!!,
                    Utils.EXTERNAL_STORAGE_PERMISSION)
            if (storagePermission == PackageManager.PERMISSION_GRANTED)
                selectImage()
            else {
                Utils.showLog("Storage permission access failed")
                Utils.showToast(mContext!!, mContext!!.getString(R.string.permission_denied))
            }
        }
    }

    private fun insertItem() {
        sendName = mRootView!!.send_name.text.toString()
        sendContent = mRootView!!.send_content.text.toString()
        if (TextUtils.isEmpty(sendName) ||
                (TextUtils.isEmpty(sendContent) && sendImageUri == null)) {
            Utils.showToast(mContext!!, mContext!!.getString(R.string.no_inputted_content))
            return
        }
        if (picturePath != null) {
            val file = BmobFile(File(picturePath))
            file.uploadblock(object : UploadFileListener() {
                override fun done(e: BmobException?) {
                    if (e == null)
                        Utils.showLog("Upload image succeed")
                    else
                        Utils.showLog("Upload image failed：" + e.message + "," + e.errorCode)
                    insertItemToBulletin(file)
                }
            })
        } else
            insertItemToBulletin(null)
    }

    fun insertItemToBulletin(file: BmobFile?) {
        val bulletin = Bulletin(sendName!!, sendContent, file)
        bulletin.save(object: SaveListener<String>() {
            override fun done(objectId: String, e: BmobException?) {
                if (e == null) {
                    Utils.showLog("Insert bulletin succeed：" + objectId)
                    Utils.showToast(mContext!!,
                            mContext!!.getString(R.string.send_succeed))
                } else
                    Utils.showLog("Insert bulletin failed：" + e.message + "," + e.errorCode)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils.showLog("SendFragment onDestroyView")
    }
}