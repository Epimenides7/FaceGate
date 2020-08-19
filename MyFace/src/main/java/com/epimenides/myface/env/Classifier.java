package com.epimenides.myface.env;

import android.app.Activity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * @author kyle-luo
 * @create 2020-06-09-18:10
 */
public abstract class Classifier {

    private MappedByteBuffer tfliteModel;

    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    protected Interpreter tflite;

    protected final int imageSizeX;
    protected final int imageSizeY;

    protected TensorImage inputImageBuffer;
    protected final TensorBuffer outputProbabilityBuffer;

    protected final TensorProcessor probabilityProcessor;

    private GpuDelegate gpuDelegate = null;

    protected Classifier(Activity activity, int numThreads) throws IOException {
        tfliteModel = FileUtil.loadMappedFile(activity, getModelPath());

//        gpuDelegate = new GpuDelegate();
//        Interpreter.Options options = (new Interpreter.Options()).addDelegate(gpuDelegate);
//        options.setNumThreads(numThreads);

        tfliteOptions.setNumThreads(numThreads);

//        tfliteOptions.addDelegate(gpuDelegate);
//        tflite = new Interpreter(tfliteModel, tfliteOptions);
        tflite = new Interpreter(tfliteModel, tfliteOptions);

        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];

        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();


        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape();
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        System.out.println(probabilityDataType);

        inputImageBuffer = new TensorImage(imageDataType);

        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);
    }

    private void recreateInterpreter() {
        if (tflite != null) {
            tflite.close();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    public void close() {
        if (tflite != null) {
            // TODO: Close the interpreter
            tflite.close();
            tflite = null;
        }
        tfliteModel = null;
    }

    public void useGpu() {
        if (gpuDelegate == null) {
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            recreateInterpreter();
        }
    }
    protected abstract String getModelPath();

    protected abstract TensorOperator getPostprocessNormalizeOp();

    public abstract TensorOperator getPreprocessNormalizeOp();

    public abstract int getImageSizeX();
    public abstract int getImageSizeY();

    public abstract Interpreter getTflite();

    public abstract TensorBuffer getOutputProbabilityBuffer();
    public abstract TensorProcessor getProbabilityProcessor();
    public abstract TensorImage getInputImageBuffer();

}
