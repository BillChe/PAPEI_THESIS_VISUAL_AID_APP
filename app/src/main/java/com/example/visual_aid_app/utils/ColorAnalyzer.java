package com.example.visual_aid_app.utils;

import android.content.Context;
import android.media.Image;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

public class ColorAnalyzer implements ImageAnalysis.Analyzer {

    private long lastAnalyzedTimestamp = 0L;
    private String hexColor = "";
    private Context context;
    private TextView textView;


    public ColorAnalyzer(Context context, TextView textView) {
        this.context = context;
        this.textView = textView;
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        buffer.rewind(); // Rewind the buffer to zero
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data); // Copy the buffer into a byte array
        return data; // Return the byte array
    }

    private Triple<Double, Double, Double> getRGBfromYUV(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();

        int height = image.getHeight();
        int width = image.getWidth();

        // Y
        ByteBuffer yArr = planes[0].getBuffer();
        byte[] yArrByteArray = toByteArray(yArr);
        int yPixelStride = planes[0].getPixelStride();
        int yRowStride = planes[0].getRowStride();

        // U
        ByteBuffer uArr = planes[1].getBuffer();
        byte[] uArrByteArray = toByteArray(uArr);
        int uPixelStride = planes[1].getPixelStride();
        int uRowStride = planes[1].getRowStride();

        // V
        ByteBuffer vArr = planes[2].getBuffer();
        byte[] vArrByteArray = toByteArray(vArr);
        int vPixelStride = planes[2].getPixelStride();
        int vRowStride = planes[2].getRowStride();

        int y = yArrByteArray[(height * yRowStride + width * yPixelStride) / 2] & 255;
        int u = (uArrByteArray[(height * uRowStride + width * uPixelStride) / 4] & 255) - 128;
        int v = (vArrByteArray[(height * vRowStride + width * vPixelStride) / 4] & 255) - 128;

        double r = y + (1.370705 * v);
        double g = y - (0.698001 * v) - (0.337633 * u);
        double b = y + (1.732446 * u);

        return new Triple<>(r, g, b);
    }

    // analyze the color
    @Override
    public void analyze(ImageProxy image) {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.MILLISECONDS.toMillis(100)) {

            Triple<Double, Double, Double> colors = getRGBfromYUV(image);
            hexColor = String.format("#%02x%02x%02x", colors.getFirst().intValue(), colors.getSecond().intValue(), colors.getThird().intValue());
            Log.d("test", "hexColor: " + hexColor);
            if(hexColor!=null
                    && hexColor.length()>0) {
                /*Toast.makeText(context, hexColor, Toast.LENGTH_SHORT)
                        .show();*/
                lastAnalyzedTimestamp = currentTimestamp;
                ColorUtils colorUtils = new ColorUtils();
                hexColor = colorUtils.getColorNameFromRgb(colors.getFirst().intValue(),colors.getSecond().intValue(),
                        colors.getThird().intValue());
                if(hexColor!=null
                        && hexColor.length()>0) {
                  /*  Toast.makeText(context, hexColor, Toast.LENGTH_SHORT)
                            .show();*/
                    textView.setText(hexColor);
                }

            }
        }
    }

    private static class Triple<A, B, C> {
        private final A first;
        private final B second;
        private final C third;

        public Triple(A first, B second, C third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        public C getThird() {
            return third;
        }
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String hexColor) {
        this.hexColor = hexColor;
    }
}
