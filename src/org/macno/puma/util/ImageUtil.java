package org.macno.puma.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

public class ImageUtil {

//	private static final String TAG = "Booktab.IU";
	
	public static Bitmap createTransparentBitmapFromBitmap(Bitmap bitmap,
			int replaceThisColor) {
		if (bitmap != null) {
			int picw = bitmap.getWidth();
			int pich = bitmap.getHeight();
			int[] pix = new int[picw * pich];
			bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

			for (int y = 0; y < pich; y++) {
				// from left to right
				for (int x = 0; x < picw; x++) {
					int index = y * picw + x;
					//					int r = (pix[index] >> 16) & 0xff;
					//					int g = (pix[index] >> 8) & 0xff;
					//					int b = pix[index] & 0xff;

					if (pix[index] == replaceThisColor) {
						pix[index] = Color.TRANSPARENT;
					} else {
						break;
					}
				}

				// from right to left
				for (int x = picw - 1; x >= 0; x--) {
					int index = y * picw + x;
					//					int r = (pix[index] >> 16) & 0xff;
					//					int g = (pix[index] >> 8) & 0xff;
					//					int b = pix[index] & 0xff;

					if (pix[index] == replaceThisColor) {
						pix[index] = Color.TRANSPARENT;
					} else {
						break;
					}
				}
			}
			Bitmap bm = Bitmap.createBitmap(pix, picw, pich,
					Bitmap.Config.ARGB_4444);

			return bm;
		}
		return null;
	}

	public static Bitmap getBitmapFromFile(String imageFile) {
		return BitmapFactory.decodeFile(imageFile);
	}

	public static Bitmap convertColorIntoBlackAndWhiteImage(Bitmap orginalBitmap) {
		if(orginalBitmap == null) {
			return null;
		}
		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0);

		ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
				colorMatrix);

		Bitmap blackAndWhiteBitmap = orginalBitmap.copy(
				Bitmap.Config.ARGB_8888, true);

		Paint paint = new Paint();
		paint.setColorFilter(colorMatrixFilter);

		Canvas canvas = new Canvas(blackAndWhiteBitmap);
		canvas.drawBitmap(blackAndWhiteBitmap, 0, 0, paint);

		orginalBitmap.recycle();
		return blackAndWhiteBitmap;
	}
	
	public static Bitmap loadBitmapFromView(View v) {
		Bitmap b = Bitmap.createBitmap( v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);                
		Canvas c = new Canvas(b);
		v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
		v.draw(c);
//		v.setDrawingCacheEnabled(true);
//		Bitmap borig = v.getDrawingCache(true);
//		Bitmap b = Bitmap.createBitmap(borig);
//		v.setDrawingCacheEnabled(false);
		return b;
	}
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        final int heightRatio = Math.round((float) height / (float) reqHeight);
        final int widthRatio = Math.round((float) width / (float) reqWidth);

        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions larger than or equal to the
        // requested height and width.
        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }

    return inSampleSize;
}

}
