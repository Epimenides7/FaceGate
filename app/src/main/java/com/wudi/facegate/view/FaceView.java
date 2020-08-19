package com.wudi.facegate.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 人脸图像绘制view
 * Created by wudi on 2020/4/14.
 */
public class FaceView extends View {
    private Paint paint;//画笔
    private String color = "#42ed45";//颜色
    private List<RectF> faces = new ArrayList<>();//人脸信息

    public FaceView(Context context) {
        super(context);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化画笔
     */
    private void init(){
        paint = new Paint();
        paint.setColor(Color.parseColor(color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, getContext().getResources().getDisplayMetrics()));
        paint.setAntiAlias(true);//抗锯齿
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF rectF : faces){
            if(rectF == null){
                continue;
            }
            canvas.drawRect(rectF,paint);
        }
    }

    /**
     * 传入脸坐标  并刷新
     * @param faces
     */
    public void setFaces(List<RectF> faces){
        this.faces.clear();
        this.faces.addAll(faces);
        invalidate();
    }

    /**
     * 清除人脸
     */
    public void clearFace(){
        this.faces.clear();
        invalidate();
    }


}
