package com.tsbridge.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    private val SELECT_PIC_KITKAT = 121
    private val SELECT_PIC_LOW = 122

    private var mContext: Context? = null

    private var sendName: String? = null
    private var sendContent: String? = null
    private var sendImageUri: Uri? = null

    private var mRootView: View? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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
        mRootView!!.send_image_sel.setOnClickListener(this)
        mRootView!!.send_image_del.setOnClickListener(this)
        mRootView!!.send_btn.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.send_image_sel -> selectImage()
            R.id.send_image_del -> clearImage()
            R.id.send_btn -> SendBulletin()
            else -> {
            }
        }
    }

    /**
     * 选择图片: Intent.ACTION_GET_CONTENT、ACTION_OPEN_DOCUMENT、ACTION_PICK
     * 相机拍照：MediaStore.ACTION_IMAGE_CAPTURE
     */
    private fun selectImage() {
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

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SELECT_PIC_KITKAT, SELECT_PIC_LOW -> {
                    sendImageUri = intent!!.data
                    if (sendImageUri != null) {
                        imagePreview(sendImageUri!!)
                    } else {
                        Utils.showLog("Uri with returned intent is null")
                    }
                }
                else -> {
                }
            }
        } else {
            Utils.showToast(mContext!!, "没有选择图片!")
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
            mRootView!!.send_image.setImageResource(R.drawable.bulletin_image)
        }
    }

    private fun SendBulletin() {
        sendName = mRootView!!.send_name.text.toString()
        sendContent = mRootView!!.send_content.text.toString()
        if (!TextUtils.isEmpty(sendName) && (!TextUtils.isEmpty(sendContent) || sendImageUri != null)) {
            addDataToBulletin()
        } else {
            Utils.showToast(mContext!!, "请输入名称和内容(或图片)")
        }
    }

    private fun addDataToBulletin() {
        val file = BmobFile(File(picturePath!!))
        file.uploadblock(object: UploadFileListener() {

            override fun done(e: BmobException?) {
                if (e == null) {
                    Utils.showLog("图片上传成功")
                    Utils.showLog(file.fileUrl)
                } else {
                    Utils.showLog("图片上传失败：" + e.message + "," + e.errorCode)
                }

                val bulletin = Bulletin(sendName!!, sendContent, file)
                bulletin.save(object: SaveListener<String>() {
                    override fun done(objectId: String, e: BmobException?) {
                        if (e == null) {
                            Utils.showLog("创建数据成功：" + objectId)
                        } else {
                            Utils.showLog("创建数据失败：" + e.message + "," + e.errorCode)
                        }
                    }
                })
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Utils.showLog("SendFragment onDestroyView")
    }
}