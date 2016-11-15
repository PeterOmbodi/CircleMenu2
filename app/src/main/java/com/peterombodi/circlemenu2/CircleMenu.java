package com.peterombodi.circlemenu2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

class CircleMenu extends View {
    private static final String TAG = "CircleMenu";

    public static final int FLING_VELOCITY_DOWNSCALE = 22;
    private static final int HOME_ANGLE = 240;

    private RectF rectF;
    private Paint paintForeground;

    private GestureDetector mDetector;
    private List<Item> itemList = new ArrayList<>();
    private int pieColor;
    private int pieSelectedColor;
    private float viewWidth;
    private float viewHeight;
    private int currentAngle;
    private ValueAnimator anim;
    private ValueAnimator mScrollAnimator;
    private boolean isScroll;
    private Scroller mScroller;

    public CircleMenu(Context context) {
        super(context);
        init(context, null);
    }

    public CircleMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CircleMenu(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {

        paintForeground = new Paint();
        mDetector = new GestureDetector(CircleMenu.this.getContext(), new GestureListener());
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.CircleMenu,
                0, 0
        );
        try {
            pieColor = a.getInt(R.styleable.CircleMenu_pieColor, getResources().getColor(R.color.colorPie));
            pieSelectedColor = a.getInt(R.styleable.CircleMenu_pieSelectedColor, getResources().getColor(R.color.colorSelectedPie));
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }
        setWillNotDraw(false);
        // Set up the paint
        paintForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintForeground.setStyle(Paint.Style.FILL);
        paintForeground.setAntiAlias(true);
        paintForeground.setColor(pieColor);

        final ViewTreeObserver observer = this.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
//                if (!playStarted) playAnimation();
            }
        });

        mScroller = new Scroller(getContext(), new DecelerateInterpolator(), true);
        mScrollAnimator = ValueAnimator.ofFloat(0, 1);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                tickScrollAnimation();
            }
        });


    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(80, 102, 204, 255);

        for (Item it : itemList) {
            paintForeground.setColor(it.selected ? pieSelectedColor : pieColor);
            canvas.drawArc(rectF, it.startAngle, it.angle, true, paintForeground);


//            rotateDrawable =  it.icon;
//            int top =(int) (rectF.height()/6)*4;
//            int left =(int) rectF.width()/3;
//            rotateDrawable.setBounds(top, left, top+left/2, left+left/2);
            //rotateDrawable.setToDegrees(60f);
//            canvas.rotate(60, rectF.centerX(),rectF.centerY());

            // Log.d(TAG, "onDraw it.id= " + it.id + "/ it.startAngle =" + it.startAngle + "/ it.angle = " + it.angle);
        }

    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final String TAG = "GestureListener";

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            isScroll = true;
            float scrollTheta = vectorToScalarScroll(
                    distanceX,
                    distanceY,
                    e2.getX() - rectF.centerX(),
                    e2.getY() - rectF.centerY());

            currentAngle = currentAngle - (int) scrollTheta / FLING_VELOCITY_DOWNSCALE;
            onItemsChanged();
            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "___________***GestureListener.onFling");
            isScroll = true;
            // Set up the Scroller for a fling
            float scrollTheta = vectorToScalarScroll(
                    velocityX,
                    velocityY,
                    e2.getX() - rectF.centerX(),
                    e2.getY() - rectF.centerY());

            mScroller.fling(
                    0,
                    normalizeAngel(currentAngle),
                    0,
                    (int) scrollTheta / FLING_VELOCITY_DOWNSCALE,
                    0,
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE);

            mScroller.extendDuration(mScroller.getDuration());
            mScroller.setFinalY(mScroller.getFinalY());
            // mScroller.setFriction(20);

            mScrollAnimator.setDuration(mScroller.getDuration());
            mScrollAnimator.start();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            boolean inCircle = inCircle(x, y, rectF);
            if (inCircle && anim != null && anim.isRunning()) anim.cancel();
            return inCircle;
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        boolean result = mDetector.onTouchEvent(event);
        if (inCircle(x, y, rectF)
                && !result
                && event.getAction() == MotionEvent.ACTION_UP
                && !isScroll) {
            selectSector(x, y);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            isScroll = false;
        }
        return result;
    }

    private void selectSector(float x, float y) {
        int s = inSector(x, y, rectF);
        // TODO: 15.11.2016 if (s==0) ???
        if (s > 0) {
            int a = itemList.get(s - 1).startAngle; //нач. выбранного сектора
            int b = HOME_ANGLE; // куда идем
            int c = normalizeAngel(b - a); // угол поворота
            int d = normalizeAngel(c + currentAngle); //конечный угол начала меню
            currentAngle = normalizeAngel(currentAngle); //текущий угол начала меню
            if (Math.abs(currentAngle - d) > 180) {
                if (currentAngle > d) {
                    currentAngle = currentAngle - 360;
                } else {
                    d = d - 360;
                }
            }
            animation(currentAngle, d, 1000);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewHeight = h;
        viewWidth = w;
        float squareSize = ((viewHeight > viewWidth) ? viewWidth : viewHeight);
        // TODO: 15.11.2016 set border size
        rectF = new RectF(50, 50, squareSize - 50, squareSize - 50);

        currentAngle = HOME_ANGLE;

        addItem(getResources().getDrawable(android.R.drawable.ic_btn_speak_now));
        addItem(getResources().getDrawable(android.R.drawable.ic_menu_call));
        addItem(getResources().getDrawable(android.R.drawable.ic_media_rew));
        addItem(getResources().getDrawable(android.R.drawable.ic_media_play));
        addItem(getResources().getDrawable(android.R.drawable.ic_media_ff));
        addItem(getResources().getDrawable(android.R.drawable.ic_media_pause));

    }

    private boolean inCircle(float x, float y, RectF rectF) {
        float r = rectF.width() / 2;
        float dx = x - rectF.centerX();
        float dy = y - rectF.centerY();
        boolean result = (r * r) >= (dx * dx + dy * dy);
        return result;
    }

    private int inSector(float x, float y, RectF rectF) {
        float dx = x - rectF.centerX();
        float dy = y - rectF.centerY();
        double result = (180 * Math.atan2(dy, dx)) / Math.PI;
        result = (result > 0) ? result : 360 + result;
        int retVal = 0;
        for (Item it : itemList) {
            it.selected = false;
            if (it.startAngle <= result && it.startAngle + it.angle >= result) {
                retVal = it.id;
                it.selected = true;
            }
        }
        return retVal;
    }


    private class Item {

        private int id;
        private int color;
        private int startAngle;
        private int angle;
        private boolean selected;
        private Drawable icon;

    }

    private int addItem(Drawable icon) {
        Item it = new Item();
        it.id = itemList.size() + 1;
        it.selected = (itemList.size() == 0);
        it.icon = icon;
        itemList.add(it);
        onItemsChanged();
        return itemList.size() - 1;

    }

    private void onItemsChanged() {

        int total = itemList.size();
        int startAngle = currentAngle;
        int endAngle;
        int sectorAngle = (int) (360.0f / total);

        for (Item it : itemList) {
            //it.startAngle = currentAngle;
            it.startAngle = normalizeAngel(startAngle);
            endAngle = (int) ((float) startAngle + sectorAngle);
            it.angle = sectorAngle;
            startAngle = endAngle;
        }
    }

    private void animation(int start, int end, int duration) {

        anim = ValueAnimator.ofInt(start, end);
        anim.setDuration(duration);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                currentAngle = (int) animation.getAnimatedValue();
                onItemsChanged();
                Log.d(TAG, "______***onAnimationUpdate.currentAngle" + currentAngle);
                invalidate();
            }
        });
        anim.start();
    }

    private static float vectorToScalarScroll(float dx, float dy, float x, float y) {
        // get the length of the vector
        float l = (float) Math.sqrt(dx * dx + dy * dy);

        // decide if the scalar should be negative or positive by finding
        // the dot product of the vector perpendicular to (x,y).
        float crossX = -y;
        float crossY = x;

        float dot = (crossX * dx + crossY * dy);
        float sign = Math.signum(dot);
        return l * sign;
    }

    private int normalizeAngel(int angel) {
        return (angel % 360 + 360) % 360;
    }

    private void tickScrollAnimation() {
        if (!mScroller.isFinished()) {
            mScroller.computeScrollOffset();
            setSectorRotation(mScroller.getCurrY());
            Log.d(TAG, "tickScrollAnimation mScroller.getCurrY=" + mScroller.getCurrY() + "/ rotateAngle =" + currentAngle);
        } else {
            mScrollAnimator.cancel();
            //onScrollFinished();
        }
    }

    public void setSectorRotation(int rotation) {
        currentAngle = normalizeAngel(rotation);
        onItemsChanged();
        invalidate();
    }


}