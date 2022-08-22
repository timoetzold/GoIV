package com.kamron.pogoiv.utils.fractions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public abstract class Fraction {

    FractionManager fractionManager;

    public enum Anchor {
        TOP(Gravity.TOP),
        BOTTOM(Gravity.BOTTOM);

        private int gravity;

        Anchor(int gravity) {
            this.gravity = gravity;
        }

        public int getGravity() {
            return gravity;
        }
    }

    public abstract void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent);

    public abstract void onDestroy();

    /**
     * Vertical positioning of this Fraction container. If TOP to anchors to the top of the screen,
     * if BOTTOM anchors to the bottom of the screen.
     * @return The vertical side to anchor to
     */
    public abstract Anchor getAnchor();

    /**
     * Get the vertical distance of this Fraction container from its anchor.
     * See {@link #getAnchor()}.
     * @return number of pixels to offset from the anchor
     */
    public abstract int getVerticalOffset(@NonNull DisplayMetrics displayMetrics);

    public void setFractionManager(@Nullable FractionManager fractionManager) {
        this.fractionManager = fractionManager;
    }

}
