/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.cpu.nativecpu;


import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.primitives.Pair;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cache.TADManager;
import org.nd4j.linalg.compression.CompressedDataBuffer;
import org.nd4j.linalg.compression.CompressionDescriptor;
import org.nd4j.linalg.compression.CompressionType;
import org.nd4j.linalg.cpu.nativecpu.blas.*;
import org.nd4j.linalg.cpu.nativecpu.complex.ComplexDouble;
import org.nd4j.linalg.cpu.nativecpu.complex.ComplexFloat;
import org.nd4j.linalg.cpu.nativecpu.complex.ComplexNDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.BaseNDArrayFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import org.nd4j.nativeblas.LongPointerWrapper;
import org.nd4j.nativeblas.NativeOps;
import org.nd4j.nativeblas.NativeOpsHolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;

/**
 * {@link org.nd4j.linalg.factory.NDArrayFactory}
 * for cpus and the nd4j-native backend.
 *
 * @author Adam Gibson
 */
@Slf4j
public class CpuNDArrayFactory extends BaseNDArrayFactory {
    private NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps();

    protected ThreadLocal<PointerPointer> extrazA = new ThreadLocal<>();
    protected ThreadLocal<PointerPointer> extrazB = new ThreadLocal<>();
    protected ThreadLocal<Integer> extrazSize = new ThreadLocal<>();

    public CpuNDArrayFactory() {}

    static {
        //invoke the override
        Nd4j.getBlasWrapper();
    }


    public CpuNDArrayFactory(DataBuffer.Type dtype, Character order) {
        super(dtype, order);
    }

    public CpuNDArrayFactory(DataBuffer.Type dtype, char order) {
        super(dtype, order);
    }

    @Override
    public void createBlas() {
        blas = new CpuBlas();
        PointerPointer functions = new PointerPointer(10);
        functions.put(0, Loader.addressof("cblas_sgemv"));
        functions.put(1, Loader.addressof("cblas_dgemv"));
        functions.put(2, Loader.addressof("cblas_sgemm"));
        functions.put(3, Loader.addressof("cblas_dgemm"));
        functions.put(4, Loader.addressof("cblas_sgemm_batch"));
        functions.put(5, Loader.addressof("cblas_dgemm_batch"));
        functions.put(6, Loader.addressof("LAPACKE_sgesvd"));
        functions.put(7, Loader.addressof("LAPACKE_dgesvd"));
        functions.put(8, Loader.addressof("LAPACKE_sgesdd"));
        functions.put(9, Loader.addressof("LAPACKE_dgesdd"));
        nativeOps.initializeFunctions(functions);
    }

    @Override
    public void createLevel1() {
        level1 = new CpuLevel1();
    }

    @Override
    public void createLevel2() {
        level2 = new CpuLevel2();
    }

    @Override
    public void createLevel3() {
        level3 = new CpuLevel3();
    }

    @Override
    public void createLapack() {
        lapack = new CpuLapack();
    }

    @Override
    public INDArray create(int[] shape, DataBuffer buffer) {
        return new NDArray(shape, buffer);
    }

    /**
     * Create float
     *
     * @param real real component
     * @param imag imag component
     * @return
     */
    @Override
    public IComplexFloat createFloat(float real, float imag) {
        return new ComplexFloat(real, imag);
    }

    /**
     * Create an instance of a complex double
     *
     * @param real the real component
     * @param imag the imaginary component
     * @return a new imaginary double with the specified real and imaginary components
     */
    @Override
    public IComplexDouble createDouble(double real, double imag) {
        return new ComplexDouble(real, imag);
    }

    /**
     * Create an ndarray with the given data layout
     *
     * @param data the data to create the ndarray with
     * @return the ndarray with the given data layout
     */
    @Override
    public INDArray create(double[][] data) {
        return new NDArray(data);
    }

    @Override
    public INDArray create(double[][] data, char ordering) {
        return new NDArray(data, ordering);
    }

    /**
     * Create a complex ndarray from the passed in indarray
     *
     * @param arr the arr to wrap
     * @return the complex ndarray with the specified ndarray as the
     * real components
     */
    @Override
    public IComplexNDArray createComplex(INDArray arr) {
        return new ComplexNDArray(arr);
    }

    /**
     * Create a complex ndarray from the passed in indarray
     *
     * @param data  the data to wrap
     * @param shape
     * @return the complex ndarray with the specified ndarray as the
     * real components
     */
    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape) {
        return new ComplexNDArray(data, shape);
    }

    /**
     * Create a complex ndarray from the passed in indarray
     *
     * @param arrs  the arr to wrap
     * @param shape
     * @return the complex ndarray with the specified ndarray as the
     * real components
     */
    @Override
    public IComplexNDArray createComplex(List<IComplexNDArray> arrs, int[] shape) {
        return new ComplexNDArray(arrs, shape);
    }

    @Override
    public INDArray create(DataBuffer data) {
        return new NDArray(data);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data) {
        return new ComplexNDArray(data);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int rows, int columns, int[] stride, long offset) {
        return new ComplexNDArray(data, new int[] {rows, columns}, stride, offset);
    }

    @Override
    public INDArray create(DataBuffer data, int rows, int columns, int[] stride, long offset) {
        return new NDArray(data, new int[] {rows, columns}, stride, offset);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape, int[] stride, long offset) {
        return new ComplexNDArray(data, shape, stride, offset);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, long offset) {
        return createComplex(data, shape, stride, offset, order());
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new ComplexNDArray(data, shape, stride, offset, ordering);

    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, char ordering) {
        return new ComplexNDArray(data, shape, stride, 0, ordering);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, long offset, char ordering) {
        return createComplex(data, shape, Nd4j.getComplexStrides(shape), offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, char ordering) {
        return createComplex(data, shape, Nd4j.getComplexStrides(shape), 0, ordering);
    }

    /**
     * Creates a complex ndarray with the specified shape
     *
     * @param data   the data to use with the ndarray
     * @param shape  the shape of the ndarray
     * @param stride the stride for the ndarray
     * @param offset the offset of the ndarray
     * @return the instance
     */
    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, int[] stride, long offset) {
        return new ComplexNDArray(data, shape, stride, offset);
    }

    @Override
    public INDArray create(int[] shape, char ordering) {
        return new NDArray(shape, Nd4j.getStrides(shape, ordering), 0, ordering);
    }

    @Override
    public INDArray createUninitialized(int[] shape, char ordering) {
        return new NDArray(shape, Nd4j.getStrides(shape, ordering), 0, ordering, false);
    }

    @Override
    public INDArray createUninitializedDetached(int[] shape, char ordering) {
        MemoryWorkspace workspace = Nd4j.getMemoryManager().getCurrentWorkspace();
        Nd4j.getMemoryManager().setCurrentWorkspace(null);
        INDArray ret = new NDArray(shape, Nd4j.getStrides(shape, ordering), 0, ordering, false);
        Nd4j.getMemoryManager().setCurrentWorkspace(workspace);
        return ret;
    }

    @Override
    public INDArray create(DataBuffer data, int[] newShape, int[] newStride, long offset, char ordering) {
        return new NDArray(data, newShape, newStride, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] newDims, int[] newStrides, long offset, char ordering) {
        return new ComplexNDArray(data, newDims, newStrides, offset, ordering);

    }


    @Override
    public IComplexNDArray createComplex(float[] data, Character order) {
        return new ComplexNDArray(data, order);
    }

    @Override
    public INDArray create(float[] data, int[] shape, long offset, Character order) {
        return new NDArray(data, shape, offset, order);
    }

    @Override
    public INDArray create(float[] data, int rows, int columns, int[] stride, long offset, char ordering) {
        return new NDArray(data, new int[] {rows, columns}, stride, offset, ordering);
    }

    @Override
    public INDArray create(double[] data, int[] shape, char ordering) {
        return new NDArray(Nd4j.createBuffer(data), shape, ordering);
    }

    @Override
    public INDArray create(List<INDArray> list, int[] shape, char ordering) {
        return new NDArray(list, shape, ordering);
    }

    @Override
    public INDArray create(double[] data, int[] shape, long offset) {
        return new NDArray(Nd4j.createBuffer(data), shape, offset);
    }

    @Override
    public INDArray create(double[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new NDArray(Nd4j.createBuffer(data), shape, stride, offset, ordering);
    }


    /**
     * Creates an ndarray with the specified shape
     *
     * @param data
     * @param shape  the shape of the ndarray
     * @param stride the stride for the ndarray
     * @param offset the offset of the ndarray
     * @return the instance
     */
    @Override
    public INDArray create(float[] data, int[] shape, int[] stride, long offset) {
        return new NDArray(data, shape, stride, offset);
    }

    /**
     * Creates a complex ndarray with the specified shape
     *
     * @param data
     * @param shape  the shape of the ndarray
     * @param stride the stride for the ndarray
     * @param offset the offset of the ndarray
     * @return the instance
     */
    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, int[] stride, long offset) {
        return new ComplexNDArray(Nd4j.createBuffer(data), shape, stride, offset);
    }


    /**
     * Creates an ndarray with the specified shape
     *
     * @param data
     * @param shape  the shape of the ndarray
     * @param stride the stride for the ndarray
     * @param offset the offset of the ndarray
     * @return the instance
     */
    @Override
    public INDArray create(double[] data, int[] shape, int[] stride, long offset) {
        return new NDArray(data, shape, stride, offset);
    }

    @Override
    public INDArray create(DataBuffer data, int[] shape) {
        return new NDArray(data, shape);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape) {
        return new ComplexNDArray(data, shape);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape, int[] stride) {
        return new ComplexNDArray(data, shape, stride);
    }

    @Override
    public INDArray create(DataBuffer data, int[] shape, int[] stride, long offset) {
        return new NDArray(data, shape, stride, offset, Nd4j.order());
    }

    /**
     * Creates an ndarray with the specified shape
     *
     * @param list
     * @param shape the shape of the ndarray
     * @return the instance
     */
    @Override
    public INDArray create(List<INDArray> list, int[] shape) {
        return new NDArray(list, shape, Nd4j.getStrides(shape));

    }


    /**
     * Create a complex ndarray with the given data
     *
     * @param data     the data to use with tne ndarray
     * @param shape    the shape of the ndarray
     * @param stride   the stride for the ndarray
     * @param offset   the offset of the ndarray
     * @param ordering the ordering for the ndarray
     * @return the created complex ndarray
     */
    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new ComplexNDArray(ArrayUtil.floatCopyOf(data), shape, stride, offset, ordering);
    }

    /**
     * @param data
     * @param shape
     * @param offset
     * @param ordering
     * @return
     */
    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, long offset, char ordering) {
        return new ComplexNDArray(ArrayUtil.floatCopyOf(data), shape, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer buffer, int[] shape, long offset, char ordering) {
        return new ComplexNDArray(buffer, shape, Nd4j.getComplexStrides(shape), offset, ordering);
    }

    /**
     * @param data
     * @param shape
     * @param offset
     * @return
     */
    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, long offset) {
        return new ComplexNDArray(ArrayUtil.floatCopyOf(data), shape, offset);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer buffer, int[] shape, long offset) {
        return new ComplexNDArray(buffer, shape, Nd4j.getComplexStrides(shape), offset, Nd4j.order());
    }

    /**
     * Create a complex ndarray with the given data
     *
     * @param data     the data to use with tne ndarray
     * @param shape    the shape of the ndarray
     * @param stride   the stride for the ndarray
     * @param offset   the offset of the ndarray
     * @param ordering the ordering for the ndarray
     * @return the created complex ndarray
     */
    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new ComplexNDArray(data, shape, stride, offset, ordering);
    }

    @Override
    public INDArray create(float[][] floats) {
        return new NDArray(floats);
    }

    @Override
    public INDArray create(float[][] data, char ordering) {
        return new NDArray(data, ordering);
    }

    @Override
    public IComplexNDArray createComplex(float[] dim) {
        return new ComplexNDArray(dim);
    }

    @Override
    public INDArray create(float[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new NDArray(data, shape, stride, offset, ordering);
    }

    @Override
    public INDArray create(DataBuffer buffer, int[] shape, long offset) {
        return new NDArray(buffer, shape, Nd4j.getStrides(shape), offset);
    }

    /**
     * @param data
     * @param shape
     * @param offset
     * @param ordering
     * @return
     */
    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, long offset, char ordering) {
        return new ComplexNDArray(data, shape, Nd4j.getComplexStrides(shape, ordering), offset, ordering);

    }

    /**
     * @param data
     * @param shape
     * @param offset
     * @return
     */
    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, long offset) {
        return new ComplexNDArray(data, shape, offset);
    }

    @Override
    public INDArray create(float[] data, int[] shape, long offset) {
        return new NDArray(data, shape, offset);
    }

    @Override
    public INDArray toFlattened(char order, Collection<INDArray> matrices) {
        int length = 0;
        for (INDArray m : matrices)
            length += m.length();
        INDArray ret = Nd4j.create(new int[] {1, length}, order);
        int linearIndex = 0;
        PointerPointer dummy = new PointerPointer(new Pointer[] {null});
        for (INDArray m : matrices) {
            Nd4j.getCompressor().autoDecompress(m);

            if (m.ordering() == order && m.data().allocationMode() == DataBuffer.AllocationMode.HEAP
                    && Shape.strideDescendingCAscendingF(m) && Shape.isContiguousInBuffer(m)) {
                //Can do array copy
                int retFrom = linearIndex;
                long mFrom = m.offset();
                Object arr = m.data().array();
                if (arr instanceof float[]) {
                    float[] mData = (float[]) arr;
                    float[] retData = (float[]) ret.data().array();

                    // FIXME: LONG
                    System.arraycopy(mData, (int) mFrom, retData, retFrom, m.length());
                } else {
                    double[] mData = (double[]) arr;
                    double[] retData = (double[]) ret.data().array();

                    // FIXME: LONG
                    System.arraycopy(mData, (int) mFrom, retData, retFrom, m.length());
                }
                linearIndex += m.length();
            } else {
                if (m.data().dataType() == DataBuffer.Type.DOUBLE) {
                    nativeOps.flattenDouble(dummy, linearIndex, order, (DoublePointer) ret.data().addressPointer(),
                            (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                            (DoublePointer) m.data().addressPointer(),
                            (IntPointer) m.shapeInfoDataBuffer().addressPointer());
                } else if (m.data().dataType() == DataBuffer.Type.FLOAT) {
                    nativeOps.flattenFloat(dummy, linearIndex, order, (FloatPointer) ret.data().addressPointer(),
                            (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                            (FloatPointer) m.data().addressPointer(),
                            (IntPointer) m.shapeInfoDataBuffer().addressPointer());

                } else {
                    throw new UnsupportedOperationException("Illegal data opType for copy");
                }
                //Works for all cases...

                /* NdIndexIterator iter = new NdIndexIterator(order, m.shape());
                while (iter.hasNext()) {
                    ret.putScalar(linearIndex++, m.getDouble(iter.next()));
                }*/

                linearIndex += m.length();

            }
        }
        return ret;
    }

    public INDArray[] tear(INDArray tensor, int... dimensions) {
        if (tensor.isCompressed())
            Nd4j.getCompressor().decompressi(tensor);

        Arrays.sort(dimensions);

        Pair<DataBuffer, DataBuffer> tadBuffers = Nd4j.getExecutioner().getTADManager().getTADOnlyShapeInfo(tensor, dimensions);

        long tadLength = 1;
        int[] shape = new int[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            tadLength *= tensor.shape()[dimensions[i]];
            shape[i] = tensor.shape()[dimensions[i]];
        }



        int numTads = (int)(tensor.lengthLong() / tadLength);
        INDArray[] result = new INDArray[numTads];

        PointerPointer targets = new PointerPointer(numTads);

        for (int x = 0; x < numTads; x++) {
            result[x] = Nd4j.createUninitialized(shape);

            targets.put(x, result[x].data().pointer());
        }

        if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.tearDouble(null,
                    (DoublePointer) tensor.data().pointer(),
                    (IntPointer) tensor.shapeInfoDataBuffer().pointer(),
                    targets,
                    (IntPointer) result[0].shapeInfoDataBuffer().pointer(),
                    (IntPointer) tadBuffers.getFirst().pointer(),
                    new LongPointerWrapper(tadBuffers.getSecond().pointer())
            );
        } else if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.tearFloat(null,
                    (FloatPointer) tensor.data().pointer(),
                    (IntPointer) tensor.shapeInfoDataBuffer().pointer(),
                    targets,
                    (IntPointer) result[0].shapeInfoDataBuffer().pointer(),
                    (IntPointer) tadBuffers.getFirst().pointer(),
                    new LongPointerWrapper(tadBuffers.getSecond().pointer())
                    );
        } else if (Nd4j.dataType() == DataBuffer.Type.HALF) {
            throw new UnsupportedOperationException("Half precision isn't supported for CPU backend");
        }

        return result;
    }

    /**
     * concatenate ndarrays along a dimension
     *
     * @param dimension the dimension to concatenate along
     * @param toConcat  the ndarrays to concatenate
     * @return the concatenate ndarrays
     */
    @Override
    public INDArray concat(int dimension, INDArray... toConcat) {
        if (toConcat.length == 1)
            return toConcat[0];

        // if reusable var wasn't created for this thread, or is smaller then needed - set it to new value
        if (extrazA.get() == null || extrazB.get() == null || extrazSize.get() == null || extrazSize.get() < toConcat.length) {
            extrazA.set(new PointerPointer(toConcat.length));
            extrazB.set(new PointerPointer(toConcat.length));
            extrazSize.set(toConcat.length);
        }

        PointerPointer shapeInfoPointers = extrazA.get();
        PointerPointer dataPointers = extrazB.get();

        int sumAlongDim = 0;

        int[] outputShape = ArrayUtil.copy(toConcat[0].shape());


        for (int i = 0; i < toConcat.length; i++) {
            if (toConcat[i].isCompressed())
                Nd4j.getCompressor().decompressi(toConcat[i]);

            shapeInfoPointers.put(i, toConcat[i].shapeInfoDataBuffer().addressPointer());
            dataPointers.put(i, toConcat[i].data().addressPointer());
            sumAlongDim += toConcat[i].size(dimension);
            for (int j = 0; j < toConcat[i].rank(); j++)
                if (j != dimension && toConcat[i].size(j) != outputShape[j]) {
                    throw new IllegalArgumentException(
                            "Illegal concatenation at array " + i + " and shape element " + j);
                }


            //log.info("Shape[{}]: {}", i, Arrays.toString(toConcat[i].shapeInfoDataBuffer().asInt()));
        }

        outputShape[dimension] = sumAlongDim;

        //PointerPointer dummy = new PointerPointer(new Pointer[] {null});

        INDArray ret = Nd4j.createUninitialized(outputShape, Nd4j.order());

        if (ret.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.concatDouble(null, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (DoublePointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    //new PointerPointer(new Pointer[] {null}), new PointerPointer(new Pointer[] {null}));
                    null, null);
        } else if (ret.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.concatFloat(null, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (FloatPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    //new PointerPointer(new Pointer[] {null}), new PointerPointer(new Pointer[] {null}));
                    null, null);

        } else if (ret.data().dataType() == DataBuffer.Type.HALF) {
            nativeOps.concatHalf(null, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (ShortPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    //new PointerPointer(new Pointer[]{null}), new PointerPointer(new Pointer[]{null}));
                    null, null);
        } else {
            throw new ND4JIllegalStateException("Unknown dataType: " + ret.data().dataType());
        }
        return ret;
        // return super.concat(dimension,toConcat);
    }


    /**
     * For CPU backend this method is equal to concat()
     *
     * @param dimension the dimension to concatneate along
     * @param toConcat  the ndarrays to concateneate
     * @return
     */
    @Override
    public INDArray specialConcat(int dimension, INDArray... toConcat) {
        return concat(dimension, toConcat);
    }

    /**
     * This method produces concatenated array, that consist from tensors, fetched from source array, against some dimension and specified indexes
     *
     * @param source          source tensor
     * @param sourceDimension dimension of source tensor
     * @param indexes         indexes from source array
     * @return
     */
    @Override
    public INDArray pullRows(INDArray source, int sourceDimension, int[] indexes) {
        return pullRows(source, sourceDimension, indexes, Nd4j.order());
    }

    /**
     * This method produces concatenated array, that consist from tensors, fetched from source array, against some dimension and specified indexes
     *
     * @param source          source tensor
     * @param sourceDimension dimension of source tensor
     * @param indexes         indexes from source array
     * @return
     */
    @Override
    public INDArray pullRows(INDArray source, int sourceDimension, int[] indexes, char order) {
        if (indexes == null || indexes.length < 1)
            throw new IllegalStateException("Indexes can't be null or zero-length");

        int[] shape = null;
        if (sourceDimension == 1)
            shape = new int[] {indexes.length, source.shape()[sourceDimension]};
        else if (sourceDimension == 0)
            shape = new int[] {source.shape()[sourceDimension], indexes.length};
        else
            throw new UnsupportedOperationException("2D input is expected");

        INDArray ret = Nd4j.createUninitialized(shape, order);

        Nd4j.getCompressor().autoDecompress(source);

        PointerPointer dummy = new PointerPointer(new Pointer[] {null});

        TADManager tadManager = Nd4j.getExecutioner().getTADManager();

        Pair<DataBuffer, DataBuffer> tadBuffers = tadManager.getTADOnlyShapeInfo(source, new int[] {sourceDimension});

        Pair<DataBuffer, DataBuffer> zTadBuffers = tadManager.getTADOnlyShapeInfo(ret, new int[] {sourceDimension});

        Pointer hostTadShapeInfo = tadBuffers.getFirst().addressPointer();

        Pointer zTadShapeInfo = zTadBuffers.getFirst().addressPointer();

        IntPointer pIndex = new IntPointer(indexes);

        DataBuffer offsets = tadBuffers.getSecond();
        Pointer hostTadOffsets = offsets == null ? null : offsets.addressPointer();

        DataBuffer zOffsets = zTadBuffers.getSecond();

        Pointer zTadOffsets = zOffsets == null ? null : zOffsets.addressPointer();

        if (ret.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.pullRowsDouble(dummy, (DoublePointer) source.data().addressPointer(),
                    (IntPointer) source.shapeInfoDataBuffer().addressPointer(),
                    (DoublePointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(), indexes.length, pIndex,
                    (IntPointer) hostTadShapeInfo, new LongPointerWrapper(hostTadOffsets), (IntPointer) zTadShapeInfo,
                    new LongPointerWrapper(zTadOffsets));
        } else if (ret.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.pullRowsFloat(dummy, (FloatPointer) source.data().addressPointer(),
                    (IntPointer) source.shapeInfoDataBuffer().addressPointer(),
                    (FloatPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(), indexes.length, pIndex,
                    (IntPointer) hostTadShapeInfo, new LongPointerWrapper(hostTadOffsets), (IntPointer) zTadShapeInfo,
                    new LongPointerWrapper(zTadOffsets));

        } else {
            nativeOps.pullRowsHalf(dummy, (ShortPointer) source.data().addressPointer(),
                    (IntPointer) source.shapeInfoDataBuffer().addressPointer(),
                    (ShortPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(), indexes.length, pIndex,
                    (IntPointer) hostTadShapeInfo, new LongPointerWrapper(hostTadOffsets), (IntPointer) zTadShapeInfo,
                    new LongPointerWrapper(zTadOffsets));
        }

        return ret;
    }

    public INDArray accumulate(INDArray target, INDArray... arrays) {

        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        if (arrays.length == 1)
            return target.addi(arrays[0]);

        long len = target.lengthLong();

        PointerPointer dataPointers = new PointerPointer(arrays.length);

        for (int i = 0; i < arrays.length; i++) {
            Nd4j.getCompressor().autoDecompress(arrays[i]);

            if (arrays[i].elementWiseStride() != 1)
                throw new ND4JIllegalStateException("Native accumulation is applicable only to continuous INDArrays");

            if (arrays[i].lengthLong() != len)
                throw new ND4JIllegalStateException("All arrays should have equal length for accumulation");

            dataPointers.put(i, arrays[i].data().addressPointer());
        }

        if (target.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.accumulateDouble(null, dataPointers, (DoublePointer) target.data().addressPointer(), arrays.length, len);
        } else if (target.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.accumulateFloat(null, dataPointers, (FloatPointer) target.data().addressPointer(), arrays.length, len);
        } else {
            nativeOps.accumulateHalf(null, dataPointers, (ShortPointer) target.data().addressPointer(), arrays.length, len);
        }

        return target;
    }

    /**
     * This method averages input arrays, and returns averaged array
     *
     * @param target
     * @param arrays
     * @return
     */
    @Override
    public INDArray average(INDArray target, INDArray[] arrays) {
        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        if (arrays.length == 1)
            return target.assign(arrays[0]);

        long len = target != null ? target.lengthLong() : arrays[0].length();

        PointerPointer dataPointers = new PointerPointer(arrays.length);

        for (int i = 0; i < arrays.length; i++) {
            Nd4j.getCompressor().autoDecompress(arrays[i]);

            if (arrays[i].elementWiseStride() != 1)
                throw new ND4JIllegalStateException("Native averaging is applicable only to continuous INDArrays");

            if (arrays[i].lengthLong() != len)
                throw new ND4JIllegalStateException("All arrays should have equal length for averaging");

            dataPointers.put(i, arrays[i].data().addressPointer());
        }

        if (arrays[0].data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.averageDouble(null, dataPointers, target == null ? null : (DoublePointer) target.data().addressPointer(), arrays.length,
                    len, true);
        } else if (arrays[0].data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.averageFloat(null, dataPointers, target == null ? null : (FloatPointer) target.data().addressPointer(), arrays.length,
                    len, true);
        } else {
            nativeOps.averageHalf(null, dataPointers, target == null ? null :  (ShortPointer) target.data().addressPointer(), arrays.length, len, true);
        }

        return target;
    }

    /**
     * This method averages input arrays, and returns averaged array
     *
     * @param target
     * @param arrays
     * @return
     */
    @Override
    public INDArray average(INDArray target, Collection<INDArray> arrays) {
        return average(target, arrays.toArray(new INDArray[0]));
    }

    @Override
    public INDArray average(INDArray[] arrays) {
        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        INDArray ret = Nd4j.createUninitialized(arrays[0].shape(), arrays[0].ordering());

        return average(ret, arrays);
    }

    @Override
    public INDArray average(Collection<INDArray> arrays) {
        return average(arrays.toArray(new INDArray[0]));
    }

    /**
     * In place shuffle of an ndarray
     * along a specified set of dimensions
     *
     * @param array     the ndarray to shuffle
     * @param dimension the dimension to do the shuffle
     * @return
     */
    @Override
    public void shuffle(INDArray array, Random rnd, int... dimension) {
        shuffle(Collections.singletonList(array), rnd, dimension);
    }

    /**
     * Symmetric in place shuffle of an ndarray
     * along a specified set of dimensions. All arrays
     *
     * @param array     the ndarray to shuffle
     * @param dimension the dimension to do the shuffle
     * @return
     */
    @Override
    public void shuffle(Collection<INDArray> array, Random rnd, int... dimension) {
        shuffle(new ArrayList<INDArray>(array), rnd, Collections.singletonList(dimension));
    }

    /**
     * Symmetric in place shuffle of an ndarray
     * along a specified set of dimensions. Each array in list should have it's own dimension at the same index of dimensions array
     *
     * @param arrays      the ndarrays to shuffle
     * @param dimensions the dimensions to do the shuffle
     * @return
     */
    @Override
    public void shuffle(List<INDArray> arrays, Random rnd, List<int[]> dimensions) {
        if (dimensions == null || dimensions.size() == 0)
            throw new RuntimeException("Dimension can't be null or 0-length");

        if (arrays == null || arrays.size() == 0)
            throw new RuntimeException("No input arrays provided");

        if (dimensions.size() > 1 && arrays.size() != dimensions.size())
            throw new IllegalStateException("Number of dimensions do not match number of arrays to shuffle");

        int tadLength = 1;
        for (int i = 0; i < dimensions.get(0).length; i++) {
            tadLength *= arrays.get(0).shape()[dimensions.get(0)[i]];
        }

        int numTads = arrays.get(0).length() / tadLength;

        int[] map = ArrayUtil.buildInterleavedVector(rnd, numTads);

        PointerPointer dataPointers = new PointerPointer(arrays.size());
        PointerPointer shapePointers = new PointerPointer(arrays.size());
        PointerPointer tadPointers = new PointerPointer(arrays.size());
        PointerPointer offsetPointers = new PointerPointer(arrays.size());

        PointerPointer dummy = new PointerPointer(new Pointer[] {null});

        List<Pair<DataBuffer, DataBuffer>> list = new ArrayList<>();

        TADManager tadManager = Nd4j.getExecutioner().getTADManager();

        IntPointer ptrMap = new IntPointer(map);

        long[] ptrs = new long[arrays.size()];


        for (int i = 0; i < arrays.size(); i++) {
            INDArray array = arrays.get(i);

            Nd4j.getCompressor().autoDecompress(array);


            int[] dimension = dimensions.size() > 1 ? dimensions.get(i) : dimensions.get(0);

            Pair<DataBuffer, DataBuffer> tadBuffers = tadManager.getTADOnlyShapeInfo(array, dimension);
            list.add(tadBuffers);

            Pointer hostTadShapeInfo = tadBuffers.getFirst().addressPointer();

            DataBuffer offsets = tadBuffers.getSecond();

            if (offsets.length() != numTads)
                throw new ND4JIllegalStateException("Can't symmetrically shuffle arrays with non-equal number of TADs");

            if (offsets == null)
                throw new ND4JIllegalStateException("Offsets for shuffle can't be null");


            dataPointers.put(i, array.data().addressPointer());
            shapePointers.put(i, array.shapeInfoDataBuffer().addressPointer());
            offsetPointers.put(i, offsets.addressPointer());
            tadPointers.put(i, tadBuffers.getFirst().addressPointer());
        }

        if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.shuffleDouble(dummy, dataPointers, shapePointers, dataPointers, shapePointers, arrays.size(),
                    ptrMap, tadPointers, offsetPointers);
        } else if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.shuffleFloat(dummy, dataPointers, shapePointers, dataPointers, shapePointers, arrays.size(),
                    ptrMap, tadPointers, offsetPointers);
        } else {
            // HALFs
        }

        dataPointers.address();
        shapePointers.address();
        tadPointers.address();
        offsetPointers.address();
    }


    /**
     * This method converts Half-precision databuffer to current dType buffer.
     *
     * @param buffer
     * @return
     */
    /*
    @Override
    public DataBuffer restoreFromHalfs(DataBuffer buffer) {
        if (buffer.dataType() != DataBuffer.Type.COMPRESSED)
            throw new IllegalStateException("DataBuffer contains wrong data: " + buffer.dataType());
    
        CompressedDataBuffer comp = (CompressedDataBuffer) buffer;
        CompressionDescriptor descriptor = comp.getCompressionDescriptor();
    
        DataBuffer targetBuffer = Nd4j.createBuffer(descriptor.getCompressedLength() / 2);
    
        if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.convertHalfsToDoubles(
                    null,
                    comp.addressPointer(),
                    (int) descriptor.getCompressedLength() / 2,
                    targetBuffer.addressPointer()
            );
        } else if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.convertHalfsToFloats(
                    null,
                    comp.addressPointer(),
                    (int) descriptor.getCompressedLength() / 2,
                    targetBuffer.addressPointer()
            );
        } else {
            throw new UnsupportedOperationException("Target dtype isn't supported: " + Nd4j.dataType());
        }
    
        return targetBuffer;
    }
    */

    /**
     * This method converts Single/Double precision databuffer to Half-precision databuffer
     *
     * @param buffer
     * @return
     */
    /*@Override
    public DataBuffer convertToHalfs(DataBuffer buffer) {
        // we allocate pointer
        ShortPointer pointer = new ShortPointer(buffer.length());
    
        if (buffer.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.convertDoublesToHalfs(
                    null,
                    buffer.addressPointer(),
                    (int) buffer.length(),
                    pointer
            );
        } else if (buffer.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.convertFloatsToHalfs(
                    null,
                    buffer.addressPointer(),
                    (int) buffer.length(),
                    pointer
            );
        } else {
            throw new UnsupportedOperationException("Source dtype isn't supported: " + buffer.dataType());
        }
    
        CompressionDescriptor descriptor = new CompressionDescriptor(buffer, new Float16());
        descriptor.setCompressedLength(buffer.length() * 2);
    
    
        CompressedDataBuffer result = new CompressedDataBuffer(pointer, descriptor);
        return result;
    }
    */

    /**
     * This method converts Single/Double precision databuffer to Half-precision databuffer
     *
     * @param typeSrc
     * @param source
     * @param typeDst @return
     */
    @Override
    public INDArray convertDataEx(DataBuffer.TypeEx typeSrc, INDArray source, DataBuffer.TypeEx typeDst) {
        if (source.isView())
            throw new UnsupportedOperationException("Impossible to compress View. Consider using dup() before. ");

        DataBuffer buffer = convertDataEx(typeSrc, source.data(), typeDst);
        source.setData(buffer);

        if (buffer instanceof CompressedDataBuffer)
            source.markAsCompressed(true);
        else
            source.markAsCompressed(false);

        return source;
    }

    @Override
    public DataBuffer convertDataEx(DataBuffer.TypeEx typeSrc, DataBuffer source, DataBuffer.TypeEx typeDst) {
        int elementSize = 0;
        if (typeDst.ordinal() <= 2)
            elementSize = 1;
        else if (typeDst.ordinal() <= 5)
            elementSize = 2;
        else if (typeDst.ordinal() == 6)
            elementSize = 4;
        else if (typeDst.ordinal() == 7)
            elementSize = 8;
        else
            throw new UnsupportedOperationException("Unknown target TypeEx: " + typeDst.name());

        DataBuffer buffer = null;


        if (typeDst.ordinal() < 8) {
            // all types below 6 are compression modes
            BytePointer pointer = new BytePointer(source.length() * elementSize);
            CompressionDescriptor descriptor = new CompressionDescriptor(source, typeDst.name());
            descriptor.setCompressionType(CompressionType.LOSSY);
            descriptor.setCompressedLength(source.length() * elementSize);
            buffer = new CompressedDataBuffer(pointer, descriptor);
        } else {
            CompressedDataBuffer compressed = (CompressedDataBuffer) source;
            CompressionDescriptor descriptor = compressed.getCompressionDescriptor();

            // decompression mode
            buffer = Nd4j.createBuffer(descriptor.getNumberOfElements(), true);
        }

        convertDataEx(typeSrc, source, typeDst, buffer);

        return buffer;
    }

    @Override
    public void convertDataEx(DataBuffer.TypeEx typeSrc, Pointer source, DataBuffer.TypeEx typeDst, Pointer target,
                              long length) {
        nativeOps.convertTypes(null, typeSrc.ordinal(), source, length, typeDst.ordinal(), target);
    }

    @Override
    public void convertDataEx(DataBuffer.TypeEx typeSrc, DataBuffer source, DataBuffer.TypeEx typeDst,
                              DataBuffer target) {
        convertDataEx(typeSrc, source.addressPointer(), typeDst, target.addressPointer(), target.length());
    }


    /**
     * Create from an in memory numpy pointer
     *
     * @param pointer the pointer to the
     *                numpy array
     * @return an ndarray created from the in memory
     * numpy pointer
     */
    @Override
    public INDArray createFromNpyPointer(Pointer pointer) {
        Pointer dataPointer = nativeOps.dataPointForNumpy(pointer);
        int dataBufferElementSize = nativeOps.elementSizeForNpyArray(pointer);
        DataBuffer data = null;
        Pointer shapeBufferPointer = nativeOps.shapeBufferForNumpy(pointer);
        int length = nativeOps.lengthForShapeBufferPointer(shapeBufferPointer);
        shapeBufferPointer.capacity(4 * length);
        shapeBufferPointer.limit(4 * length);
        shapeBufferPointer.position(0);


        IntPointer intPointer = new IntPointer(shapeBufferPointer);
        IntPointer newPointer = new IntPointer(length);

        Pointer.memcpy(newPointer, intPointer, shapeBufferPointer.limit());

        DataBuffer shapeBuffer = Nd4j.createBuffer(
                newPointer,
                DataBuffer.Type.INT,
                length,
                IntIndexer.create(newPointer));

        dataPointer.position(0);
        dataPointer.limit(dataBufferElementSize * Shape.length(shapeBuffer));
        dataPointer.capacity(dataBufferElementSize * Shape.length(shapeBuffer));


        if(dataBufferElementSize == (Float.SIZE / 8)) {
            FloatPointer dPointer = new FloatPointer(dataPointer.limit() / dataBufferElementSize);
            Pointer.memcpy(dPointer, dataPointer, dataPointer.limit());

            data = Nd4j.createBuffer(dPointer,
                    DataBuffer.Type.FLOAT,
                    Shape.length(shapeBuffer),
                    FloatIndexer.create(dPointer));
        }
        else if(dataBufferElementSize == (Double.SIZE / 8)) {
            DoublePointer dPointer = new DoublePointer(dataPointer.limit() / dataBufferElementSize);
            Pointer.memcpy(dPointer, dataPointer, dataPointer.limit());

            data = Nd4j.createBuffer(dPointer,
                    DataBuffer.Type.DOUBLE,
                    Shape.length(shapeBuffer),
                    DoubleIndexer.create(dPointer));
        }

        INDArray ret = Nd4j.create(data,
                Shape.shape(shapeBuffer),
                Shape.strideArr(shapeBuffer),
                Shape.offset(shapeBuffer),
                Shape.order(shapeBuffer));

        return ret;
    }

    /**
     * Create from a given numpy file.
     *
     * @param file the file to create the ndarray from
     * @return the created ndarray
     */
    @Override
    public INDArray createFromNpyFile(File file) {
        byte[] pathBytes = file.getAbsolutePath().getBytes(Charset.forName("UTF-8" ));
        String otherBytes = new String(pathBytes);
        System.out.println(otherBytes);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(pathBytes.length).order(ByteOrder.nativeOrder());
        directBuffer.put(pathBytes);
        directBuffer.rewind();
        directBuffer.position(0);
        Pointer pointer = nativeOps.numpyFromFile(new BytePointer(directBuffer));

        INDArray result = createFromNpyPointer(pointer);

        // releasing original pointer here
        nativeOps.releaseNumpy(pointer);
        return result;
    }

    @Override
    public INDArray createSparseCSR(double[] data, int[] columns, int[] pointerB, int[] pointerE, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCSR(float[] data, int[] columns, int[] pointerB, int[] pointerE, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCSR(DataBuffer data, int[] columns, int[] pointerB, int[] pointerE, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCOO(double[] values, int[][] indices, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCOO(float[] values, int[][] indices, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCOO(DataBuffer values, DataBuffer indices, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray createSparseCOO(DataBuffer values, DataBuffer indices, DataBuffer sparseInformation, int[] shape) {
        throw new UnsupportedOperationException();
    }


    @Override
    public INDArray createSparseCOO(DataBuffer values, DataBuffer indices, long[] sparseOffsets, int[] flags, int[] hiddenDimensions, int underlyingRank, int[] shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray sort(INDArray x, boolean descending) {
        if (x.isScalar())
            return x;

        if (x.data().dataType() == DataBuffer.Type.FLOAT) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().sortFloat(null, (FloatPointer) x.data().addressPointer(), (IntPointer) x.shapeInfoDataBuffer().addressPointer(), descending);
        } else if (x.data().dataType() == DataBuffer.Type.DOUBLE) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().sortDouble(null, (DoublePointer) x.data().addressPointer(), (IntPointer) x.shapeInfoDataBuffer().addressPointer(), descending);
        } else {
            throw new UnsupportedOperationException("Unknown dataype " + x.data().dataType());
        }
        return x;
    }

    @Override
    public INDArray sort(INDArray x, boolean descending, int... dimension) {
        if (x.isScalar())
            return x;

        Arrays.sort(dimension);
        Pair<DataBuffer, DataBuffer> tadBuffers = Nd4j.getExecutioner().getTADManager().getTADOnlyShapeInfo(x, dimension);

        if (x.data().dataType() == DataBuffer.Type.FLOAT) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().sortTadFloat(null,
                    (FloatPointer) x.data().addressPointer(),
                    (IntPointer) x.shapeInfoDataBuffer().addressPointer(),
                    (IntPointer) Nd4j.getConstantHandler().getConstantBuffer(dimension).addressPointer(),
                    dimension.length,
                    (IntPointer) tadBuffers.getFirst().addressPointer(),
                    new LongPointerWrapper(tadBuffers.getSecond().addressPointer()),
                    descending);
        } else if (x.data().dataType() == DataBuffer.Type.DOUBLE) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().sortTadDouble(null,
                    (DoublePointer) x.data().addressPointer(),
                    (IntPointer) x.shapeInfoDataBuffer().addressPointer(),
                    (IntPointer) Nd4j.getConstantHandler().getConstantBuffer(dimension).addressPointer(),
                    dimension.length,
                    (IntPointer) tadBuffers.getFirst().addressPointer(),
                    new LongPointerWrapper(tadBuffers.getSecond().addressPointer()),
                    descending);
        } else {
            throw new UnsupportedOperationException("Unknown dataype " + x.data().dataType());
        }

        return x;
    }

    @Override
    public INDArray sortCooIndices(INDArray x) {
        throw new UnsupportedOperationException("Not an COO ndarray");
    }
}
