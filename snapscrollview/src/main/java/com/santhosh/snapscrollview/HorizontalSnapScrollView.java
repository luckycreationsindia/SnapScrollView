package com.santhosh.snapscrollview;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

/**
 * Created by santhosh-3366 on 18/01/17.
 */

public class HorizontalSnapScrollView extends ViewGroup {

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;

    private int mLastYPos;
    private int mLastXPos;

    private int mMaximumHeight;
    private int mMaximumWidth;

    private int mPageHeight;
    private int mPageWidth;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private int mChildHeight;
    private int mChildWidth;
    private int mCurrentPage;
    private int mOverscrollDistance;
    private int mOverFlingDistance;
    private int mTouchSlop;
    private int mActivePointerId;
    private int mScrollMode = 2;

    private boolean mIsBeingDragged, isDrag;
    private boolean snap;

    private int childTopMargin = 0;
    private int childBottomMargin = 0;
    private int childLeftMargin = 0;
    private int childRightMargin = 0;

    private static final int SNAP_VELOCITY = 500;
    private static final int BOTTOM = 1;
    private static final int INVALID_POINTER = -1;
    private static final int DEFAULT_DURATION = 800;

    public HorizontalSnapScrollView(Context context) {
        this(context, null);
    }

    public HorizontalSnapScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalSnapScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray a = context.obtainStyledAttributes(attrs, com.santhosh.snapscrollview.R.styleable.SnapScrollView, defStyleAttr, 0);
        setScrollMode(a.getInteger(R.styleable.SnapScrollView_mScrollmode, 2));
        setSnap(a.getBoolean(R.styleable.SnapScrollView_snap, true));
        setChildTopMargin((int) a.getDimension(R.styleable.SnapScrollView_childTopMargin, 0));
        setChildLeftMargin((int) a.getDimension(R.styleable.SnapScrollView_childTLeftMargin, 0));
        setChildBottomMargin((int) a.getDimension(R.styleable.SnapScrollView_childBottomMargin, 0));
        setChildRightMargin((int) a.getDimension(R.styleable.SnapScrollView_childRightMargin, 0));
    }

    private void init() {
        mScroller = new OverScroller(getContext(), new ViscousFluidInterpolator());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverFlingDistance = configuration.getScaledOverflingDistance();
        mTouchSlop = configuration.getScaledTouchSlop();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }

    }


    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int count = getChildCount();
        mMaximumHeight = 0;
        mMaximumWidth = 0;
        childLeftMargin = 10;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.measure(0, 0);
            LayoutParams params = child.getLayoutParams();
            child.requestLayout();
            mChildHeight = mChildHeight > params.height ? mChildHeight : params.height + child.getPaddingTop() + child.getPaddingBottom() + childTopMargin + childBottomMargin;
            mChildWidth = mChildWidth > params.width ? mChildWidth : params.width + child.getPaddingLeft() + child.getPaddingRight() + childLeftMargin + childRightMargin;
            if (mScrollMode == 2) {
                child.layout(mMaximumWidth, 0, mMaximumWidth + params.width, params.height);
                mMaximumWidth += mChildWidth;
                mMaximumHeight = getMeasuredHeight();
            }
        }
        scrollTo(getScrollX(), 0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                if (!inChild(x, y)) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                mLastXPos = x;
                mLastYPos = y;

                mActivePointerId = ev.getPointerId(0);
                initVelocityTracker();
                mVelocityTracker.addMovement(ev);
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    break;
                }
                final int x = (int) ev.getX(pointerIndex);
                final int xDiff = Math.abs(x - mLastYPos);

                final int y = (int) ev.getY(pointerIndex);
                if ((xDiff > mTouchSlop && mScrollMode == 2)) {
                    mIsBeingDragged = true;
                    mLastXPos = x;
                    mLastYPos = y;
                    initVelocityTracker();
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                int diffX = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - mChildWidth) / 2;
                if (mScroller.springBack(getScrollX(), getScrollY(), -diffX, getHorizontalScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTracker();
        mVelocityTracker.addMovement(ev);

        int action = ev.getActionMasked();
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (getChildCount() == 0) {
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastXPos = x;
                mLastYPos = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }
                horizontalMove(x);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTrack = mVelocityTracker;
                    velocityTrack.computeCurrentVelocity(1000, mMaximumVelocity);
                        int velocityX = (int) velocityTrack.getXVelocity(mActivePointerId);
                        computeHorizontalScroll(velocityX);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    if (mScrollMode == 1) {
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
                            postInvalidateOnAnimation();
                        }
                    } else {
                        int diffX = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - mChildWidth) / 2;
                        if (mScroller.springBack(getScrollX(), getScrollY(), -diffX, getHorizontalScrollRange(), 0, 0)) {
                            postInvalidateOnAnimation();
                        }
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
        }
        return true;
    }

    private void horizontalMove(int x) {
        int diffX = mLastXPos - x;
        isDrag = isDrag || Math.abs(diffX) > mTouchSlop;
        if (!mIsBeingDragged && Math.abs(diffX) > mTouchSlop) {
            final ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            mIsBeingDragged = true;
            if (diffX > 0) {
                diffX -= mTouchSlop;
            } else {
                diffX += mTouchSlop;
            }
        }
        if (mIsBeingDragged) {
            mLastXPos = x;
            if (isDrag) {
                scrollBy(diffX, 0);
            }
            if (overScrollBy(diffX,0, getScrollX(), 0, getHorizontalScrollRange(), 0, mOverscrollDistance,0, true)) {
                mVelocityTracker.clear();
            }
        }
    }

    private void computeHorizontalScroll(int velocityX) {
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int offsetX = getfinalOffsetX(getScrollX(), getScrollY(), -velocityX, 0, 0, Math.max(0, mMaximumWidth - width),0,0);
        float childNum = (float) (offsetX) / mChildWidth;
        int childNo = Math.floor(childNum) > getChildCount() - 1 ? getChildCount() - 1 : (int) Math.floor(childNum);
        View child = getChildAt(childNo);
        if (child != null) {
            int deltaX = child.getRight();
            if (childNo == getChildCount() - 1) {
                deltaX = child.getRight() - getMeasuredWidth() - getScrollX();
            }
            String type = "FLING";//No I18N
            if (Math.abs(velocityX) > mMinimumVelocity) {
                mCurrentPage = childNo+1;
                if (velocityX >= 0) {
                    //Scroll Leftwards
                    type = "SNAP";//No I18N
                    if (offsetX <= 0) {
                        type = "LEFT";//No I18N
                        mCurrentPage = 0;
                    }else if (deltaX > getScrollX()) {
                        while (deltaX > getScrollX()) {
                            deltaX -= mChildWidth;
                            mCurrentPage--;
                        }
                    }
                } else if (velocityX < 0) {
                    //Scroll Righwards
                    type = "SNAP";//No I18N
                    if (getScrollX() > mMaximumWidth - width || deltaX > mMaximumWidth - width) {
                        type = "RIGHT";//No I18N
                    } else if (deltaX < getScrollX()) {
                        deltaX += mChildWidth;
                    }
                }
            }
            switch (type) {
                case "LEFT"://No I18N
                    snapToStart();
                    mCurrentPage=0;
                    break;
                case "RIGHT"://No I18N
                    snapToEnd(offsetX);
                    mCurrentPage=getChildCount()-1;
                    break;
                case "SNAP"://No I18N
                    horizontalSnap(deltaX);
                    break;
                case "FLING"://No I18N
                    horizontalFling(velocityX);
                    break;
            }

        }
    }

    private void snapToStart(){
        int diffX = (getWidth()-getPaddingLeft()-getPaddingRight()-mChildWidth)/2;
        mScroller.startScroll(getScrollX(), getScrollY(), -(getScrollX()+diffX), 0, DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void snapToEnd(int offsetX){
        mScroller.startScroll(getScrollX(), getScrollY(), offsetX-getScrollX(), 0, DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void horizontalSnap(int deltaX){
        int diffX = (getWidth()-getPaddingLeft()-getPaddingRight()-mChildWidth)/2;
        diffX = deltaX==0? 0 : diffX;
        mScroller.startScroll(getScrollX(), getScrollY(), deltaX-getScrollX()-diffX, 0, DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void horizontalFling(int velocityX){
        flingHorizontally(-velocityX);
    }

    public void flingHorizontally(int velocityX) {
        if (getChildCount() > 0) {
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int diffX = (width-mChildWidth)/2;
            mScroller.fling(getScrollX(), getScrollY(), velocityX, 0, -diffX, Math.max(0, mMaximumWidth - width), 0, 0, width / 2, 0);
            postInvalidateOnAnimation();
        }
    }


    private void endDrag() {
        mIsBeingDragged = false;
        isDrag = false;
        recycleVelocityTracker();
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            recycleVelocityTracker();
        }
    }

    private int getHorizontalScrollRange(){
        int scrollRange = 0;
        if (getChildCount() > 0) {
            scrollRange = Math.max(0, mMaximumWidth - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }


    @Override
    public void setOverScrollMode(int mode) {
        super.setOverScrollMode(mode);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY || y >= child.getBottom() - scrollY
                    || x < child.getLeft() || x >= child.getRight());
        }
        return false;
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            scrollRange = Math.max(0, mMaximumHeight - (getHeight() - getPaddingBottom() - getPaddingTop()));
        }
        return scrollRange;
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = getHeight() - getPaddingBottom() - getPaddingTop();
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, Math.max(0, mMaximumHeight - height), 0, height / 2);
            postInvalidateOnAnimation();
        }
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        // Continue a scroll or fling in progress
        float velocity = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        float coeffX = velocity == 0 ? 1.0f : velocityX / velocity;
        float coeffY = velocity == 0 ? 1.0f : velocityY / velocity;

        double totalDistance = getSplineFlingDistance(velocity);
        int mDistance = (int) (totalDistance * Math.signum(velocity));

        int mMinX = minX;
        int mMaxX = maxX;
        int mMinY = minY;
        int mMaxY = maxY;

        int mFinalX = startX + (int) Math.round(totalDistance * coeffX);
        // Pin to mMinX <= mFinalX <= mMaxX
        mFinalX = Math.min(mFinalX, mMaxX);
        mFinalX = Math.max(mFinalX, mMinX);

        int mFinalY = startY + (int) Math.round(totalDistance * coeffY);
        // Pin to mMinY <= mFinalY <= mMaxY
        mFinalY = Math.min(mFinalY, mMaxY);
        mFinalY = Math.max(mFinalY, mMinY);
    }

    public int getfinalOffsetX(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        if (!mScroller.isFinished()) {
            float oldVel = mScroller.getCurrVelocity();
            float dx = (float) (mScroller.getFinalX() - mScroller.getStartX());
            float dy = (float) (mScroller.getFinalY() - mScroller.getStartY());
            float hyp = (float) Math.sqrt(dx * dx + dy * dy);

            float ndx = dx / hyp;
            float ndy = dy / hyp;

            float oldVelocityX = ndx * oldVel;
            float oldVelocityY = ndy * oldVel;
            if (Math.signum(velocityX) == Math.signum(oldVelocityX) &&
                    Math.signum(velocityY) == Math.signum(oldVelocityY)) {
                velocityX += oldVelocityX;
                velocityY += oldVelocityY;
            }
        }
        // Continue a scroll or fling in progress
        float velocity = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        float coeffX = velocity == 0 ? 1.0f : velocityX / velocity;

        double totalDistance = getSplineFlingDistance(velocity);

        int mFinalX = startX + (int) Math.round(totalDistance * coeffX);
        // Pin to mMinX <= mFinalX <= mMaxX
        mFinalX = Math.min(mFinalX, maxX);
        mFinalX = Math.max(mFinalX, minX);
        return mFinalX;
    }

    private int getSplineFlingDuration(float velocity) {
        double physicalCoef = SensorManager.GRAVITY_EARTH * 39.37 * (getContext().getResources().getDisplayMetrics().density * 160.0f) * 0.84;
        double splineDeceleration = Math.log(0.35f * Math.abs(velocity) / (ViewConfiguration.getScrollFriction() * physicalCoef));
        double deceleration = (float) (Math.log(0.78) / Math.log(0.9));
        final double decelMinusOne = deceleration - 1.0;
        return (int) (1000.0 * Math.exp(splineDeceleration / decelMinusOne));
    }

    public double getSplineFlingDistance(float velocity) {
        double physicalCoef = SensorManager.GRAVITY_EARTH * 39.37 * (getContext().getResources().getDisplayMetrics().density * 160.0f) * 0.84;
        double splineDeceleration = Math.log(0.35f * Math.abs(velocity) / (ViewConfiguration.getScrollFriction() * physicalCoef));
        double deceleration = (float) (Math.log(0.78) / Math.log(0.9));
        double decelMinusOne = deceleration - 1.0;
        double splineFlingDistance = ViewConfiguration.getScrollFriction() * physicalCoef * Math.exp(deceleration / decelMinusOne * splineDeceleration);
        return splineFlingDistance;
    }


    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int mCurrentPage) {
        this.mCurrentPage = mCurrentPage;
        invalidate();
    }

    public int getChildRightMargin() {
        return childRightMargin;
    }

    public void setChildRightMargin(int childRightMargin) {
        this.childRightMargin = childRightMargin;
        invalidate();
    }

    public int getChildLeftMargin() {
        return childLeftMargin;
    }

    public void setChildLeftMargin(int childLeftMargin) {
        this.childLeftMargin = childLeftMargin;
        invalidate();
    }

    public int getChildBottomMargin() {
        return childBottomMargin;
    }

    public void setChildBottomMargin(int childBottomMargin) {
        this.childBottomMargin = childBottomMargin;
        invalidate();
    }

    public int getChildTopMargin() {
        return childTopMargin;
    }

    public void setChildTopMargin(int childTopMargin) {
        this.childTopMargin = childTopMargin;
        invalidate();
    }

    public int getPageHeight() {
        return mPageHeight;
    }

    public void setPageHeight(int mPageHeight) {
        this.mPageHeight = mPageHeight;
        invalidate();
    }

    public int getMaximumHeight() {
        return mMaximumHeight;
    }

    public void setMaximumHeight(int mMaximumHeight) {
        this.mMaximumHeight = mMaximumHeight;
        invalidate();
    }

    public int getChildHeight() {
        return mChildHeight;
    }

    public void setChildHeight(int mChildHeight) {
        this.mChildHeight = mChildHeight;
        invalidate();
    }

    public void setChildMargins(int left, int top, int right, int bottom) {
        childLeftMargin = left;
        childTopMargin = top;
        childRightMargin = right;
        childBottomMargin = bottom;
        invalidate();
    }

    public int getScrollMode() {
        return mScrollMode;
    }

    public void setScrollMode(int mScrollMode) {
        this.mScrollMode = mScrollMode;
    }

    public boolean isSnap() {
        return snap;
    }

    public void setSnap(boolean snap) {
        this.snap = snap;
    }

    static class ViscousFluidInterpolator implements Interpolator {
        /**
         * Controls the viscous fluid effect (how much of it).
         */
        private static final float VISCOUS_FLUID_SCALE = 6.0f;

        private static final float VISCOUS_FLUID_NORMALIZE;
        private static final float VISCOUS_FLUID_OFFSET;

        static {

            // must be set to 1.0 (used in viscousFluid())
            VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
            // account for very small floating-point error
            VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
        }

        private static float viscousFluid(float x) {
            x *= VISCOUS_FLUID_SCALE;
            if (x < 1.0f) {
                x -= (1.0f - (float) Math.exp(-x));
            } else {
                float start = 0.36787944117f;   // 1/e == exp(-1)
                x = 1.0f - (float) Math.exp(1.0f - x);
                x = start + x * (1.0f - start);
            }
            return x;
        }

        @Override
        public float getInterpolation(float input) {
            final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
            if (interpolated > 0) {
                return interpolated + VISCOUS_FLUID_OFFSET;
            }
            return interpolated;
        }
    }
}
