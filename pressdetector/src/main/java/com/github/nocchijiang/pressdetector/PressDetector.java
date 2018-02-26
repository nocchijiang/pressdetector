package com.github.nocchijiang.pressdetector;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link FrameLayout} that is able to detect changes of pressed state for any of its direct or indirect children.
 * <p/>
 * Usage:
 * <ul>
 *     <li>Use this layout as the root of the view hierarchy where you'd like to detect pressed state changes.</li>
 *     <li>Create an instance of {@link Callback} to handle the changes of pressed state.</li>
 *     <li>Set the callback via {@link #addCallback(Callback)}.</li>
 *     <li>Exclude any {@link View} instances via {@link #exclude(View)}. Once the detector finds a pressed view
 *     which is excluded, the "searching a pressed view" progress will be terminated immediately.</li>
 * </ul>
 * <p/>
 */

@SuppressWarnings("unused") // Public API
public class PressDetector extends FrameLayout {

    public interface Callback {

        /**
         * The callback is triggered when a child view (either direct or indirect) is about to be pressed.
         *
         * @param view the view being pressed
         */
        void onViewPressed(View view);

        /**
         * The callback is triggered when a previously pressed child view (either direct or indirect)
         * is about to be unpressed.
         *
         * @param view the view being unpressed
         */
        void onViewUnpressed(View view);
    }

    /**
     * Excludes a {@link View} instance globally.
     */
    public static void exclude(View child) {
        child.setTag(R.id.press_detector_exclude_key, EXCLUDE_CHILD_TAG);
    }

    private static final Object EXCLUDE_CHILD_TAG = new Object();

    private static final int PFLAG_PREPRESSED = 0x02000000;

    private static final Field PRIVATE_FLAGS_FIELD;

    static {
        try {
            PRIVATE_FLAGS_FIELD = View.class.getDeclaredField("mPrivateFlags");
            PRIVATE_FLAGS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Should not happen.
            throw new AssertionError(e);
        }
    }

    View childBeingPrepressed;
    View childBeingPressed;
    final boolean[] pressedFlags = new boolean[2];
    private CheckForTap checkForTap = new CheckForTap();
    private UnsetPressedState unsetPressedState = new UnsetPressedState();

    private List<Callback> callbacks = new ArrayList<Callback>();

    private static boolean shouldSkip(View child) {
        return child != null && child.getTag(R.id.press_detector_exclude_key) == EXCLUDE_CHILD_TAG;
    }

    private class CheckForTap implements Runnable {

        @Override
        public void run() {
            getViewPressedFlags(childBeingPrepressed);

            if (pressedFlags[0] || pressedFlags[1]) {
                setPressedChildAndNotify(childBeingPrepressed);
            }
            childBeingPrepressed = null;
        }
    }

    private final class UnsetPressedState implements Runnable {
        @Override
        public void run() {
            reset();
        }
    }

    public PressDetector(Context context) {
        super(context);
    }

    public PressDetector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PressDetector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final boolean result = super.dispatchTouchEvent(ev);
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
            View pressedChild = findPressedView(this);
            if (pressedChild != null) {
                if (pressedFlags[1]) {
                    setPressedChildAndNotify(pressedChild);
                } else if (pressedFlags[0]) {
                    childBeingPrepressed = pressedChild;
                    postDelayed(checkForTap, ViewConfiguration.getTapTimeout());
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE && childBeingPressed != null && !childBeingPressed.isPressed()
                   || action == MotionEvent.ACTION_CANCEL) {
            reset();
        } else if (action == MotionEvent.ACTION_UP) {
            removeCallbacks(checkForTap);

            if (childBeingPrepressed != null) {
                getViewPressedFlags(childBeingPrepressed);
                if (pressedFlags[0] || pressedFlags[1]) {
                    setPressedChildAndNotify(childBeingPrepressed);
                    postDelayed(unsetPressedState, ViewConfiguration.getPressedStateDuration());
                } else {
                    reset();
                }
            } else {
                reset();
            }
        }

        return result;
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
        removeCallbacks(unsetPressedState);
        reset();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(unsetPressedState);
        reset();
    }

    private void reset() {
        removeCallbacks(checkForTap);
        if (childBeingPressed != null) {
            for (Callback callback : callbacks) {
                callback.onViewUnpressed(childBeingPressed);
            }
        }
        childBeingPressed = null;
    }

    private void setPressedChildAndNotify(View pressedChild) {
        childBeingPressed = pressedChild;
        for (Callback callback : callbacks) {
            callback.onViewPressed(childBeingPressed);
        }
    }

    View findPressedView(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child == null || child.getVisibility() != VISIBLE) {
                continue;
            }
            getViewPressedFlags(child);
            if (pressedFlags[0] || pressedFlags[1]) {
                if (shouldSkip(child)) {
                    return null;
                }
                return child;
            }
            if (child instanceof ViewGroup) {
                final View view = findPressedView((ViewGroup) child);
                if (view != null) {
                    return view;
                }
            }
        }
        return null;
    }

    private void getViewPressedFlags(View view) {
        try {
            pressedFlags[0] = (PRIVATE_FLAGS_FIELD.getInt(view) & PFLAG_PREPRESSED) == PFLAG_PREPRESSED;
            pressedFlags[1] = view.isPressed();
        } catch (IllegalAccessException e) {
            // Should not happen.
            throw new AssertionError(e);
        }
    }

}
