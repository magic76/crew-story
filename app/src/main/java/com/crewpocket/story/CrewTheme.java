package com.crewpocket.story;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class CrewTheme {
    private CrewTheme() {}

    public static final int BG_PRIMARY     = 0xFF0B0F19; // Deep dark slate
    public static final int BG_SURFACE     = 0xFF111827; // Card slate
    public static final int BG_ELEVATED    = 0xFF1F2937; // Elevated surface
    public static final int BG_CARD        = 0xF2111827;

    public static final int AMBER_500      = 0xFFF59E0B; // Story Gold
    public static final int AMBER_400      = 0xFFFBBF24;
    public static final int ORANGE_500     = 0xFFF97316;
    public static final int INDIGO_500     = 0xFF6366F1;
    public static final int INDIGO_400     = 0xFF818CF8;
    public static final int PURPLE_500     = 0xFFA855F7;
    public static final int PURPLE_400     = 0xFFC084FC;
    public static final int EMERALD_500    = 0xFF10B981;
    public static final int EMERALD_400    = 0xFF34D399;
    public static final int SKY_400        = 0xFF38BDF8;
    public static final int ROSE_500       = 0xFFF43F5E;

    public static final int TEXT_PRIMARY   = 0xFFF9FAFB;
    public static final int TEXT_SECONDARY = 0xFF9CA3AF;
    public static final int TEXT_MUTED     = 0xFF6B7280;

    public static final int BORDER_DEFAULT = 0xFF374151;
    public static final int BORDER_GOLD    = 0x4DF59E0B;
    public static final int BORDER_PURPLE  = 0x4DA855F7;

    public static int dp(Context ctx, float value) {
        if (ctx == null) return (int) value;
        return (int) (value * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable createCard(Context ctx, int bgColor, int borderColor, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        if (borderColor != Color.TRANSPARENT) {
            gd.setStroke(dp(ctx, 1f), borderColor);
        }
        return gd;
    }
}
