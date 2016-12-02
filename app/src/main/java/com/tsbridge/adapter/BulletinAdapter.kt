package com.tsbridge.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.tsbridge.R
import com.tsbridge.entity.ReceiveBulletin
import com.tsbridge.fragment.SendFragment
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.bulletin_item.view.*
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.onClick
import java.io.File

class BulletinAdapter(private val mContext: Context,
                      private val mBulletins: List<ReceiveBulletin>?)
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

    override fun getItemCount() = mBulletins!!.size

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        init {
            itemView.bulletin_content_image.onClick {
                showFullImage()
            }
        }

        fun bindItemView(position: Int) {
            QueryImageTask(itemView.bulletin_image).execute(mBulletins!![position].bulletinImage)
            itemView.bulletin_name.text = mBulletins[position].teacherName
            itemView.bulletin_time.text = mBulletins[position].teacherTime
            itemView.bulletin_content.text = mBulletins[position].bulletinContent
            /* check if this is the last child, if yes then hide the divider, remember -1 */
            if (position == itemCount - 1)
                itemView.bulletin_divider.visibility = View.GONE
        }

        private fun showFullImage() {
            val dialog = Dialog(mContext, R.style.DialogTitle)
            dialog.setContentView(mContext.linearLayout {
                imageView {
                    imageBitmap = BitmapFactory.decodeFile(SendFragment.picturePath)
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
                Glide.with(mContext).load(R.drawable.bulletin_image).into(mBulletinImage)
                return
            }
            Glide.with(mContext).load(result).into(mBulletinImage)
        }
    }
}
