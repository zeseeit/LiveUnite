package com.liveunite.chat.converters;

import android.content.Context;
import android.graphics.Typeface;


public class FontManager {
    public static final String FONT_MATERIAL = "MaterialFont.ttf";
    private static Context context;
    private static FontManager mInstance;

    public FontManager(Context context) {
        FontManager.context = context;
    }

    public static FontManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FontManager(context);
        }
        return mInstance;
    }

    public Typeface getTypeFace(){
        Typeface typeface = Typeface.createFromAsset(context.getAssets(),FONT_MATERIAL);
        return typeface;
    }

}
