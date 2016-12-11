package com.tsbridge.fragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.Selection
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobFile
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UploadFileListener
import com.tsbridge.R
import com.tsbridge.activity.NetworkActivity
import com.tsbridge.activity.PermissionActivity
import com.tsbridge.entity.User
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.login_fragment.*
import org.jetbrains.anko.onCheckedChange
import java.io.File

class LoginFragment: Fragment(), View.OnClickListener {
    private val SELECT_PIC_LOW = 121
    private val SELECT_PIC_KITKAT = 122

    /** 邮箱和手机信息没有加入 */
    private var mLoginName = ""
    private var mLoginPsw = ""
    private var mLoginImageUri: Uri? = null

    private var mIsBackFromNetworkIn = false
    private var mIsBackFromNetworkReg = false
    private var mIsBackFromNetworkOut = false
    private var mIsBackFromPermission = false

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
                                : View? {
        Utils.showLog("LoginFragment onCreateView")

        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.showLog("LoginFragment onActivityCreated")

        initialization()
    }

    private fun initialization() {
        login_image_sel.setOnClickListener(this@LoginFragment)
        login_in.setOnClickListener(this@LoginFragment)
        login_reg.setOnClickListener(this@LoginFragment)
        login_out.setOnClickListener(this@LoginFragment)
        login_psw_check.onCheckedChange { compoundButton, b ->
            if (b)
                login_psw.transformationMethod = HideReturnsTransformationMethod.getInstance()
            else
                login_psw.transformationMethod = PasswordTransformationMethod.getInstance()
            var psw = login_psw.text
            Selection.setSelection(psw, psw.length)
        }

        /** 如果用户已登录，则显示名称与头像信息 */
        setLoginInfo()
    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.login_image_sel -> selectImageBtn()
            R.id.login_in -> loginInBtn()
            R.id.login_reg -> loginRegBtn()
            R.id.login_out -> loginOutBtn()
            else -> { }
        }
    }

    private fun setLoginInfo() {
        if(BmobUser.getCurrentUser() != null) {
            login_name.setText(BmobUser.getCurrentUser().username)
            Utils.setImageToView(activity, BmobUser.getCurrentUser().username, null, login_image)
        }
    }

    /**
     * 选择图片: Intent.ACTION_GET_CONTENT、ACTION_OPEN_DOCUMENT、ACTION_PICK
     * 相机拍照：MediaStore.ACTION_IMAGE_CAPTURE
     */
    private fun selectImageBtn() {
        if(BmobUser.getCurrentUser() != null) {
            Utils.showToast(activity, activity.getString(R.string.login_yes_image))
            return
        }
        var storagePermission = Utils.checkPermission(activity,
                Utils.EXTERNAL_STORAGE_PERMISSION)
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Utils.showLog("Has not External storage Permission")

            var permissionAccessTimes = Utils.getPermissionAccessTimes(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY,
                    1)
            if (permissionAccessTimes > 1 &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION)) {
                /** 用户拒绝了权限申请并选择了不再显示 */
                mIsBackFromPermission = true
                val intent = Intent(activity, PermissionActivity::class.java)
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
                Utils.setPermissionAccessTimes(activity,
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
                    Utils.showToast(activity, activity.getString(R.string.permission_denied))
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
                    mLoginImageUri = intent?.data
                    if (mLoginImageUri != null)
                        imagePreview(mLoginImageUri!!)
                    else
                        Utils.showLog("Uri with returned intent is null")
                }
                else -> { }
            }
        } else {
            Utils.showLog("No selected image")
            Utils.showToast(activity, activity.getString(R.string.no_selected_image))
        }
    }

    private fun imagePreview(uri: Uri) {
        var picturePath = Utils.getPath(activity, uri)
        Utils.showLog(picturePath)
        Utils.setImageToView(activity, null, picturePath, login_image)
    }

    /** 用户注册 */
    private fun loginRegBtn() {
        if (Utils.isNetWorkConnected(activity))
            loginReg()
        else {
            mIsBackFromNetworkReg = true
            startActivity(Intent(activity, NetworkActivity::class.java))
        }
    }

    private fun loginReg() {
        if(BmobUser.getCurrentUser() != null) {
            Utils.showToast(activity, activity.getString(R.string.login_yes))
            return
        }
        /** 发送者名称需要从其用户信息中读取，故只有注册用户才能发送 */
        mLoginName = login_name.text.toString()
        mLoginPsw = login_psw.text.toString()
        if (TextUtils.isEmpty(mLoginName) || TextUtils.isEmpty(mLoginPsw)
                || mLoginImageUri == null) {
            Utils.showToast(activity, activity.getString(R.string.login_info_3))
            return
        }
        /** 获取路径一定要用 Utils 中定义的方法，如果使用 uri.path 不同 SDK 结果不同 */
        val file = BmobFile(File(Utils.getPath(activity, mLoginImageUri!!)))
        file.uploadblock(object : UploadFileListener() {
            override fun done(e: BmobException?) {
                if (e == null) {
                    Utils.showLog("Upload image succeed")

                    insertItemToUser(file)
                } else {
                    Utils.showLog("Upload image failed: " + e.message
                            + " Error code: " + e.errorCode)
                    Utils.showToast(activity,
                            activity.getString(R.string.login_reg_failed))
                }
            }
        })
    }

    private fun insertItemToUser(file: BmobFile) {
        val user = User(file)
        user.username = mLoginName
        user.setPassword(mLoginPsw)
        user.signUp(object : SaveListener<User>() {
            override fun done(s: User?, e: BmobException?) {
                if (s != null && e == null) {
                    Utils.showLog("Register User succeed")
                    Utils.showToast(activity,
                            activity.getString(R.string.login_reg_succeed))

                    login_psw.setText("")
                } else {
                    Utils.showLog("Register User failed: " + e?.message
                            + " Error code: " + e?.errorCode)
                    Utils.showToast(activity,
                            activity.getString(R.string.login_reg_failed)+ ": " + e?.message)
                }
            }
        })
    }

    /** 用户登录 */
    private fun loginInBtn() {
        if (Utils.isNetWorkConnected(activity))
            loginIn()
        else {
            mIsBackFromNetworkIn = true
            startActivity(Intent(activity, NetworkActivity::class.java))
        }
    }

    private fun loginIn() {
        if(BmobUser.getCurrentUser() != null) {
            Utils.showToast(activity, activity.getString(R.string.login_yes))
            return
        }
        mLoginName = login_name.text.toString()
        mLoginPsw = login_psw.text.toString()
        if (TextUtils.isEmpty(mLoginName) || TextUtils.isEmpty(mLoginPsw)) {
            Utils.showToast(activity, activity.getString(R.string.login_info_2))
            return
        }
        var user = User(null)
        user.username = mLoginName
        user.setPassword(mLoginPsw)
        user.login(object : SaveListener<User>() {
            override fun done(s: User?, e: BmobException?) {
                if (s != null && e == null) {
                    Utils.showLog("Login in succeed")
                    Utils.showToast(activity,
                            activity.getString(R.string.login_in_succeed))

                    login_psw.setText("")
                    setLoginInfo()
                } else {
                    Utils.showLog("Login in failed: " + e?.message + " Error code: " + e?.errorCode)
                    Utils.showToast(activity,
                            activity.getString(R.string.login_in_failed)+ ": " + e?.message)
                }
            }
        })
    }

    /** 用户注销 */
    private fun loginOutBtn() {
        if (Utils.isNetWorkConnected(activity))
            loginOut()
        else {
            mIsBackFromNetworkOut = true
            startActivity(Intent(activity, NetworkActivity::class.java))
        }
    }

    private fun loginOut() {
        if(BmobUser.getCurrentUser() == null) {
            Utils.showToast(activity, activity.getString(R.string.login_no))
            return
        }
        BmobUser.logOut()

        Utils.showLog("Login out succeed: ")
        Utils.showToast(activity,
                activity.getString(R.string.login_out_succeed))

        login_name.setText("")
        Utils.setImageToView(activity, null, null, login_image)
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("LoginFragment onResume")

        /** 新打开的 Activity 在当前 onResume 执行后才开始创建 */
        if (mIsBackFromNetworkReg && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetworkReg = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(activity))
                loginReg()
            else
                Utils.showToast(activity,
                        activity.getString(R.string.no_connected_network))
        }

        if (mIsBackFromNetworkIn && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetworkIn = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(activity))
                loginIn()
            else
                Utils.showToast(activity,
                        activity.getString(R.string.no_connected_network))
        }

        if (mIsBackFromNetworkOut && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetworkOut = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(activity))
                loginOut()
            else
                Utils.showToast(activity,
                        activity.getString(R.string.no_connected_network))
        }

        if (mIsBackFromPermission && Utils.mIsBackFromSetPermission) {
            mIsBackFromPermission = false
            Utils.mIsBackFromSetPermission = false
            var storagePermission = Utils.checkPermission(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION)
            if (storagePermission == PackageManager.PERMISSION_GRANTED)
                loginReg()
            else {
                Utils.showLog("Storage permission access failed")
                Utils.showToast(activity, activity.getString(R.string.permission_denied))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils.showLog("LoginFragment onDestroyView")
    }
}