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
 * Created by santhosh-3366 on 19/01/17.
 */

public class SnapScrollView extends ViewGroup {

    private android.widget.OverScroller mScroller;
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

    private int mCurrentPage = 0;
    private int mOverscrollDistance;
    private int mOverFlingDistance;
    private int mTouchSlop;
    private int mActivePointerId;
    private int mScrollMode = 1;

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

    public SnapScrollView(Context context) {
        this(context, null);
    }

    public SnapScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SnapScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray a = context.obtainStyledAttributes(attrs, com.santhosh.snapscrollview.R.styleable.SnapScrollView, defStyleAttr, 0);
        setScrollMode(a.getInteger(R.styleable.SnapScrollView_mScrollmode, 1));
        setSnap(a.getBoolean(R.styleable.SnapScrollView_snap,true));
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
        mPageWidth = mPageWidth != 0 ? mPageWidth : getMeasuredWidth();
        mPageHeight = mPageHeight != 0 ? mPageHeight : getMeasuredHeight();
        setMeasuredDimension(mPageWidth, getMeasuredHeight());
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }
        scrollTo(getScrollX(), getScrollY());
    }


    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        int count = getChildCount();
        mMaximumHeight = 0;
        mMaximumWidth = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.measure(0, 0);
            ViewGroup.LayoutParams params = child.getLayoutParams();
            child.requestLayout();
            mChildHeight = mChildHeight > params.height ? mChildHeight : params.height + child.getPaddingTop() + child.getPaddingBottom() + childTopMargin + childBottomMargin;
            mChildWidth = mChildWidth > params.width ? mChildWidth : params.width + child.getPaddingLeft() + child.getPaddingRight() + childLeftMargin + childRightMargin;
            if (mScrollMode == 1) {
                child.layout(0, mMaximumHeight, params.width, mMaximumHeight + params.height);
                mMaximumHeight += mChildHeight;
                mMaximumWidth = getMeasuredWidth();
                mPageWidth = mChildWidth;
                mPageHeight = mChildHeight;
            } else {
                child.layout(mMaximumWidth, 0, mMaximumWidth + params.width, params.height);
                mMaximumWidth += mChildWidth;
                mMaximumHeight = getMeasuredHeight();
                mPageHeight = mChildHeight;
                mPageWidth = mChildWidth;
            }
        }
        if (mScrollMode == 1) {
            scrollTo(0, getChildAt(mCurrentPage).getBottom());
        } else {
            int diffX = (getWidth() - getPaddingLeft() - getPaddingRight() - mChildWidth) / 2;
            scrollTo(getChildAt(mCurrentPage).getRight() - diffX, 0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        if (getScrollY() == 0 && !canScrollVertically(1) && mScrollMode == 1) {
            return false;
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
                final int yDiff = Math.abs(y - mLastYPos);
                if ((yDiff > mTouchSlop && mScrollMode == 1)) {
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
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
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
                verticalMove(y);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTrack = mVelocityTracker;
                    velocityTrack.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocityY = (int) velocityTrack.getYVelocity(mActivePointerId);
                    if (snap) {
                        computeVerticalScroll(velocityY);
                    }else {
                        flingVertically(-velocityY);
                    }
                }
                break;
        }
        return true;
    }

    private void verticalMove(int y) {
        int diffY = mLastYPos - y;
        isDrag = isDrag || Math.abs(diffY) > mTouchSlop;
        if (!mIsBeingDragged) {
            final ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            mIsBeingDragged = true;
            if (diffY > 0) {
                diffY -= mTouchSlop;
            } else {
                diffY += mTouchSlop;
            }
        }
        if (mIsBeingDragged) {
            mLastYPos = y;
            if (isDrag) {
                scrollBy(0, diffY);
            }
            if (overScrollBy(0, diffY, 0, getScrollY(), 0, getScrollRange(), 0, mOverscrollDistance, true)) {
                mVelocityTracker.clear();
            }
        }
    }

    private void computeVerticalScroll(int velocityY) {
        int height = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
        int offsetY = getfinalOffsetY(getScrollX(), getScrollY(), 0, -velocityY, 0, 0, 0, Math.max(0, mMaximumHeight - height));
        float childNum = (float) (offsetY) / mChildHeight;
        int childNo = Math.floor(childNum) > getChildCount() - 1 ? getChildCount() - 1 : (int) Math.floor(childNum);
        View child = getChildAt(childNo);
        if (child != null) {
            int deltaY = child.getBottom();
            if (childNo == getChildCount() - 1) {
                deltaY = child.getBottom() - getMeasuredHeight() - getScrollY();
            }

            mCurrentPage = childNo;
            String type = "FLING";
            if (Math.abs(velocityY) > mMinimumVelocity) {
                if (velocityY >= 0) {
                    //Scroll Upwards
                    type = "SNAP";
                    if (deltaY > getScrollY()) {
                        while (deltaY > getScrollY()) {
                            deltaY -= mChildHeight;
                        }
                    } else if (offsetY <= 0) {
                        type = "TOP";
                    }
                } else if (velocityY < 0) {
                    //Scroll Downwards
                    type = "SNAP";
                    if (deltaY < getScrollY()) {
                        deltaY += mChildHeight;
                    } else if (getScrollY() > mMaximumHeight - height || deltaY > mMaximumHeight - height) {
                        type = "BOTTOM";
                    }
                }
            }
            switch (type) {
                case "TOP":
                    snapToTop();
                    break;
                case "BOTTOM":
                    snapToBottom(offsetY);
                    break;
                case "SNAP":
                    verticalSnap(deltaY);
                    break;
                case "FLING":
                    verticalFling(velocityY);
                    break;
            }

        }
    }

    private void snapToTop() {
        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void snapToBottom(int offsetY) {
        mScroller.startScroll(getScrollX(), getScrollY(), 0, offsetY - getScrollY(), DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void verticalSnap(int deltaY) {
        mScroller.startScroll(getScrollX(), getScrollY(), 0, deltaY - getScrollY(), DEFAULT_DURATION);
        invalidate();
        endDrag();
    }

    private void verticalFling(int velocityY) {
        flingVertically(-velocityY);
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

    private int getHorizontalScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            scrollRange = Math.max(0, mMaximumWidth - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    public void flingVertically(int velocityY) {
        if (getChildCount() > 0) {
            int height = getHeight() - getPaddingBottom() - getPaddingTop();
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, Math.max(0, mMaximumHeight - height), 0, height / 2);
            postInvalidateOnAnimation();
        }
    }

    public int getfinalOffsetY(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
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
        float velocity = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        float coeffY = velocity == 0 ? 1.0f : velocityY / velocity;

        double totalDistance = getSplineFlingDistance(velocity);
        int mFinalY = startY + (int) Math.round(totalDistance * coeffY);
        // Pin to mMinY <= mFinalY <= mMaxY
        mFinalY = Math.min(mFinalY, maxY);
        mFinalY = Math.max(mFinalY, minY);
        return mFinalY;
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

    public boolean isSnap() {
        return snap;
    }

    public void setSnap(boolean snap) {
        this.snap = snap;
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
        invalidate();
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
