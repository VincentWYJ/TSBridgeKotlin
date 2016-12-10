package com.tsbridge.adapter

import android.app.Dialog
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bumptech.glide.Glide
import com.tsbridge.R
import com.tsbridge.entity.ReceiveBulletin
import com.tsbridge.entity.User
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.bulletin_item.view.*
import kotlinx.android.synthetic.main.login_fragment.*
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.onClick

class BulletinAdapter(private val mContext: Context,
                      private val mBulletins: List<ReceiveBulletin>)
        : RecyclerView.Adapter<BulletinAdapter.ViewHolder>() {
    init {
        Utils.showLog("Create a BulletinAdapter object")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bulletinItem = LayoutInflater.from(mContext)
                .inflate(R.layout.bulletin_item, parent, false)
        val viewHolder = ViewHolder(bulletinItem)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItemView(position)
    }

    override fun getItemCount() = mBulletins.size

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        fun bindItemView(position: Int) {
            itemView.bulletin_name.text = mBulletins[position].teacherName
            itemView.bulletin_time.text = mBulletins[position].bulletinTime
            itemView.bulletin_content.text = mBulletins[position].bulletinContent
            Utils.setImage(mContext, mBulletins[position].teacherName,itemView.bulletin_image)
            /** 若公告图片内容为 null ，则隐藏让其不可点击 */
            if (mBulletins[position].bulletinImage == null)
                itemView.bulletin_content_image.visibility = View.GONE
            else {
                itemView.bulletin_content_image.visibility = View.VISIBLE
                itemView.bulletin_content_image.onClick {
                    showFullImage(mBulletins[position].bulletinImage)
                }
                Glide.with(mContext.applicationContext)
                        .load(mBulletins[position].bulletinImage)
                        .into(itemView.bulletin_content_image)
            }
            /**
             * Check if this is the last child, if yes then hide the divider, remember -1
             * else show it, remember to show every element if use notifyDataSetChanged to update list
             * */
            if (position == itemCount - 1)
                itemView.bulletin_divider.visibility = View.GONE
            else
                itemView.bulletin_divider.visibility = View.VISIBLE
        }

        private fun showFullImage(imageUri: String?) {
            val dialog = Dialog(mContext, R.style.DialogTitle)
            dialog.setContentView(mContext.linearLayout {
                imageView {
                    Glide.with(mContext.applicationContext).load(imageUri).into(this)
                    onClick {
                        dialog.dismiss()
                    }
                }
            })

            dialog.show()
        }
    }
}
