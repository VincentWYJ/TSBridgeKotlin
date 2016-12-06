package com.tsbridge.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.util.AttributeSet
import android.widget.ImageView
import com.tsbridge.R

class CircleImageView: ImageView {
    /** 图片显示的模式，按比例缩放图片，使得图片长 (宽)的大于等于视图的相应维度 */
    private val SCALE_TYPE = ImageView.ScaleType.CENTER_CROP
    /**
     * ARGB指的是一种色彩模式，里面A代表Alpha，R表示red，G表示green，B表示blue,
     * ALPHA_8        代表8位Alpha位图
     * ARGB_4444      代表16位ARGB位图
     * ARGB_8888     代表32位ARGB位图
     * RGB_565         代表8位RGB位图
     */
    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val COLORDRAWABLE_DIMENSION = 2
    /** 圆形image的边框大小  */
    private val DEFAULT_BORDER_WIDTH = 0
    /** 圆形image的边框颜色，默认为黑色  */
    private val DEFAULT_BORDER_COLOR = Color.BLACK
    /** 填充颜色，默认为：透明  */
    private val DEFAULT_FILL_COLOR = Color.TRANSPARENT
    private val DEFAULT_BORDER_OVERLAY = false

    private val mDrawableRect = RectF()
    private val mBorderRect = RectF()
    private val mShaderMatrix = Matrix()
    private val mBitmapPaint = Paint()
    private val mBorderPaint = Paint()
    private val mFillPaint = Paint()
    /** 边框颜色  */
    private var mBorderColor = DEFAULT_BORDER_COLOR
    /** 边框大小  */
    private var mBorderWidth = DEFAULT_BORDER_WIDTH
    /** 填充颜色  */
    private var mFillColor = DEFAULT_FILL_COLOR
    private var mBitmap: Bitmap? = null
    /**
     * BitmapShader的作用就是通过Paint对画布进行置顶Bitmap的填充，填充时有以下几种模式可以选择：
     * 1.CLAMP 拉伸 拉伸的是图片最后的那一个像素，不断重复
     * 2.REPEAT 重复 横向、纵向不断重复
     * 3.MIRROR 镜像 横向不断翻转重复，纵向不断翻转重复
     */
    private var mBitmapShader: BitmapShader? = null
    private var mBitmapWidth: Int = 0
    private var mBitmapHeight: Int = 0
    private var mDrawableRadius: Float = 0.toFloat()
    private var mBorderRadius: Float = 0.toFloat()
    private var mColorFilter: ColorFilter? = null
    private var mReady: Boolean = false
    private var mSetupPending: Boolean = false
    private var mBorderOverlay: Boolean = false
    var isDisableCircularTransformation: Boolean = false
        set(disableCircularTransformation) {
            if (isDisableCircularTransformation == disableCircularTransformation)
                return
            field = disableCircularTransformation
            initializeBitmap()
        }

    /** 在 java 代码中直接 new 自定义组件对象时，调用一参构造函数 */
    constructor(context: Context): super(context) {
        initialization()
    }

    /** 在 xml 布局中添加组件时，调用的是两参构造函数，进而调用三参函数 */
    @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyle: Int = 0)
                                : super(context, attrs, defStyle) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView,
                defStyle, 0)
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_circle_border_width,
                DEFAULT_BORDER_WIDTH)
        mBorderColor = a.getColor(R.styleable.CircleImageView_circle_border_color,
                DEFAULT_BORDER_COLOR)
        mBorderOverlay = a.getBoolean(R.styleable.CircleImageView_circle_border_overlay,
                DEFAULT_BORDER_OVERLAY)
        mFillColor = a.getColor(R.styleable.CircleImageView_circle_fill_color,
                DEFAULT_FILL_COLOR)
        a.recycle()
        initialization()
    }

    private fun initialization() {
        super.setScaleType(SCALE_TYPE)
        mReady = true
        if (mSetupPending) {
            setup()
            mSetupPending = false
        }
    }

    override fun getScaleType(): ImageView.ScaleType {
        return SCALE_TYPE
    }

    override fun setScaleType(scaleType: ImageView.ScaleType) {
        if (scaleType != SCALE_TYPE)
            throw IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType))
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (adjustViewBounds)
            throw IllegalArgumentException("adjustViewBounds not supported.")
    }

    override fun onDraw(canvas: Canvas) {
        if (isDisableCircularTransformation) {
            super.onDraw(canvas)
            return
        }
        if (mBitmap == null)
            return
        if (mFillColor != Color.TRANSPARENT)
            canvas.drawCircle(mDrawableRect.centerX(),
                    mDrawableRect.centerY(),
                    mDrawableRadius,
                    mFillPaint)  //填充
        canvas.drawCircle(mDrawableRect.centerX(),
                mDrawableRect.centerY(),
                mDrawableRadius,
                mBitmapPaint)  //画图片
        if (mBorderWidth > 0)
            canvas.drawCircle(mBorderRect.centerX(),
                    mBorderRect.centerY(),
                    mBorderRadius,
                    mBorderPaint)  //边框
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    var borderColor: Int
        get() = mBorderColor
        set(@ColorInt borderColor) {
            if (borderColor == mBorderColor)
                return
            mBorderColor = borderColor
            mBorderPaint.color = mBorderColor
            invalidate()
        }


    /**
     * Set a color to be drawn behind the circle-shaped drawable. Note that
     * this has no effect if the drawable is opaque or no drawable is set.

     * @param fillColor The color to be drawn behind the drawable
     * *
     * *
     */
    @Deprecated("")
    fun setFillColor(@ColorInt fillColor: Int) {
        if (fillColor == mFillColor)
            return
        mFillColor = fillColor
        mFillPaint.color = fillColor
        invalidate()
    }

    /**
     * Set a color to be drawn behind the circle-shaped drawable. Note that
     * this has no effect if the drawable is opaque or no drawable is set.

     * @param fillColorRes The color resource to be resolved to a color and
     * *                     drawn behind the drawable
     * *
     * *
     */
    @Deprecated("")
    fun setFillColorResource(@ColorRes fillColorRes: Int) {
        setFillColor(context.resources.getColor(fillColorRes))
    }

    var borderWidth: Int
        get() = mBorderWidth
        set(borderWidth) {
            if (borderWidth == mBorderWidth)
                return
            mBorderWidth = borderWidth
            setup()
        }

    var isBorderOverlay: Boolean
        get() = mBorderOverlay
        set(borderOverlay) {
            if (borderOverlay == mBorderOverlay)
                return
            mBorderOverlay = borderOverlay
            setup()
        }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    /** Glide 给 ImageView 赋予图像时会回调该方法  */
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === mColorFilter)
            return
        mColorFilter = cf
        applyColorFilter()
        invalidate()
    }

    override fun getColorFilter(): ColorFilter? {
        return mColorFilter
    }

    private fun applyColorFilter() {
        if (mBitmapPaint != null)
            mBitmapPaint.colorFilter = mColorFilter
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null)
            return null
        if (drawable is BitmapDrawable)
            return drawable.bitmap
        try {
            val bitmap: Bitmap
            if (drawable is ColorDrawable)
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION,
                        COLORDRAWABLE_DIMENSION,
                        BITMAP_CONFIG)
            else
                bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        BITMAP_CONFIG)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    private fun initializeBitmap() {
        if (isDisableCircularTransformation)
            mBitmap = null
        else
            mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    private fun setup() {
        if (!mReady) {
            mSetupPending = true
            return
        }
        if (width == 0 && height == 0)
            return
        if (mBitmap == null) {
            invalidate()
            return
        } else {
            mBitmapShader = BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            mBitmapPaint.isAntiAlias = true
            mBitmapPaint.shader = mBitmapShader
            mBorderPaint.style = Paint.Style.STROKE
            mBorderPaint.isAntiAlias = true
            mBorderPaint.color = mBorderColor
            mBorderPaint.strokeWidth = mBorderWidth.toFloat()
            mFillPaint.style = Paint.Style.FILL
            mFillPaint.isAntiAlias = true
            mFillPaint.color = mFillColor
            mBitmapHeight = mBitmap!!.height
            mBitmapWidth = mBitmap!!.width
            mBorderRect.set(calculateBounds())
            mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2.0f,
                    (mBorderRect.width() - mBorderWidth) / 2.0f)
            mDrawableRect.set(mBorderRect)
            if (!mBorderOverlay && mBorderWidth > 0)
                mDrawableRect.inset(mBorderWidth - 1.0f, mBorderWidth - 1.0f)
            mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)
            applyColorFilter()
            /** updateShaderMatrix();  */
            invalidate()
        }
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom
        val sideLength = Math.min(availableWidth, availableHeight)
        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f
        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0f
        var dy = 0f
        mShaderMatrix.set(null)
        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = mDrawableRect.height() / mBitmapHeight.toFloat()
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f
        } else {
            scale = mDrawableRect.width() / mBitmapWidth.toFloat()
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f
        }
        mShaderMatrix.setScale(scale, scale)
        mShaderMatrix.postTranslate((dx + 0.5f).toInt() + mDrawableRect.left, (dy + 0.5f).toInt() + mDrawableRect.top)
        mBitmapShader!!.setLocalMatrix(mShaderMatrix)
    }
}
