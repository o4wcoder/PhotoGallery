package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;

public interface Listener<Token> {

	void onThumbnailDownloaded(Token token, Bitmap thumbnail);
}
