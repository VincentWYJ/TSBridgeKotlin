package com.tsbridge.entity

import cn.bmob.v3.BmobObject

/**
 * teacherName String 发布者名称
 * bulletinTime String 发布时间
 * bulletinContent String 文本内容
 * bulletinImage String 图片图片
 * 注：在类外部有可能访问成员变量的，需要声明为var/val，否则访问不到
 * 如 BulletinFragment 类获取数据传给 BulletinAdapter 后，需要将值取出显示在列表
 */
class ReceiveBulletin(val teacherName: String,
                      val bulletinTime: String,
                      val bulletinContent: String?,
                      val bulletinImage: String?): BmobObject() {
    init {
        //Utils.showLog("Create a ReceiveBulletin object")
    }
}
