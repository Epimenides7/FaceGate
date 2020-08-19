package com.wudi.facegate.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.apkfuns.logutils.LogUtils;

/**
 * 人脸识别区域View
 * Created by wudi on 2020/6/14.
 */

public class IdentifyAreasView extends View{
    private Paint paint;
    private String color = "#282FE3";//颜色
    private float left;
    private float top;
    private float right;
    private float bottom;

    public IdentifyAreasView(Context context) {
        super(context);
        initPaint();
    }

    public IdentifyAreasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public IdentifyAreasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    private void initPaint(){
        paint = new Paint();
        paint.setColor(Color.parseColor(color));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f, getContext().getResources().getDisplayMetrics()));
        paint.setPathEffect(new DashPathEffect(new float[] {20, 10}, 0));
        paint.setAntiAlias(true);//抗锯齿
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = canvas.getHeight();
        float width = canvas.getWidth();
        top = height/6f;
        bottom = height - (height/6f);
        left = width/5f;
        right = width - (width/5f);
//        canvas.drawRect(left,top,right,bottom,paint);
        canvas.drawOval(left,top,right,bottom,paint);
        float y = bottom - ((bottom-top)/2f);
        canvas.drawLine(left,y,right,y,paint);
//        canvas.drawText("人脸识别区域",left+150f,top - 30f,textPaint);
    }

    /**
     * 获取识别区域
     * @return
     */
    public float[] getArea(){
        return new float[]{left,top,right,bottom};
    }

}
