package com.example.project;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class StickyTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(@NonNull View page, float position) {
        float absPos = Math.abs(position);
        
        // Rotate the page based on position
        page.setRotationY(position * -30f);
        
        // Scale down the page as it moves away from center
        float scale = absPos > 1 ? 0.8f : 1 - (absPos * 0.2f);
        page.setScaleX(scale);
        page.setScaleY(scale);
        
        // Change alpha for depth effect
        page.setAlpha(Math.max(0.5f, 1 - absPos));
        
        // Slight vertical shift or elevation if needed
        page.setTranslationZ(position < 0 ? 0 : -absPos * 10f);
    }
}