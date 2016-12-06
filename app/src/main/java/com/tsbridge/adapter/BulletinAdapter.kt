package com.tsbridge.adapter

import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.tsbridge.R
import com.tsbridge.entity.ReceiveBulletin
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.bulletin_item.view.*
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.onClick
import java.io.File

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
            QueryImageTask(itemView.bulletin_image).execute(mBulletins[position].bulletinImage)
            /** 若公告图片内容为 null ，则隐藏让其不可点击 */
            if (mBulletins[position].bulletinImage == null)
                itemView.bulletin_content_image.visibility = View.GONE
            else {
                itemView.bulletin_content_image.visibility = View.VISIBLE
                itemView.bulletin_content_image.onClick {
                    showFullImage(mBulletins[position].bulletinImage)
                }
                QueryImageTask(itemView.bulletin_content_image)
                        .execute(mBulletins[position].bulletinImage)
            }
            itemView.bulletin_name.text = mBulletins[position].teacherName
            itemView.bulletin_time.text = mBulletins[position].bulletinTime
            itemView.bulletin_content.text = mBulletins[position].bulletinContent
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
                    QueryImageTask(this).execute(imageUri)
                    onClick {
                        dialog.dismiss()
                    }
                }
            })

            dialog.show()
        }
    }

    private inner class QueryImageTask(val mBulletinImage: ImageView?)
            : AsyncTask<String, Void, File>() {
        override fun doInBackground(vararg params: String): File? {
            val imageUrl = params[0]
            val imageSize = mContext.resources.getDimensionPixelSize(R.dimen.bulletin_image)
            try {
                return Glide.with(mContext).load(imageUrl).downloadOnly(imageSize, imageSize).get()
            } catch (ex: Exception) {
                return null
            }
        }

        override fun onPostExecute(result: File?) {
            if (result == null) {
                Utils.showLog("Image file is not existed")
                Glide.with(mContext).load(R.drawable.bulletin_image).into(mBulletinImage)
                return
            }
            Glide.with(mContext).load(result).into(mBulletinImage)
        }
    }
}
