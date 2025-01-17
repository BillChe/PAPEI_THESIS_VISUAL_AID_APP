/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.visual_aid_app.camera_utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.Image.Plane;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Utils functions for bitmap conversions. */
public class BitmapUtils {
  private static final String TAG = "BitmapUtils";

  /** Converts NV21 format byte buffer to bitmap. */
  @Nullable
  public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {
    data.rewind();
    byte[] imageInBuffer = new byte[data.limit()];
    data.get(imageInBuffer, 0, imageInBuffer.length);
    try {
      YuvImage image =
          new YuvImage(
              imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);

      Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

      stream.close();
      return rotateBitmap(bmp, metadata.getRotation(), false, false);
    } catch (Exception e) {
      Log.e("VisionProcessorBase", "Error: " + e.getMessage());
    }
    return null;
  }

  /** Converts a YUV_420_888 image from CameraX API to a bitmap. */
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @Nullable
  @ExperimentalGetImage
  public static Bitmap getBitmap(ImageProxy image) {
    FrameMetadata frameMetadata =
        new FrameMetadata.Builder()
            .setWidth(image.getWidth())
            .setHeight(image.getHeight())
            .setRotation(image.getImageInfo().getRotationDegrees())
            .build();

    ByteBuffer nv21Buffer =
        yuv420ThreePlanesToNV21(image.getImage().getPlanes(), image.getWidth(), image.getHeight());
    return getBitmap(nv21Buffer, frameMetadata);
  }

  /** Rotates a bitmap if it is converted from a bytebuffer. */
  private static Bitmap rotateBitmap(
      Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
    Matrix matrix = new Matrix();

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees);

    // Mirror the image along the X or Y axis.
    matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
    Bitmap rotatedBitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != bitmap) {
      //bitmap.recycle();
    }
    return rotatedBitmap;
  }

  @Nullable
  public static Bitmap getBitmapFromContentUri(ContentResolver contentResolver, Uri imageUri)
      throws IOException {
    Bitmap decodedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
    if (decodedBitmap == null) {
      return null;
    }
    int orientation = getExifOrientationTag(contentResolver, imageUri);

    int rotationDegrees = 0;
    boolean flipX = false;
    boolean flipY = false;
    // See e.g. https://magnushoff.com/articles/jpeg-orientation/ for a detailed explanation on each
    // orientation.
    switch (orientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_ROTATE_90:
        rotationDegrees = 90;
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        rotationDegrees = 90;
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        rotationDegrees = 180;
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        flipY = true;
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        rotationDegrees = -90;
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        rotationDegrees = -90;
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_UNDEFINED:
      case ExifInterface.ORIENTATION_NORMAL:
      default:
        // No transformations necessary in this case.
    }

    return rotateBitmap(decodedBitmap, rotationDegrees, flipX, flipY);
  }
  public static Bitmap selfieBitmapRotate(Bitmap decodedBitmap,
                                          int orientation) {
    if (decodedBitmap == null) {
      return null;
    }
    int rotationDegrees = 0;
    boolean flipX = false;
    boolean flipY = false;
    // See e.g. https://magnushoff.com/articles/jpeg-orientation/ for a detailed explanation on each
    // orientation.
    switch (orientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_ROTATE_90:
      case ExifInterface.ORIENTATION_ROTATE_270:
        rotationDegrees = 90;
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        rotationDegrees = 90;
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        rotationDegrees = 180;
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        flipY = true;
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        rotationDegrees = -90;
        flipX = true;
        break;
      case ExifInterface.ORIENTATION_UNDEFINED:
      case ExifInterface.ORIENTATION_NORMAL:
      default:
        // No transformations necessary in this case.
    }

    return rotateBitmap(decodedBitmap, rotationDegrees, flipX, flipY);
  }

  public static int getSquareCropDimensionForBitmap(Bitmap bitmap)
  {
    //use the smallest dimension of the image to crop to
    return Math.min(bitmap.getWidth(), bitmap.getHeight());
  }

  public static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
    // GET CURRENT SIZE
    int width = bm.getWidth();
    int height = bm.getHeight();
    // GET SCALE SIZE
    float scaleWidth = ((float) newWidth) / width;
    float scaleHeight = ((float) newHeight) / height;
    // CREATE A MATRIX FOR THE MANIPULATION
    Matrix matrix = new Matrix();
    // RESIZE THE BIT MAP
    matrix.postScale(scaleWidth, scaleHeight);
    // "RECREATE" THE NEW BITMAP
    Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    return resizedBitmap;
  }

  private static int getExifOrientationTag(ContentResolver resolver, Uri imageUri) {
    // We only support parsing EXIF orientation tag from local file on the device.
    // See also:
    // https://android-developers.googleblog.com/2016/12/introducing-the-exifinterface-support-library.html
    if (!ContentResolver.SCHEME_CONTENT.equals(imageUri.getScheme())
        && !ContentResolver.SCHEME_FILE.equals(imageUri.getScheme())) {
      return 0;
    }

    ExifInterface exif;
    try (InputStream inputStream = resolver.openInputStream(imageUri)) {
      if (inputStream == null) {
        return 0;
      }

      exif = new ExifInterface(inputStream);
    } catch (IOException e) {
      Log.e(TAG, "failed to open file to read rotation meta data: " + imageUri, e);
      return 0;
    }

    return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
  }

  /**
   * Converts YUV_420_888 to NV21 bytebuffer.
   *
   * <p>The NV21 format consists of a single byte array containing the Y, U and V values. For an
   * image of size S, the first S positions of the array contain all the Y values. The remaining
   * positions contain interleaved V and U values. U and V are subsampled by a factor of 2 in both
   * dimensions, so there are S/4 U values and S/4 V values. In summary, the NV21 array will contain
   * S Y values followed by S/4 VU values: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
   *
   * <p>YUV_420_888 is a generic format that can describe any YUV image where U and V are subsampled
   * by a factor of 2 in both dimensions. {@link Image#getPlanes} returns an array with the Y, U and
   * V planes. The Y plane is guaranteed not to be interleaved, so we can just copy its values into
   * the first part of the NV21 array. The U and V planes may already have the representation in the
   * NV21 format. This happens if the planes share the same buffer, the V buffer is one position
   * before the U buffer and the planes have a pixelStride of 2. If this is case, we can just copy
   * them to the NV21 array.
   */
  private static ByteBuffer yuv420ThreePlanesToNV21(
      Plane[] yuv420888planes, int width, int height) {
    int imageSize = width * height;
    byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

    if (areUVPlanesNV21(yuv420888planes, width, height)) {
      // Copy the Y values.
      yuv420888planes[0].getBuffer().get(out, 0, imageSize);

      ByteBuffer uBuffer = yuv420888planes[1].getBuffer();
      ByteBuffer vBuffer = yuv420888planes[2].getBuffer();
      // Get the first V value from the V buffer, since the U buffer does not contain it.
      vBuffer.get(out, imageSize, 1);
      // Copy the first U value and the remaining VU values from the U buffer.
      uBuffer.get(out, imageSize + 1, 2 * imageSize / 4 - 1);
    } else {
      // Fallback to copying the UV values one by one, which is slower but also works.
      // Unpack Y.
      unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
      // Unpack U.
      unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
      // Unpack V.
      unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
    }

    return ByteBuffer.wrap(out);
  }

  /** Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format. */
  private static boolean areUVPlanesNV21(Plane[] planes, int width, int height) {
    int imageSize = width * height;

    ByteBuffer uBuffer = planes[1].getBuffer();
    ByteBuffer vBuffer = planes[2].getBuffer();

    // Backup buffer properties.
    int vBufferPosition = vBuffer.position();
    int uBufferLimit = uBuffer.limit();

    // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
    vBuffer.position(vBufferPosition + 1);
    // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
    uBuffer.limit(uBufferLimit - 1);

    // Check that the buffers are equal and have the expected number of elements.
    boolean areNV21 =
        (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

    // Restore buffers to their initial state.
    vBuffer.position(vBufferPosition);
    uBuffer.limit(uBufferLimit);

    return areNV21;
  }

  /**
   * Unpack an image plane into a byte array.
   *
   * <p>The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
   * spaced by 'pixelStride'. Note that there is no row padding on the output.
   */
  private static void unpackPlane(
      Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
    ByteBuffer buffer = plane.getBuffer();
    buffer.rewind();

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.
    int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
    if (numRow == 0) {
      return;
    }
    int scaleFactor = height / numRow;
    int numCol = width / scaleFactor;

    // Extract the data in the output buffer.
    int outputPos = offset;
    int rowStart = 0;
    for (int row = 0; row < numRow; row++) {
      int inputPos = rowStart;
      for (int col = 0; col < numCol; col++) {
        out[outputPos] = buffer.get(inputPos);
        outputPos += pixelStride;
        inputPos += plane.getPixelStride();
      }
      rowStart += plane.getRowStride();
    }
  }

  /**
   * Decode strem.
   *
   * @param fil
   *            the fil
   * @param selectedImage
   *            the selected image
   * @param mContext
   *            the m context
   * @return the bitmap
   */
  public static Bitmap decodeStrem(File fil, Uri selectedImage,
                                   Context mContext) {

    Bitmap bitmap = null;
    try {

      bitmap = BitmapFactory.decodeStream(mContext.getContentResolver()
              .openInputStream(selectedImage));

      final int THUMBNAIL_SIZE = getThumbSize(bitmap);

      bitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE,
              THUMBNAIL_SIZE, false);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
      bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(baos
              .toByteArray()));

      return bitmap = rotateImage(bitmap, fil.getAbsolutePath());

    } catch (Exception e) {
      e.printStackTrace();
    }
    return bitmap;
  }


  /**
   * Decode file.
   *
   * @param f
   *            the f
   * @param sampling
   *            the sampling
   *            the check
   * @return the bitmap
   */
  public static Bitmap decodeFile(File f, int sampling) {
    try {
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inJustDecodeBounds = true;
      BitmapFactory.decodeStream(
              new FileInputStream(f.getAbsolutePath()), null, o2);

      o2.inSampleSize = sampling;
      o2.inTempStorage = new byte[48 * 1024];

      o2.inJustDecodeBounds = false;
      Bitmap bitmap = BitmapFactory.decodeStream(
              new FileInputStream(f.getAbsolutePath()), null, o2);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
      bitmap = rotateImage(bitmap, f.getAbsolutePath());
      return bitmap;

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Rotate image.
   *
   * @param bmp
   *            the bmp
   * @param imageUrl
   *            the image url
   * @return the bitmap
   */
  public static Bitmap rotateImage(Bitmap bmp, String imageUrl) {
    if (bmp != null) {
      android.media.ExifInterface ei;
      int orientation = 0;
      try {
        ei = new android.media.ExifInterface(imageUrl);
        orientation = ei.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL);

      } catch (IOException e) {
        e.printStackTrace();
      }
      int bmpWidth = bmp.getWidth();
      int bmpHeight = bmp.getHeight();
      Matrix matrix = new Matrix();
      switch (orientation) {
        case android.media.ExifInterface.ORIENTATION_UNDEFINED:
          matrix.postRotate(90);
          break;
        case android.media.ExifInterface.ORIENTATION_ROTATE_90:
          matrix.postRotate(90);
          break;
        case android.media.ExifInterface.ORIENTATION_ROTATE_180:
          matrix.postRotate(180);
          break;
        case android.media.ExifInterface.ORIENTATION_ROTATE_270:
          matrix.postRotate(270);
          break;
        default:
          break;
      }
      Log.i("resizedBitmap w [%d]" , String.valueOf(bmpWidth));
      Log.i("resizedBitmap h [%d]" , String.valueOf(bmpHeight));

      Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmpWidth,
              bmpHeight, matrix, true);
      return resizedBitmap;
    } else {
      return bmp;
    }
  }


  /**
   * Gets the thumb size.
   *
   * @param bitmap
   *            the bitmap
   * @return the thumb size
   */
  public static int getThumbSize(Bitmap bitmap) {

    int THUMBNAIL_SIZE = 250;
    if (bitmap.getWidth() < 300) {
      THUMBNAIL_SIZE = 250;
    } else if (bitmap.getWidth() < 600) {
      THUMBNAIL_SIZE = 500;
    } else if (bitmap.getWidth() < 1000) {
      THUMBNAIL_SIZE = 750;
    } else if (bitmap.getWidth() < 2000) {
      THUMBNAIL_SIZE = 1500;
    } else if (bitmap.getWidth() < 4000) {
      THUMBNAIL_SIZE = 2000;
    } else if (bitmap.getWidth() > 4000) {
      THUMBNAIL_SIZE = 2000;
    }
    return THUMBNAIL_SIZE;
  }

}
