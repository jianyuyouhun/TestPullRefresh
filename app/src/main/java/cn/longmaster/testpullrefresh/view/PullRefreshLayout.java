package cn.longmaster.testpullrefresh.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.HashMap;

import cn.longmaster.testpullrefresh.R;

import static cn.longmaster.testpullrefresh.view.PullRefreshLayout.PullType.TYPE_CONTENT;

/**
 * 下拉刷新控件
 * Created by wangyu on 2017/11/27.
 */

public class PullRefreshLayout extends ViewGroup {

    private int//触发刷新偏移量
            triggerOffsetLeft = 0,
            triggerOffsetRight = 0,
            triggerOffsetTop = 0,
            triggerOffsetBottom = 0;

    private int//最大偏移量
            maxOffsetLeft = 0,
            maxOffsetRight = 0,
            maxOffsetTop = 0,
            maxOffsetBottom = 0;

    private boolean
            fixedContentLeft = false,
            fixedContentRight = false,
            fixedContentTop = false,
            fixedContentBottom = false;

    private long rollBackDuration = 0;//回滚动画时长
    private float stickyFactor = 0;//滑动阻尼系数，越大越难滑动，范围0~1f

    private float downX = 0;
    private float downY = 0;
    private float offsetX = 0;
    private float offsetY = 0;
    private float lastPullFraction = 0;

    private PullType currentType = PullType.TYPE_NONE;//当前刷新类型
    private PullState currentState = PullState.STATE_IDLE;//当前刷新状态

    private ValueAnimator horizontalAnimator = null;
    private ValueAnimator verticalAnimator = null;

    private OnEdgeListener onEdgeListener;
    private OnPullListener onPullListener;
    private OnTriggerListener onTriggerListener;

    private HashMap<View, ChildViewAttr> childViews = new HashMap<>();

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PullRefreshLayout, defStyleAttr, 0);
        triggerOffsetLeft = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_trigger_offset_left,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        triggerOffsetTop = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_trigger_offset_top,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        triggerOffsetRight = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_trigger_offset_right,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        triggerOffsetBottom = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_trigger_offset_bottom,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        maxOffsetLeft = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_max_offset_left,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        maxOffsetRight = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_max_offset_right,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        maxOffsetTop = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_max_offset_top,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        maxOffsetBottom = array.getDimensionPixelOffset(R.styleable.PullRefreshLayout_max_offset_bottom,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, getResources().getDisplayMetrics()));
        fixedContentLeft = array.getBoolean(R.styleable.PullRefreshLayout_fixed_content_left, false);
        fixedContentRight = array.getBoolean(R.styleable.PullRefreshLayout_fixed_content_right, false);
        fixedContentTop = array.getBoolean(R.styleable.PullRefreshLayout_fixed_content_top, false);
        fixedContentBottom = array.getBoolean(R.styleable.PullRefreshLayout_fixed_content_bottom, false);
        rollBackDuration = array.getInt(R.styleable.PullRefreshLayout_roll_back_duration, 300);
        stickyFactor = array.getFloat(R.styleable.PullRefreshLayout_sticky_factor, 0.66f);
        stickyFactor = stickyFactor < 0f ? 0f : (stickyFactor > 1f ? 1f : stickyFactor);
        array.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            LayoutParams lp = (LayoutParams) childView.getLayoutParams();
            if (getByType(lp.type) != null) {
                throw new RuntimeException("Each child type can only be defined once!");
            } else {
                childViews.put(childView, new ChildViewAttr());
            }
        }
        final View contentView = getByType(TYPE_CONTENT);
        if (contentView == null)
            throw new RuntimeException("Child type \"content\" must be defined!");
        setOnEdgeListener(new OnEdgeListener() {
            @Override
            public PullType onEdge() {
                View byLeft = getByType(PullType.TYPE_EDGE_LEFT);
                if (byLeft != null) {
                    if (!contentView.canScrollHorizontally(-1)) {
                        return PullType.TYPE_EDGE_LEFT;
                    }
                }
                View byRight = getByType(PullType.TYPE_EDGE_RIGHT);
                if (byRight != null) {
                    if (!contentView.canScrollHorizontally(1)) {
                        return PullType.TYPE_EDGE_RIGHT;
                    }
                }
                View byTop = getByType(PullType.TYPE_EDGE_TOP);
                if (byTop != null) {
                    if (!contentView.canScrollVertically(-1)) {
                        return PullType.TYPE_EDGE_TOP;
                    }
                }
                View byBottom = getByType(PullType.TYPE_EDGE_BOTTOM);
                if (byBottom != null) {
                    if (!contentView.canScrollVertically(1)) {
                        return PullType.TYPE_EDGE_BOTTOM;
                    }
                }
                return PullType.TYPE_NONE;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (View view : childViews.keySet()) {
            ChildViewAttr childViewAttr = childViews.get(view);
            measureChildWithMargins(view, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            switch (lp.type) {
                case TYPE_NONE:
                case TYPE_CONTENT:
                    break;
                case TYPE_EDGE_LEFT:
                case TYPE_EDGE_RIGHT:
                    childViewAttr.size = view.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                    triggerOffsetLeft = triggerOffsetLeft < 0 ? childViewAttr.size / 2 : triggerOffsetLeft;
                    triggerOffsetRight = triggerOffsetRight < 0 ? childViewAttr.size / 2 : triggerOffsetRight;
                    maxOffsetLeft = maxOffsetLeft < 0 ? childViewAttr.size : maxOffsetLeft;
                    maxOffsetRight = maxOffsetRight < 0 ? childViewAttr.size : maxOffsetRight;
                    break;
                case TYPE_EDGE_TOP:
                case TYPE_EDGE_BOTTOM:
                    childViewAttr.size = view.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                    triggerOffsetTop = triggerOffsetTop < 0 ? childViewAttr.size / 2 : triggerOffsetTop;
                    triggerOffsetBottom = triggerOffsetBottom < 0 ? childViewAttr.size / 2 : triggerOffsetRight;
                    maxOffsetTop = maxOffsetTop < 0 ? childViewAttr.size : maxOffsetTop;
                    maxOffsetBottom = maxOffsetBottom < 0 ? childViewAttr.size : maxOffsetBottom;
                    break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View contentView = getByType(TYPE_CONTENT);
        if (contentView == null)
            throw new RuntimeException("EasyPullLayout must have and only have one layout_type \"content\"!");
        int contentWidth = contentView.getMeasuredWidth();
        int contentHeight = contentView.getMeasuredHeight();

        for (View childView : childViews.keySet()) {
            ChildViewAttr childViewAttr = childViews.get(childView);
            LayoutParams lp = (LayoutParams) childView.getLayoutParams();
            int left = getPaddingLeft() + lp.leftMargin;
            int top = getPaddingTop() + lp.topMargin;
            int right = left + childView.getMeasuredWidth();
            int bottom = top + childView.getMeasuredHeight();
            switch (lp.type) {
                case TYPE_EDGE_LEFT:
                    left -= childViewAttr.size;
                    right -= childViewAttr.size;
                    break;
                case TYPE_EDGE_TOP:
                    top -= childViewAttr.size;
                    bottom -= childViewAttr.size;
                    break;
                case TYPE_EDGE_RIGHT:
                    left += contentWidth;
                    right += contentWidth;
                    break;
                case TYPE_EDGE_BOTTOM:
                    top += contentHeight;
                    bottom += contentHeight;
                    break;
                default:
                    break;
            }
            childViewAttr.set(left, top, right, bottom, 0);
            childView.layout(left, top, right, bottom);
        }
        if (fixedContentLeft) {
            View byLeft = getByType(PullType.TYPE_EDGE_LEFT);
            if (byLeft != null) byLeft.bringToFront();
        }
        if (fixedContentTop) {
            View byTop = getByType(PullType.TYPE_EDGE_TOP);
            if (byTop != null) byTop.bringToFront();
        }
        if (fixedContentRight) {
            View byRight = getByType(PullType.TYPE_EDGE_RIGHT);
            if (byRight != null) byRight.bringToFront();
        }
        if (fixedContentBottom) {
            View byBottom = getByType(PullType.TYPE_EDGE_BOTTOM);
            if (byBottom != null) byBottom.bringToFront();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (currentState != PullState.STATE_IDLE) {
            return false;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                PullType type = onEdgeListener.onEdge();
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                currentType = type;
                switch (type) {
                    case TYPE_EDGE_LEFT:
                        return ev.getX() > downX && Math.abs(dx) > Math.abs(dy);
                    case TYPE_EDGE_RIGHT:
                        return ev.getX() < downX && Math.abs(dx) > Math.abs(dy);
                    case TYPE_EDGE_TOP:
                        return ev.getY() > downY && Math.abs(dy) > Math.abs(dx);
                    case TYPE_EDGE_BOTTOM:
                        return ev.getY() < downY && Math.abs(dy) > Math.abs(dx);
                    default:
                        return false;
                }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentState != PullState.STATE_IDLE) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                offsetX = (x - downX) * (1 - stickyFactor * 0.75f);
                offsetY = (y - downY) * (1 - stickyFactor * 0.75f);

                float pullFraction = 0f;
                switch (currentType) {
                    case TYPE_EDGE_LEFT:
                        offsetX = offsetX < 0 ? 0f : (offsetX > maxOffsetLeft ? maxOffsetLeft : offsetX);
                        pullFraction = offsetX == 0f ? 0f : (triggerOffsetLeft > offsetX ? offsetX / triggerOffsetLeft : 1f);
                        break;
                    case TYPE_EDGE_RIGHT:
                        offsetX = offsetX > 0 ? 0f : (offsetX < -maxOffsetRight ? -maxOffsetRight : offsetX);
                        pullFraction = offsetX == 0f ? 0f : (-triggerOffsetRight < offsetX ? offsetX / -triggerOffsetRight : 1f);
                        break;
                    case TYPE_EDGE_TOP:
                        offsetY = offsetY < 0 ? 0f : (offsetY > maxOffsetTop ? maxOffsetTop : offsetY);
                        pullFraction = offsetY == 0f ? 0f : (triggerOffsetTop > offsetY ? offsetY / triggerOffsetTop : 1f);
                        break;
                    case TYPE_EDGE_BOTTOM:
                        offsetY = offsetY > 0 ? 0f : (offsetY < -maxOffsetBottom ? -maxOffsetBottom : offsetY);
                        pullFraction = offsetY == 0f ? 0f : (-triggerOffsetBottom < offsetY ? offsetY / -triggerOffsetBottom : 1f);
                        break;
                }
                boolean changed = !(lastPullFraction < 1f && pullFraction < 1f || lastPullFraction == 1f && pullFraction == 1f);
                if (onPullListener != null)
                    onPullListener.onPull(currentType, pullFraction, changed);
                lastPullFraction = pullFraction;

                switch (currentType) {
                    case TYPE_EDGE_LEFT:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentLeft) {
                                view.setX(childViewAttr.left + offsetX);
                            }
                        }
                        break;
                    case TYPE_EDGE_RIGHT:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentRight) {
                                view.setX(childViewAttr.left + offsetX);
                            }
                        }
                        break;
                    case TYPE_EDGE_TOP:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentTop) {
                                view.setY(childViewAttr.top + offsetY);
                            }
                        }
                        break;
                    case TYPE_EDGE_BOTTOM:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentBottom) {
                                view.setY(childViewAttr.top + offsetY);
                            }
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                currentState = PullState.STATE_ROLLING;
                switch (currentType) {
                    case TYPE_EDGE_TOP:
                    case TYPE_EDGE_BOTTOM:
                        rollBackVertical();
                        break;
                    case TYPE_EDGE_LEFT:
                    case TYPE_EDGE_RIGHT:
                        rollBackHorizontal();
                        break;
                }
        }
        return true;
    }

    /**
     * 横向回滚，注意一次刷新流程最多会有两次触发，
     * 第一次触发为偏移量大于触发边界时先触发回弹，
     * 第二次为刷新结束调用stopRefresh时偏移量回滚到0
     */
    private void rollBackHorizontal() {
        if (horizontalAnimator != null) {
            return;
        }
        final float rollBackOffset = offsetX > triggerOffsetLeft ? offsetX - triggerOffsetLeft
                : (offsetX < -triggerOffsetRight ? offsetX + triggerOffsetRight : offsetX);
        final float triggerOffset;
        if (rollBackOffset != offsetX) {//判断处于越界回弹
            switch (currentType) {
                case TYPE_EDGE_LEFT:
                    triggerOffset = triggerOffsetLeft;
                    break;
                case TYPE_EDGE_RIGHT:
                    triggerOffset = triggerOffsetRight;
                    break;
                default:
                    triggerOffset = 0;
            }
        } else {//此时为回滚到初始状态
            triggerOffset = 0;
        }
        horizontalAnimator = ValueAnimator.ofFloat(1f, 0f);
        horizontalAnimator.setDuration(rollBackDuration);
        horizontalAnimator.setInterpolator(new DecelerateInterpolator());
        horizontalAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                switch (currentType) {
                    case TYPE_EDGE_LEFT:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentLeft) {
                                view.setX(childViewAttr.left + triggerOffset + rollBackOffset * value);
                            }
                        }
                        break;
                    case TYPE_EDGE_RIGHT:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentRight) {
                                view.setX(childViewAttr.left + triggerOffset + rollBackOffset * value);
                            }
                        }
                        break;
                }
            }
        });
        horizontalAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                horizontalAnimator = null;
                if (triggerOffset != 0 && currentState == PullState.STATE_ROLLING) {//下拉状态且回滚偏移量不为0
                    currentState = PullState.STATE_TRIGGERING;//设置为回滚中
                    offsetX = triggerOffset;
                    if (onTriggerListener != null) onTriggerListener.onTrigger(currentType);
                } else {//偏移量为0时重置控件状态
                    currentState = PullState.STATE_IDLE;
                    offsetX = 0f;
                }
            }
        });
        horizontalAnimator.start();
    }

    /**
     * 纵向回滚，注意一次刷新流程最多会有两次触发，
     * 第一次触发为偏移量大于触发边界时先触发回弹，
     * 第二次为刷新结束调用stopRefresh时偏移量回滚到0
     */
    private void rollBackVertical() {
        if (verticalAnimator != null) {
            return;
        }
        final float rollBackOffset = offsetY > triggerOffsetTop ? offsetY - triggerOffsetTop
                : (offsetY < -triggerOffsetBottom ? offsetY + triggerOffsetBottom : offsetY);
        final float triggerOffset;
        if (rollBackOffset != offsetY) {//判断处于越界回弹
            switch (currentType) {
                case TYPE_EDGE_TOP:
                    triggerOffset = triggerOffsetTop;
                    break;
                case TYPE_EDGE_BOTTOM:
                    triggerOffset = triggerOffsetBottom;
                    break;
                default:
                    triggerOffset = 0;
            }
        } else {//此时为回滚到初始状态
            triggerOffset = 0;
        }
        verticalAnimator = ValueAnimator.ofFloat(1f, 0f);
        verticalAnimator.setDuration(rollBackDuration);
        verticalAnimator.setInterpolator(new DecelerateInterpolator());
        verticalAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                switch (currentType) {
                    case TYPE_EDGE_TOP:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentTop) {
                                view.setY(childViewAttr.top + triggerOffset + rollBackOffset * value);
                            }
                        }
                        break;
                    case TYPE_EDGE_BOTTOM:
                        for (View view : childViews.keySet()) {
                            ChildViewAttr childViewAttr = childViews.get(view);
                            LayoutParams lp = (LayoutParams) view.getLayoutParams();
                            if (lp.type != TYPE_CONTENT || !fixedContentBottom) {
                                view.setY(childViewAttr.top + triggerOffset + rollBackOffset * value);
                            }
                        }
                        break;
                }
            }
        });
        verticalAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                verticalAnimator = null;
                if (triggerOffset != 0 && currentState == PullState.STATE_ROLLING) {
                    currentState = PullState.STATE_TRIGGERING;
                    offsetY = triggerOffset;
                    if (onTriggerListener != null) onTriggerListener.onTrigger(currentType);
                } else {
                    currentState = PullState.STATE_IDLE;
                    offsetY = 0f;
                }
            }
        });
        verticalAnimator.start();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return null != p && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public void stopRefresh() {
        switch (currentType) {
            case TYPE_EDGE_RIGHT:
            case TYPE_EDGE_LEFT:
                rollBackHorizontal();
                break;
            case TYPE_EDGE_TOP:
            case TYPE_EDGE_BOTTOM:
                rollBackVertical();
                break;
        }
    }

    @Nullable
    private View getByType(PullType type) {
        for (View view : childViews.keySet()) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.type == type) {
                return view;
            }
        }
        return null;
    }

    /**
     * 设置获取当前刷新类型回调
     */
    private void setOnEdgeListener(OnEdgeListener onEdgeListener) {
        this.onEdgeListener = onEdgeListener;
    }

    public void setOnPullListener(OnPullListener onPullListener) {
        this.onPullListener = onPullListener;
    }

    /**
     * 设置刷新状态变化监听器
     */
    public void setOnTriggerListener(OnTriggerListener onTriggerListener) {
        this.onTriggerListener = onTriggerListener;
    }

    public interface OnPullListener {
        void onPull(PullType type, float fraction, boolean changed);
    }

    private interface OnEdgeListener {
        PullType onEdge();
    }

    public interface OnTriggerListener {
        void onTrigger(PullType type);
    }

    public enum PullType {
        TYPE_NONE(-1),
        TYPE_EDGE_LEFT(0),
        TYPE_EDGE_TOP(1),
        TYPE_EDGE_RIGHT(2),
        TYPE_EDGE_BOTTOM(3),
        TYPE_CONTENT(4);
        int value;

        PullType(int value) {
            this.value = value;
        }

        static PullType build(int value) {
            switch (value) {
                case -1:
                    return TYPE_NONE;
                case 0:
                    return TYPE_EDGE_LEFT;
                case 1:
                    return TYPE_EDGE_TOP;
                case 2:
                    return TYPE_EDGE_RIGHT;
                case 3:
                    return TYPE_EDGE_BOTTOM;
                case 4:
                    return TYPE_CONTENT;
                default:
                    return TYPE_NONE;
            }
        }
    }

    public enum PullState {
        /**
         * 闲置
         */
        STATE_IDLE,
        /**
         * 正在回滚
         */
        STATE_ROLLING,
        /**
         * 等待触发回滚
         */
        STATE_TRIGGERING
    }

    public class LayoutParams extends ViewGroup.MarginLayoutParams {

        private PullType type = PullType.TYPE_NONE;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray array = c.getTheme().obtainStyledAttributes(attrs, R.styleable.PullRefreshLayout_LayoutParams, 0, 0);
            type = PullType.build(array.getInt(R.styleable.PullRefreshLayout_LayoutParams_layout_type, -1));
            array.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public class ChildViewAttr {
        public int left = 0;
        public int top = 0;
        public int right = 0;
        public int bottom = 0;
        public int size = 0;

        public void set(int left, int top, int right, int bottom, int size) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.size = size;
        }
    }
}
