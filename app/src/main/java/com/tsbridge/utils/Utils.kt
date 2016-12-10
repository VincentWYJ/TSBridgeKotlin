package com.tsbridge.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bumptech.glide.Glide
import com.tsbridge.R
import com.tsbridge.entity.User
import kotlinx.android.synthetic.main.login_fragment.*

object Utils {
    init {
        showLog("Create a Utils object")
    }

    val LOG_TAG = "TSBridge"

    /** 可以直接使用 Anko 提供的 toast(message: CharSequence) 方法
     * 注：需要 Context 上下文环境
     * */
    fun showToast(context: Context, message: Any) {
        Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show()
    }

    /** 打印日志，若 message 为 null，那么调用 toString() 后返回 "null" */
    fun showLog(message: Any?) {
        Log.i(LOG_TAG, message?.toString())
    }

    fun setImage(context: Context, username: String, imageview: ImageView) {
        val query = BmobQuery<User>()
        query.addWhereEqualTo("username", username)
        query.findObjects(object: FindListener<User>() {
            override fun done(`object`: List<User>, e: BmobException?) {
                if (e == null) {
                    showLog("查询成功: 共" + `object`.size + "条数据")

                    Glide.with(context.applicationContext)
                            .load(`object`[0].imageFile?.fileUrl)
                            .into(imageview)
                } else
                    showLog("失败: " + e.message + "," + e.errorCode)
            }
        })
    }

    /**
     * *******************************************************************************************1
     * 功能简述: 以 SDk 4.4 版本分段获取正确的图片路径
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true))
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                                java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type)
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else if ("video" == type)
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else if ("audio" == type)
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                if (contentUri != null)
                    return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri))
                return uri.lastPathSegment
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true))
            return uri.path
        return null
    }

    fun getDataColumn(context: Context, uri: Uri, selection: String?,
                      selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection,
                    selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * *******************************************************************************************2
     * 功能简述: 为 SDk 6.0 以上版本添加权限申请
     */
    val CAMERA_PERMISSION_ACCESS_TIMES_KEY = "CameraPermissionAccessTimesKey"
    val CAMERA_PERMISSION = Manifest.permission.CAMERA
    val REQUEST_CODE_ASK_CAMERA_PERMISSION = 111

    val EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY = "ExternalStoragePermissionAccessTimesKey"
    val EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    val REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION = 112

    val READ_PHONE_STATE_PERMISSION_ACCESS_TIMES_KEY = "ReadPhoneStateAccessTimesKey"
    val READ_PHONE_STATE_PERMISSION = Manifest.permission.READ_PHONE_STATE
    val REQUEST_CODE_ASK_READ_PHONE_STATE_PERMISSION = 113

    var mIsBackFromSetPermission: Boolean = false
    var mIsBackFromSetNetwork: Boolean = false

    val PERMISSION_TITLE = "PermissionTitle"
    val PERMISSION_EXPLAIN = "PermissionExplain"
    val PERMISSION_NAME = "PermissionName"

    /** 检测网络连接状态 */
    fun isNetWorkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /** 检查有权限是否已经申请 */
    fun checkPermission(context: Context, permission: String): Int {
        return ContextCompat.checkSelfPermission(context, permission)
    }

    /** 显示自定义 */
    fun showPermissionDialog(context: Context,
                             message: String,
                             okListener: DialogInterface.OnClickListener,
                             cancelListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, okListener)
                .setNegativeButton(R.string.cancel, cancelListener)
                .setCancelable(false)
                .create()
                .show()
    }

    /** 进入应用对应的settings页面 */
    private val SCHEME = "package"
    private val APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName"
    private val APP_PKG_NAME_22 = "pkg"
    private val APP_DETAILS_PACKAGE_NAME = "com.android.settings"
    private val APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails"
    fun showInstalledAppDetails(context: Context) {
        val packageName = context.packageName
        val apiLevel = Build.VERSION.SDK_INT
        val intent = Intent()
        if (apiLevel >= 9) {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts(SCHEME, packageName, null)
            intent.data = uri
        } else {
            val appPkgName = if (apiLevel == 8) APP_PKG_NAME_22 else APP_PKG_NAME_21
            intent.action = Intent.ACTION_VIEW
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setClassName(APP_DETAILS_PACKAGE_NAME, APP_DETAILS_CLASS_NAME)
            intent.putExtra(appPkgName, packageName)
        }
        context.startActivity(intent)
    }

    /**
     * 获取指定权限的申请次数
     * permissionAccessTimesKey--某权限的字串KEY
     * defaultPermissionAccessTimes--初始默认次数值，一般为1
     */
    fun getPermissionAccessTimes(context: Context,
                                 permissionAccessTimesKey: String,
                                 defaultPermissionAccessTimes: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val permissionAccessTimes = sharedPreferences.getInt(permissionAccessTimesKey,
                defaultPermissionAccessTimes)
        return permissionAccessTimes
    }

    /**
     * 设置指定权限的申请次数
     * permissionAccessTimesKey--某权限的字串KEY
     * defaultPermissionAccessTimes--当前已申请次数值
     */
    fun setPermissionAccessTimes(context: Context,
                                 permissionAccessTimesKey: String,
                                 currentPermissionAccessTimes: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putInt(permissionAccessTimesKey, currentPermissionAccessTimes)
        editor.commit()
    }
}
