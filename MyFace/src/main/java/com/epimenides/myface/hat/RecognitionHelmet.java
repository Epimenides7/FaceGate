package com.epimenides.myface.hat;

import android.graphics.Bitmap;


import com.epimenides.myface.env.Recognition;

import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

/**
 * @author kyle-luo
 * @create 2020-06-11-8:21
 */
public class RecognitionHelmet extends Recognition {

    private final Helmet helmet;
    private final Bitmap bitmap;

    private final TensorProcessor probabilityProcessor;

    public RecognitionHelmet(Helmet helmet, Bitmap bitmap) {
        this.helmet = helmet;
        this.bitmap = bitmap;
        this.probabilityProcessor = this.helmet.getProbabilityProcessor();
    }

    public float recognizeHelmet() {

        TensorImage inputImageBuffer = this.loadImage();

        this.helmet.getTflite().run(inputImageBuffer.getBuffer(),
                this.helmet.getOutputProbabilityBuffer().getBuffer().rewind());

        return probabilityProcessor.process(this.helmet.getOutputProbabilityBuffer()).getFloatValue(0);
    }

    @Override
    public float[] recognize() {
        TensorImage inputImageBuffer = this.loadImage();

        this.helmet.getTflite().run(inputImageBuffer.getBuffer(),
                this.helmet.getOutputProbabilityBuffer().getBuffer().rewind());

        return probabilityProcessor.process(this.helmet.getOutputProbabilityBuffer()).getFloatArray();
    }

    /**
     * 将输入的bitmap缓存到inputImageBuffer里
     * @return 缓存了输入待检测图片的inputImageBuffer
     */
    @Override
    public TensorImage loadImage() {

        this.helmet.getInputImageBuffer().load(this.bitmap);

        int cropSize = Math.min(this.bitmap.getWidth(), this.bitmap.getHeight());

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder().
                        add(new ResizeWithCropOrPadOp(cropSize, cropSize)).
                        add(new ResizeOp(this.helmet.getImageSizeX(),
                                this.helmet.getImageSizeY(), ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)).
                        add(helmet.getPreprocessNormalizeOp()).build();

        return imageProcessor.process(this.helmet.getInputImageBuffer());
    }
}
