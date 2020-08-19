package com.epimenides.myface.env;

import org.tensorflow.lite.support.image.TensorImage;

/**
 * @author kyle-luo
 * @create 2020-06-11-13:59
 */
public abstract class Recognition {

    protected abstract float[] recognize();

    protected abstract TensorImage loadImage();
}
