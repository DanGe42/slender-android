package io.dge.slender;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

// Code adapted from http://sunil-android.blogspot.com/2013/02/create-our-android-compass.html
public class Compass extends View {
    private Paint circlePaint, arrowPaint;
    private float diameter;
    private double direction;
    private Path triangle;
    private Path arrow;

    public Compass(Context context) {
        this(context, null);
    }

    public Compass(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Compass(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4);
        circlePaint.setColor(Color.WHITE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(8);
        arrowPaint.setColor(Color.RED);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int radius = Math.min(width, height) / 2;

        canvas.drawLine(
                width / 2,
                height / 2,
                (float) (width / 2 + radius * Math.sin(-direction)),
                (float) (height / 2 - radius * Math.cos(-direction)),
                arrowPaint);

        canvas.drawCircle(width / 2, height / 2, radius, circlePaint);
    }

    public void update(Direction dir){
        switch (dir) {
            case NORTH:     direction = 0.0; break;
            case NORTHEAST: direction = Math.PI / 4; break;
            case EAST:      direction = Math.PI / 2; break;
            case SOUTHEAST: direction = 3 * Math.PI / 4; break;
            case SOUTH:     direction = Math.PI; break;
            case SOUTHWEST: direction = 5 * Math.PI / 4; break;
            case WEST:      direction = 3 * Math.PI / 2; break;
            case NORTHWEST: direction = 7 * Math.PI / 2; break;
        }
        invalidate();
    }
}
