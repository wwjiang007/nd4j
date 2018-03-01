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

package org.nd4j.linalg.jcublas;

import org.nd4j.linalg.primitives.Pair;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.nd4j.jita.allocator.enums.CudaConstants;
import org.nd4j.jita.allocator.impl.AllocationPoint;
import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.jita.allocator.pointers.CudaPointer;
import org.nd4j.jita.allocator.utils.AllocationUtils;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.executioner.GridExecutioner;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cache.TADManager;
import org.nd4j.linalg.compression.CompressedDataBuffer;
import org.nd4j.linalg.compression.CompressionDescriptor;
import org.nd4j.linalg.compression.CompressionType;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.BaseNDArrayFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.blas.*;
import org.nd4j.linalg.jcublas.buffer.AddressRetriever;
import org.nd4j.linalg.jcublas.buffer.CudaDoubleDataBuffer;
import org.nd4j.linalg.jcublas.buffer.CudaIntDataBuffer;
import org.nd4j.linalg.jcublas.complex.ComplexDouble;
import org.nd4j.linalg.jcublas.complex.ComplexFloat;
import org.nd4j.linalg.jcublas.complex.JCublasComplexNDArray;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.linalg.util.ArrayUtil;
import org.nd4j.nativeblas.LongPointerWrapper;
import org.nd4j.nativeblas.NativeOps;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Jcublas ndarray factory. Handles creation of
 * jcuda.jcublas ndarrays.
 *
 * @author mjk
 */
public class JCublasNDArrayFactory extends BaseNDArrayFactory {
    private NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps();
    private static Logger log = LoggerFactory.getLogger(JCublasNDArrayFactory.class);

    public JCublasNDArrayFactory() { }

    public JCublasNDArrayFactory(DataBuffer.Type dtype, Character order) {
        super(dtype, order);
    }

    public JCublasNDArrayFactory(DataBuffer.Type dtype, char order) {
        super(dtype, order);
        AtomicAllocator.getInstance();
    }

    @Override
    public void createBlas() {
        blas = new CudaBlas();
        PointerPointer functions = new PointerPointer(13);
        functions.put(0, Loader.addressof("cublasSgemv_v2"));
        functions.put(1, Loader.addressof("cublasDgemv_v2"));
        functions.put(2, Loader.addressof("cublasHgemm"));
        functions.put(3, Loader.addressof("cublasSgemm_v2"));
        functions.put(4, Loader.addressof("cublasDgemm_v2"));
        functions.put(5, Loader.addressof("cublasSgemmEx"));
        functions.put(6, Loader.addressof("cublasHgemmBatched"));
        functions.put(7, Loader.addressof("cublasSgemmBatched"));
        functions.put(8, Loader.addressof("cublasDgemmBatched"));
        functions.put(9, Loader.addressof("cusolverDnSgesvd_bufferSize"));
        functions.put(10, Loader.addressof("cusolverDnDgesvd_bufferSize"));
        functions.put(11, Loader.addressof("cusolverDnSgesvd"));
        functions.put(12, Loader.addressof("cusolverDnDgesvd"));
        nativeOps.initializeFunctions(functions);
    }

    @Override
    public void createLevel1() {
        level1 = new JcublasLevel1();
    }

    @Override
    public void createLevel2() {
        level2 = new JcublasLevel2();
    }

    @Override
    public void createLevel3() {
        level3 = new JcublasLevel3();
    }

    @Override
    public void createLapack() {
        lapack = new JcublasLapack();
    }

    @Override
    public INDArray create(int[] shape, DataBuffer buffer) {
        return new JCublasNDArray(shape, buffer);
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
        return new JCublasNDArray(data);
    }

    @Override
    public INDArray create(double[][] data, char ordering) {
        return new JCublasNDArray(data, ordering);
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
        return new JCublasComplexNDArray(arr);
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
        return new JCublasComplexNDArray(data, shape, Nd4j.getComplexStrides(shape, Nd4j.order()));
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
        return new JCublasComplexNDArray(arrs, shape);
    }

    @Override
    public INDArray create(DataBuffer data) {
        return new JCublasNDArray(data);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data) {
        return new JCublasComplexNDArray(data);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int rows, int columns, int[] stride, long offset) {
        return new JCublasComplexNDArray(data, new int[] {rows, columns}, stride, offset);
    }

    @Override
    public INDArray create(DataBuffer data, int rows, int columns, int[] stride, long offset) {
        return new JCublasNDArray(data, new int[] {rows, columns}, stride, offset);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape, int[] stride, long offset) {
        return new JCublasComplexNDArray(data, shape, stride, offset);
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
        return new JCublasComplexNDArray(data, shape, stride, offset);
    }

    @Override
    public INDArray create(int[] shape, char ordering) {
        return new JCublasNDArray(shape, ordering);
    }

    @Override
    public INDArray createUninitialized(int[] shape, char ordering) {
        return new JCublasNDArray(shape, Nd4j.getStrides(shape, ordering), 0, ordering, false);
    }

    @Override
    public INDArray createUninitializedDetached(int[] shape, char ordering) {
        MemoryWorkspace workspace = Nd4j.getMemoryManager().getCurrentWorkspace();
        Nd4j.getMemoryManager().setCurrentWorkspace(null);
        INDArray ret = new JCublasNDArray(shape, Nd4j.getStrides(shape, ordering), 0, ordering, false);
        Nd4j.getMemoryManager().setCurrentWorkspace(workspace);
        return ret;
    }

    @Override
    public INDArray create(DataBuffer data, int[] newShape, int[] newStride, long offset, char ordering) {
        return new JCublasNDArray(data, newShape, newStride, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] newDims, int[] newStrides, long offset, char ordering) {
        return new JCublasComplexNDArray(data, newDims, newStrides, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(float[] data, Character order) {
        return new JCublasComplexNDArray(data, order);
    }

    @Override
    public INDArray create(float[] data, int[] shape, long offset, Character order) {
        return new JCublasNDArray(data, shape, offset, order);
    }

    @Override
    public INDArray create(float[] data, int rows, int columns, int[] stride, long offset, char ordering) {
        return new JCublasNDArray(data, new int[] {rows, columns}, stride, offset, ordering);
    }

    @Override
    public INDArray create(double[] data, int[] shape, char ordering) {
        return new JCublasNDArray(data, shape, ordering);
    }

    @Override
    public INDArray create(List<INDArray> list, int[] shape, char ordering) {
        return new JCublasNDArray(list, shape, ordering);
    }

    @Override
    public INDArray create(double[] data, int[] shape, long offset) {
        return new JCublasNDArray(data, shape, (char) offset);
    }

    @Override
    public INDArray create(double[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new JCublasNDArray(data, shape, stride, offset, ordering);
    }


    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, long offset) {
        return new JCublasComplexNDArray(data, shape, stride, offset);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new JCublasComplexNDArray(data, shape, stride, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, int[] stride, char ordering) {
        return new JCublasComplexNDArray(data, shape, stride, ordering);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, long offset, char ordering) {
        return new JCublasComplexNDArray(data, shape, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(IComplexNumber[] data, int[] shape, char ordering) {
        return new JCublasComplexNDArray(data, shape, ordering);
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
        return new JCublasNDArray(data, shape, stride, offset);
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
        return new JCublasComplexNDArray(ArrayUtil.floatCopyOf(data), shape, stride, offset);
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
        return new JCublasNDArray(data, shape, stride, offset);
    }

    @Override
    public INDArray create(DataBuffer data, int[] shape) {
        return new JCublasNDArray(data, shape);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape) {
        return new JCublasComplexNDArray(data, shape);

    }

    @Override
    public IComplexNDArray createComplex(DataBuffer data, int[] shape, int[] stride) {
        return new JCublasComplexNDArray(data, shape, stride);
    }

    @Override
    public INDArray create(DataBuffer data, int[] shape, int[] stride, long offset) {
        return new JCublasNDArray(data, shape, stride, offset);
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
        if (order == FORTRAN)
            return new JCublasNDArray(list, shape, ArrayUtil.calcStridesFortran(shape));
        else
            return new JCublasNDArray(list, shape);
    }

    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new JCublasComplexNDArray(ArrayUtil.floatCopyOf(data), shape, stride, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, long offset, char ordering) {
        return new JCublasComplexNDArray(ArrayUtil.floatCopyOf(data), shape, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer buffer, int[] shape, long offset, char ordering) {
        return new JCublasComplexNDArray(buffer, shape, offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(double[] data, int[] shape, long offset) {
        return new JCublasComplexNDArray(ArrayUtil.floatCopyOf(data), shape, offset);
    }

    @Override
    public IComplexNDArray createComplex(DataBuffer buffer, int[] shape, long offset) {
        return new JCublasComplexNDArray(buffer, shape, offset);
    }

    @Override
    public INDArray create(float[] data, int[] shape, long offset) {
        return new JCublasNDArray(data, shape, offset);
    }

    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, long offset, char ordering) {
        return new JCublasComplexNDArray(data, shape, Nd4j.getComplexStrides(shape, ordering), offset, ordering);
    }

    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, long offset) {
        return new JCublasComplexNDArray(data, shape, offset);
    }

    @Override
    public IComplexNDArray createComplex(float[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new JCublasComplexNDArray(data, shape, stride, offset, ordering);
    }

    @Override
    public INDArray create(float[][] floats) {
        return new JCublasNDArray(floats);
    }

    @Override
    public INDArray create(float[][] data, char ordering) {
        return new JCublasNDArray(data, ordering);
    }

    @Override
    public IComplexNDArray createComplex(float[] dim) {
        if (dim.length % 2 != 0)
            throw new IllegalArgumentException("Complex nd array buffers must have an even number of elements");
        IComplexNDArray ret = Nd4j.createComplex(dim.length / 2);
        int count = 0;
        for (int i = 0; i < dim.length - 1; i += 2) {
            ret.putScalar(count++, Nd4j.createDouble(dim[i], dim[i + 1]));
        }
        return ret;
    }

    @Override
    public INDArray create(float[] data, int[] shape, int[] stride, long offset, char ordering) {
        return new JCublasNDArray(data, shape, stride, offset, ordering);
    }

    @Override
    public INDArray create(DataBuffer buffer, int[] shape, long offset) {
        return new JCublasNDArray(buffer, shape, offset);
    }


    @Override
    public INDArray toFlattened(Collection<INDArray> matrices) {
        return this.toFlattened(order(), matrices);
    }

    @Override
    public INDArray toFlattened(char order, Collection<INDArray> matrices) {
        if (Nd4j.getExecutioner() instanceof GridExecutioner)
            ((GridExecutioner) Nd4j.getExecutioner()).flushQueue();

        int length = 0;
        for (INDArray m : matrices)
            length += m.length();
        INDArray ret = Nd4j.create(new int[] {1, length}, order);
        int linearIndex = 0;

        AtomicAllocator allocator = AtomicAllocator.getInstance();


        for (INDArray m : matrices) {

            CudaContext context = allocator.getFlowController().prepareAction(ret, m);

            if (m.ordering() == order && ret.elementWiseStride() == m.elementWiseStride()
                            && ret.elementWiseStride() == 1) {
                // do memcpy in proper direction and forget about that
                allocator.memcpyAsync(ret.data(), new CudaPointer(allocator.getHostPointer(m).address()),
                                AllocationUtils.getRequiredMemory(AllocationUtils.buildAllocationShape(m)),
                                linearIndex * (m.data().dataType() == DataBuffer.Type.DOUBLE ? 8
                                                : m.data().dataType() == DataBuffer.Type.FLOAT ? 4 : 2));
                linearIndex += m.length();
            } else {
                Pointer hostYShapeInfo = AddressRetriever.retrieveHostPointer(m.shapeInfoDataBuffer());

                PointerPointer extras = new PointerPointer(
                                AddressRetriever.retrieveHostPointer(ret.shapeInfoDataBuffer()), context.getOldStream(),
                                allocator.getDeviceIdPointer(), context.getBufferAllocation(),
                                context.getBufferReduction(), context.getBufferScalar(), context.getBufferSpecial(),
                                hostYShapeInfo, AddressRetriever.retrieveHostPointer(ret.shapeInfoDataBuffer()));

                if (m.data().dataType() == DataBuffer.Type.DOUBLE) {
                    nativeOps.flattenDouble(extras, linearIndex, order,
                                    (DoublePointer) allocator.getPointer(ret, context),
                                    (IntPointer) allocator.getPointer(ret.shapeInfoDataBuffer(), context),
                                    (DoublePointer) allocator.getPointer(m, context),
                                    (IntPointer) allocator.getPointer(m.shapeInfoDataBuffer(), context));
                } else if (m.data().dataType() == DataBuffer.Type.FLOAT) {
                    nativeOps.flattenFloat(extras, linearIndex, order,
                                    (FloatPointer) allocator.getPointer(ret, context),
                                    (IntPointer) allocator.getPointer(ret.shapeInfoDataBuffer(), context),
                                    (FloatPointer) allocator.getPointer(m, context),
                                    (IntPointer) allocator.getPointer(m.shapeInfoDataBuffer(), context));

                } else {
                    nativeOps.flattenHalf(extras, linearIndex, order, (ShortPointer) allocator.getPointer(ret, context),
                                    (IntPointer) allocator.getPointer(ret.shapeInfoDataBuffer(), context),
                                    (ShortPointer) allocator.getPointer(m, context),
                                    (IntPointer) allocator.getPointer(m.shapeInfoDataBuffer(), context));
                }



                //Works for all cases...

                /* NdIndexIterator iter = new NdIndexIterator(order, m.shape());
                while (iter.hasNext()) {
                    ret.putScalar(linearIndex++, m.getDouble(iter.next()));
                }*/

                linearIndex += m.length();
            }

            if (ret != null)
                allocator.registerAction(context, ret, m);
        }
        return ret;
    }

    @Override
    public INDArray concat(int dimension, INDArray... toConcat) {
        if (Nd4j.getExecutioner() instanceof GridExecutioner)
            ((GridExecutioner) Nd4j.getExecutioner()).flushQueue();

        if (toConcat.length == 1)
            return toConcat[0];

        int sumAlongDim = 0;
        for (int i = 0; i < toConcat.length; i++) {
            if (toConcat[i].isCompressed())
                Nd4j.getCompressor().decompressi(toConcat[i]);

            sumAlongDim += toConcat[i].size(dimension);
        }

        int[] outputShape = ArrayUtil.copy(toConcat[0].shape());

        outputShape[dimension] = sumAlongDim;

        INDArray ret = Nd4j.createUninitialized(outputShape, Nd4j.order());

        AtomicAllocator allocator = AtomicAllocator.getInstance();

        CudaContext context = allocator.getFlowController().prepareAction(ret, toConcat);

        long[] shapeInfoPointers = new long[toConcat.length];
        long[] dataPointers = new long[toConcat.length];
        long[] tadPointers = new long[toConcat.length];
        long[] offsetsPointers = new long[toConcat.length];
        long[] hostShapeInfoPointers = new long[toConcat.length];

        TADManager tadManager = Nd4j.getExecutioner().getTADManager();
        for (int i = 0; i < toConcat.length; i++) {
            shapeInfoPointers[i] = AddressRetriever.retrieveDeviceAddress(toConcat[i].shapeInfoDataBuffer(), context);
            dataPointers[i] = AtomicAllocator.getInstance().getPointer(toConcat[i], context).address();
            hostShapeInfoPointers[i] =
                            AtomicAllocator.getInstance().getHostPointer(toConcat[i].shapeInfoDataBuffer()).address();

            sumAlongDim += toConcat[i].size(dimension);
            for (int j = 0; j < toConcat[i].rank(); j++)
                if (j != dimension && toConcat[i].size(j) != outputShape[j]) {
                    throw new IllegalArgumentException(
                                    "Illegal concatenation at array " + i + " and shape element " + j);
                }

            Pair<DataBuffer, DataBuffer> tadBuffers =
                            tadManager.getTADOnlyShapeInfo(toConcat[i], new int[] {dimension});

            long devTadShapeInfo = AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context).address();

            DataBuffer offsets = tadBuffers.getSecond();
            long devTadOffsets = AtomicAllocator.getInstance().getPointer(offsets, context).address();

            tadPointers[i] = devTadShapeInfo;
            offsetsPointers[i] = devTadOffsets;

        }

        // getting tadOnlyShape for result
        Pair<DataBuffer, DataBuffer> zBuffers = tadManager.getTADOnlyShapeInfo(ret, new int[] {dimension});


        //System.out.println("shapePointers: " + Arrays.toString(shapeInfoPointers));

        Pointer dZ = AtomicAllocator.getInstance().getPointer(ret, context);
        Pointer dZShapeInfo = AddressRetriever.retrieveDevicePointer(ret.shapeInfoDataBuffer(), context);



        CudaDoubleDataBuffer tempData = new CudaDoubleDataBuffer(toConcat.length);
        CudaDoubleDataBuffer tempShapes = new CudaDoubleDataBuffer(toConcat.length);
        CudaDoubleDataBuffer tempTAD = new CudaDoubleDataBuffer(toConcat.length);
        CudaDoubleDataBuffer tempOffsets = new CudaDoubleDataBuffer(toConcat.length);

        AtomicAllocator.getInstance().memcpyBlocking(tempData, new LongPointer(dataPointers), dataPointers.length * 8,
                        0);
        AtomicAllocator.getInstance().memcpyBlocking(tempShapes, new LongPointer(shapeInfoPointers),
                        shapeInfoPointers.length * 8, 0);
        AtomicAllocator.getInstance().memcpyBlocking(tempTAD, new LongPointer(tadPointers), tadPointers.length * 8, 0);
        AtomicAllocator.getInstance().memcpyBlocking(tempOffsets, new LongPointer(offsetsPointers),
                        offsetsPointers.length * 8, 0);

        Pointer dataPointer = AtomicAllocator.getInstance().getPointer(tempData, context);
        Pointer shapesPointer = AtomicAllocator.getInstance().getPointer(tempShapes, context);
        Pointer tadPointer = AtomicAllocator.getInstance().getPointer(tempTAD, context);
        Pointer offsetPointer = AtomicAllocator.getInstance().getPointer(tempOffsets, context);


        // System.out.println("ShapesPointer after conversion: " + shapesPointer);

        PointerPointer extras = new PointerPointer(AddressRetriever.retrieveHostPointer(ret.shapeInfoDataBuffer()),
                        context.getOldStream(), allocator.getDeviceIdPointer(), context.getBufferAllocation(),
                        context.getBufferReduction(), context.getBufferScalar(), context.getBufferSpecial(),
                        AddressRetriever.retrieveHostPointer(toConcat[0].shapeInfoDataBuffer()),
                        AddressRetriever.retrieveHostPointer(ret.shapeInfoDataBuffer()),
                        new LongPointer(hostShapeInfoPointers),
                        AtomicAllocator.getInstance().getPointer(zBuffers.getFirst(), context), // getting zTADShape
                        AtomicAllocator.getInstance().getPointer(zBuffers.getSecond(), context) // getting zOffset
        );

        if (ret.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.concatDouble(extras, dimension, toConcat.length, new PointerPointer(new Pointer[] {dataPointer}),
                            new PointerPointer(new Pointer[] {shapesPointer}), (DoublePointer) dZ,
                            (IntPointer) dZShapeInfo, new PointerPointer(new Pointer[] {tadPointer}),
                            new PointerPointer(new Pointer[] {offsetPointer}));
        } else if (ret.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.concatFloat(extras, dimension, toConcat.length, new PointerPointer(new Pointer[] {dataPointer}),
                            new PointerPointer(new Pointer[] {shapesPointer}), (FloatPointer) dZ,
                            (IntPointer) dZShapeInfo, new PointerPointer(new Pointer[] {tadPointer}),
                            new PointerPointer(new Pointer[] {offsetPointer}));

        } else {
            nativeOps.concatHalf(extras, dimension, toConcat.length, new PointerPointer(new Pointer[] {dataPointer}),
                            new PointerPointer(new Pointer[] {shapesPointer}), (ShortPointer) dZ,
                            (IntPointer) dZShapeInfo, new PointerPointer(new Pointer[] {tadPointer}),
                            new PointerPointer(new Pointer[] {offsetPointer}));

        }

        allocator.registerAction(context, ret, toConcat);

        return ret;
        //return super.concat(dimension, toConcat);
    }


    @Override
    public INDArray specialConcat(int dimension, INDArray... toConcat) {
        if (toConcat.length == 1)
            return toConcat[0];

        if (Nd4j.getExecutioner() instanceof GridExecutioner)
            ((GridExecutioner) Nd4j.getExecutioner()).flushQueue();

        PointerPointer shapeInfoPointers = new PointerPointer(toConcat.length);
        PointerPointer dataPointers = new PointerPointer(toConcat.length);

        AtomicAllocator allocator = AtomicAllocator.getInstance();
        CudaContext context = (CudaContext) allocator.getDeviceContext().getContext();


        int sumAlongDim = 0;

        int[] outputShape = ArrayUtil.copy(toConcat[0].shape());


        for (int i = 0; i < toConcat.length; i++) {
            if (toConcat[i].isCompressed())
                Nd4j.getCompressor().decompressi(toConcat[i]);

            allocator.synchronizeHostData(toConcat[i]);
            shapeInfoPointers.put(i, allocator.getHostPointer(toConcat[i].shapeInfoDataBuffer()));
            dataPointers.put(i, allocator.getHostPointer(toConcat[i].data()));
            sumAlongDim += toConcat[i].size(dimension);

            for (int j = 0; j < toConcat[i].rank(); j++)
                if (j != dimension && toConcat[i].size(j) != outputShape[j]) {
                    throw new IllegalArgumentException(
                            "Illegal concatenation at array " + i + " and shape element " + j);
                }
        }

        outputShape[dimension] = sumAlongDim;

        PointerPointer dummy = new PointerPointer(new Pointer[] {null});

        INDArray ret = Nd4j.createUninitialized(outputShape, Nd4j.order());

        if (ret.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.specialConcatDouble(dummy, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (DoublePointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    new PointerPointer(new Pointer[] {null}), new PointerPointer(new Pointer[] {null}));
        } else if (ret.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.specialConcatFloat(dummy, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (FloatPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    new PointerPointer(new Pointer[] {null}), new PointerPointer(new Pointer[] {null}));

        } else if (ret.data().dataType() == DataBuffer.Type.HALF) {
            nativeOps.specialConcatHalf(dummy, dimension, toConcat.length, dataPointers, shapeInfoPointers,
                    (ShortPointer) ret.data().addressPointer(),
                    (IntPointer) ret.shapeInfoDataBuffer().addressPointer(),
                    new PointerPointer(new Pointer[]{null}), new PointerPointer(new Pointer[]{null}));
        } else {
            throw new ND4JIllegalStateException("Unknown dataType: " + ret.data().dataType());
        }

        AllocationPoint point = allocator.getAllocationPoint(ret);


        nativeOps.memcpyAsync(point.getDevicePointer(), point.getHostPointer(), ret.lengthLong() * Nd4j.sizeOfDataType(ret.data().dataType()), CudaConstants.cudaMemcpyHostToDevice, context.getSpecialStream());
        context.getSpecialStream().synchronize();

        point.tickHostRead();
        point.tickDeviceWrite();

        return ret;
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
        if (Nd4j.getExecutioner() instanceof GridExecutioner)
            ((GridExecutioner) Nd4j.getExecutioner()).flushQueue();

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

        AtomicAllocator allocator = AtomicAllocator.getInstance();
        CudaContext context = allocator.getFlowController().prepareAction(ret, source);

        Pointer x = AtomicAllocator.getInstance().getPointer(source, context);
        Pointer xShape = AtomicAllocator.getInstance().getPointer(source.shapeInfoDataBuffer(), context);
        Pointer z = AtomicAllocator.getInstance().getPointer(ret, context);
        Pointer zShape = AtomicAllocator.getInstance().getPointer(ret.shapeInfoDataBuffer(), context);

        PointerPointer extras = new PointerPointer(AddressRetriever.retrieveHostPointer(ret.shapeInfoDataBuffer()),
                        context.getOldStream(), allocator.getDeviceIdPointer());

        CudaIntDataBuffer tempIndexes = new CudaIntDataBuffer(indexes.length);
        AtomicAllocator.getInstance().memcpyBlocking(tempIndexes, new IntPointer(indexes), indexes.length * 4, 0);

        Pointer pIndex = AtomicAllocator.getInstance().getPointer(tempIndexes, context);

        TADManager tadManager = Nd4j.getExecutioner().getTADManager();

        Pair<DataBuffer, DataBuffer> tadBuffers = tadManager.getTADOnlyShapeInfo(source, new int[] {sourceDimension});
        Pair<DataBuffer, DataBuffer> zTadBuffers = tadManager.getTADOnlyShapeInfo(ret, new int[] {sourceDimension});

        Pointer tadShapeInfo = AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context);
        Pointer zTadShapeInfo = AtomicAllocator.getInstance().getPointer(zTadBuffers.getFirst(), context);

        DataBuffer offsets = tadBuffers.getSecond();
        Pointer tadOffsets = AtomicAllocator.getInstance().getPointer(offsets, context);

        Pointer zTadOffsets = AtomicAllocator.getInstance().getPointer(zTadBuffers.getSecond(), context);

        if (ret.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.pullRowsDouble(extras, (DoublePointer) x, (IntPointer) xShape, (DoublePointer) z,
                            (IntPointer) zShape, indexes.length, (IntPointer) pIndex, (IntPointer) tadShapeInfo,
                            new LongPointerWrapper(tadOffsets), (IntPointer) zTadShapeInfo, new LongPointerWrapper(zTadOffsets));
        } else if (ret.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.pullRowsFloat(extras, (FloatPointer) x, (IntPointer) xShape, (FloatPointer) z,
                            (IntPointer) zShape, indexes.length, (IntPointer) pIndex, (IntPointer) tadShapeInfo,
                        new LongPointerWrapper(tadOffsets), (IntPointer) zTadShapeInfo, new LongPointerWrapper(zTadOffsets));
        } else {
            nativeOps.pullRowsHalf(extras, (ShortPointer) x, (IntPointer) xShape, (ShortPointer) z, (IntPointer) zShape,
                            indexes.length, (IntPointer) pIndex, (IntPointer) tadShapeInfo, new LongPointerWrapper(tadOffsets),
                            (IntPointer) zTadShapeInfo, new LongPointerWrapper(zTadOffsets));
        }

        allocator.registerAction(context, ret, source);

        return ret;
    }

    public INDArray accumulate(INDArray target, INDArray... arrays) {
        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        if (arrays.length == 1)
            return target.assign(arrays[0]);

        // we do averaging on GPU only if ALL devices have p2p links
        if (CudaEnvironment.getInstance().getConfiguration().isCrossDeviceAccessAllowed() && nativeOps.isP2PAvailable()) {
            Nd4j.getExecutioner().push();

            long len = target.lengthLong();

            AtomicAllocator allocator = AtomicAllocator.getInstance();

            CudaContext context = allocator.getFlowController().prepareAction(target, arrays);

            PointerPointer extras = new PointerPointer(null, // not used
                    context.getOldStream(), allocator.getDeviceIdPointer(), new CudaPointer(0));


            Pointer z = AtomicAllocator.getInstance().getPointer(target, context);

            long[] xPointers = new long[arrays.length];

            for (int i = 0; i < arrays.length; i++) {
                if (arrays[i].elementWiseStride() != 1)
                    throw new ND4JIllegalStateException("Native averaging is applicable only to continuous INDArrays");

                if (arrays[i].lengthLong() != len)
                    throw new ND4JIllegalStateException("All arrays should have equal length for averaging");

                AllocationPoint point = allocator.getAllocationPoint(arrays[i]);
                xPointers[i] = point.getPointers().getDevicePointer().address();
                point.tickDeviceWrite();
            }

            CudaDoubleDataBuffer tempX = new CudaDoubleDataBuffer(arrays.length);

            allocator.memcpyBlocking(tempX, new LongPointer(xPointers), xPointers.length * 8, 0);

            PointerPointer x = new PointerPointer(AtomicAllocator.getInstance().getPointer(tempX, context));

            if (target.data().dataType() == DataBuffer.Type.DOUBLE) {
                nativeOps.accumulateDouble(extras, x, (DoublePointer) z, arrays.length, len);
            } else if (target.data().dataType() == DataBuffer.Type.FLOAT) {
                nativeOps.accumulateFloat(extras, x, (FloatPointer) z, arrays.length, len);
            } else {
                nativeOps.accumulateHalf(extras, x, (ShortPointer) z, arrays.length, len);
            }

            allocator.getFlowController().registerAction(context, target, arrays);

            tempX.address();

            return target;
        } else {
            long len = target.lengthLong();

            Nd4j.getExecutioner().commit();

            CudaContext context = (CudaContext) AtomicAllocator.getInstance().getDeviceContext().getContext();

            PointerPointer dataPointers = new PointerPointer(arrays.length);
            PointerPointer extras = new PointerPointer(null, // not used
                    context.getOldStream(), AtomicAllocator.getInstance().getDeviceIdPointer(), new CudaPointer(1) );

            for (int i = 0; i < arrays.length; i++) {
                Nd4j.getCompressor().autoDecompress(arrays[i]);

                if (arrays[i].elementWiseStride() != 1)
                    throw new ND4JIllegalStateException("Native averaging is applicable only to continuous INDArrays");

                if (arrays[i].lengthLong() != len)
                    throw new ND4JIllegalStateException("All arrays should have equal length for averaging");

                dataPointers.put(i, AtomicAllocator.getInstance().getHostPointer(arrays[i]));
            }

            if (target.data().dataType() == DataBuffer.Type.DOUBLE) {
                nativeOps.accumulateDouble(extras, dataPointers, (DoublePointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len);
            } else if (target.data().dataType() == DataBuffer.Type.FLOAT) {
                nativeOps.accumulateFloat(extras, dataPointers, (FloatPointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len);
            } else {
                nativeOps.accumulateHalf(extras, dataPointers, (ShortPointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len);
            }

            AtomicAllocator.getInstance().getAllocationPoint(target).tickHostWrite();



            return target;
        }

    }

    @Override
    public INDArray average(INDArray target, INDArray[] arrays) {
        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        if (arrays.length == 1)
            return target.assign(arrays[0]);

        // we do averaging on GPU only if ALL devices have p2p links
        if (nativeOps.isP2PAvailable() && CudaEnvironment.getInstance().getConfiguration().isCrossDeviceAccessAllowed()) {

            Nd4j.getExecutioner().push();

            long len = target != null ? target.lengthLong() : arrays[0].lengthLong();

            AtomicAllocator allocator = AtomicAllocator.getInstance();

            CudaContext context = allocator.getFlowController().prepareAction(target, arrays);

            PointerPointer extras = new PointerPointer(null, // not used
                    context.getOldStream(), allocator.getDeviceIdPointer(), new CudaPointer(0));


            Pointer z = target == null ? null : AtomicAllocator.getInstance().getPointer(target, context);

            long[] xPointers = new long[arrays.length];

            for (int i = 0; i < arrays.length; i++) {
                if (arrays[i].elementWiseStride() != 1)
                    throw new ND4JIllegalStateException("Native averaging is applicable only to continuous INDArrays");

                if (arrays[i].lengthLong() != len)
                    throw new ND4JIllegalStateException("All arrays should have equal length for averaging");

                AllocationPoint point = allocator.getAllocationPoint(arrays[i]);
                xPointers[i] = point.getPointers().getDevicePointer().address();
                point.tickDeviceWrite();
            }

            CudaDoubleDataBuffer tempX = new CudaDoubleDataBuffer(arrays.length);

            allocator.memcpyBlocking(tempX, new LongPointer(xPointers), xPointers.length * 8, 0);

            PointerPointer x = new PointerPointer(AtomicAllocator.getInstance().getPointer(tempX, context));

            if (arrays[0].data().dataType() == DataBuffer.Type.DOUBLE) {
                nativeOps.averageDouble(extras, x, target == null ? null : (DoublePointer) z, arrays.length, len, true);
            } else if (arrays[0].data().dataType() == DataBuffer.Type.FLOAT) {
                nativeOps.averageFloat(extras, x, target == null ? null : (FloatPointer) z, arrays.length, len, true);
            } else {
                nativeOps.averageHalf(extras, x, target == null ? null : (ShortPointer) z, arrays.length, len, true);
            }

            allocator.getFlowController().registerAction(context, target, arrays);

            tempX.address();

            return target;
        } else {
            // otherwise we do averging on CPU side
            /**
             * We expect all operations are complete at this point
             */
            long len = target == null ? arrays[0].lengthLong() : target.lengthLong();

            CudaContext context = (CudaContext) AtomicAllocator.getInstance().getDeviceContext().getContext();

            PointerPointer dataPointers = new PointerPointer(arrays.length);
            PointerPointer extras = new PointerPointer(null, // not used
                    context.getOldStream(), AtomicAllocator.getInstance().getDeviceIdPointer(), new CudaPointer(1) );

            for (int i = 0; i < arrays.length; i++) {
                Nd4j.getCompressor().autoDecompress(arrays[i]);

                if (arrays[i].elementWiseStride() != 1)
                    throw new ND4JIllegalStateException("Native averaging is applicable only to continuous INDArrays");

                if (arrays[i].lengthLong() != len)
                    throw new ND4JIllegalStateException("All arrays should have equal length for averaging");

                dataPointers.put(i, AtomicAllocator.getInstance().getHostPointer(arrays[i]));
            }

            if (arrays[0].data().dataType() == DataBuffer.Type.DOUBLE) {
                nativeOps.averageDouble(extras, dataPointers, target == null ? null : (DoublePointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len, true);
            } else if (arrays[0].data().dataType() == DataBuffer.Type.FLOAT) {
                nativeOps.averageFloat(extras, dataPointers, target == null ? null :  (FloatPointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len, true);
            } else {
                nativeOps.averageHalf(extras, dataPointers, target == null ? null : (ShortPointer) AtomicAllocator.getInstance().getHostPointer(target), arrays.length, len, true);
            }

            if (target != null)
                AtomicAllocator.getInstance().getAllocationPoint(target).tickHostWrite();

            // TODO: make propagation optional maybe?
            if (true) {
                for (int i = 0; i < arrays.length; i++) {
                    AtomicAllocator.getInstance().getAllocationPoint(arrays[i]).tickHostWrite();
                }
            }

            return target;
        }
    }

    @Override
    public INDArray average(Collection<INDArray> arrays) {
        return average(arrays.toArray(new INDArray[0]));
    }


    /**
     * This method averages input arrays, and returns averaged array
     *
     * @param arrays
     * @return
     */
    @Override
    public INDArray average(INDArray[] arrays) {
        if (arrays == null || arrays.length == 0)
            throw new RuntimeException("Input arrays are missing");

        // we assume all arrays have equal length,
        INDArray ret = Nd4j.createUninitialized(arrays[0].shape(), arrays[0].ordering());

        return average(ret, arrays);
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
     * along a specified set of dimensions. Each array in list should have it's own dimension at the same index of dimensions array
     *
     * @param arrays      the ndarrays to shuffle
     * @param dimensions the dimensions to do the shuffle
     * @return
     */
    @Override
    public void shuffle(List<INDArray> arrays, Random rnd, List<int[]> dimensions) {
        // no dimension - no shuffle
        if (dimensions == null || dimensions.size() == 0)
            throw new RuntimeException("Dimension can't be null or 0-length");

        if (arrays == null || arrays.size() == 0)
            throw new RuntimeException("No input arrays provided");

        if (dimensions.size() > 1 && arrays.size() != dimensions.size())
            throw new IllegalStateException("Number of dimensions do not match number of arrays to shuffle");

        Nd4j.getExecutioner().push();

        // first we build TAD for input array and dimensions

        AtomicAllocator allocator = AtomicAllocator.getInstance();

        CudaContext context = null;

        for (int x = 0; x < arrays.size(); x++) {
            context = allocator.getFlowController().prepareAction(arrays.get(x));
        }

        int tadLength = 1;
        for (int i = 0; i < dimensions.get(0).length; i++) {
            tadLength *= arrays.get(0).shape()[dimensions.get(0)[i]];
        }

        int numTads = arrays.get(0).length() / tadLength;

        int[] map = ArrayUtil.buildInterleavedVector(rnd, numTads);

        CudaIntDataBuffer shuffle = new CudaIntDataBuffer(map);

        Pointer shuffleMap = allocator.getPointer(shuffle, context);

        PointerPointer extras = new PointerPointer(null, // not used
                        context.getOldStream(), allocator.getDeviceIdPointer());


        long[] xPointers = new long[arrays.size()];
        long[] xShapes = new long[arrays.size()];
        long[] tadShapes = new long[arrays.size()];
        long[] tadOffsets = new long[arrays.size()];

        for (int i = 0; i < arrays.size(); i++) {
            INDArray array = arrays.get(i);

            Pointer x = AtomicAllocator.getInstance().getPointer(array, context);
            Pointer xShapeInfo = AtomicAllocator.getInstance().getPointer(array.shapeInfoDataBuffer(), context);


            TADManager tadManager = Nd4j.getExecutioner().getTADManager();

            int[] dimension = dimensions.size() > 1 ? dimensions.get(i) : dimensions.get(0);

            Pair<DataBuffer, DataBuffer> tadBuffers = tadManager.getTADOnlyShapeInfo(array, dimension);

            Pointer tadShapeInfo = AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context);

            DataBuffer offsets = tadBuffers.getSecond();

            if (offsets.length() != numTads)
                throw new ND4JIllegalStateException("Can't symmetrically shuffle arrays with non-equal number of TADs");

            Pointer tadOffset = AtomicAllocator.getInstance().getPointer(offsets, context);

            xPointers[i] = x.address();
            xShapes[i] = xShapeInfo.address();
            tadShapes[i] = tadShapeInfo.address();
            tadOffsets[i] = tadOffset.address();
        }


        CudaDoubleDataBuffer tempX = new CudaDoubleDataBuffer(arrays.size());
        CudaDoubleDataBuffer tempShapes = new CudaDoubleDataBuffer(arrays.size());
        CudaDoubleDataBuffer tempTAD = new CudaDoubleDataBuffer(arrays.size());
        CudaDoubleDataBuffer tempOffsets = new CudaDoubleDataBuffer(arrays.size());

        AtomicAllocator.getInstance().memcpyBlocking(tempX, new LongPointer(xPointers), xPointers.length * 8, 0);
        AtomicAllocator.getInstance().memcpyBlocking(tempShapes, new LongPointer(xShapes), xPointers.length * 8, 0);
        AtomicAllocator.getInstance().memcpyBlocking(tempTAD, new LongPointer(tadShapes), xPointers.length * 8, 0);
        AtomicAllocator.getInstance().memcpyBlocking(tempOffsets, new LongPointer(tadOffsets), xPointers.length * 8, 0);


        if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.shuffleDouble(extras, new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)),
                            new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)), arrays.size(),
                            (IntPointer) shuffleMap, new PointerPointer(allocator.getPointer(tempTAD, context)),
                            new PointerPointer(allocator.getPointer(tempOffsets, context)));
        } else if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.shuffleFloat(extras, new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)),
                            new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)), arrays.size(),
                            (IntPointer) shuffleMap, new PointerPointer(allocator.getPointer(tempTAD, context)),
                            new PointerPointer(allocator.getPointer(tempOffsets, context)));
        } else {
            // HALFs
            nativeOps.shuffleHalf(extras, new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)),
                            new PointerPointer(allocator.getPointer(tempX, context)),
                            new PointerPointer(allocator.getPointer(tempShapes, context)), arrays.size(),
                            (IntPointer) shuffleMap, new PointerPointer(allocator.getPointer(tempTAD, context)),
                            new PointerPointer(allocator.getPointer(tempOffsets, context)));
        }


        for (int f = 0; f < arrays.size(); f++) {
            allocator.getFlowController().registerAction(context, arrays.get(f));
        }


        // just to keep reference
        shuffle.address();

        tempX.dataType();
        tempShapes.dataType();
        tempOffsets.dataType();
        tempTAD.dataType();
    }

    /**
     * Symmetric in place shuffle of an ndarray
     * along a specified set of dimensions. All arrays
     *
     * @param sourceArrays     the ndarray to shuffle
     * @param dimension the dimension to do the shuffle
     * @return
     */
    @Override
    public void shuffle(Collection<INDArray> sourceArrays, Random rnd, int... dimension) {
        shuffle(new ArrayList<INDArray>(sourceArrays), rnd, Collections.singletonList(dimension));
    }

    /*
    public DataBuffer convertToHalfs(DataBuffer buffer) {
        DataBuffer halfsBuffer = new CudaHalfDataBuffer(buffer.length());
    
        AtomicAllocator allocator = AtomicAllocator.getInstance();
    
        AllocationPoint pointSrc = allocator.getAllocationPoint(buffer);
        AllocationPoint pointDst = allocator.getAllocationPoint(halfsBuffer);
    
        CudaContext context =  allocator.getFlowController().prepareAction(pointDst, pointSrc);
    
        PointerPointer extras = new PointerPointer(
                null, // not used for conversion
                context.getOldStream(),
                AtomicAllocator.getInstance().getDeviceIdPointer());
    
        Pointer x = AtomicAllocator.getInstance().getPointer(buffer, context);
        Pointer z = AtomicAllocator.getInstance().getPointer(halfsBuffer, context);
    
        if (buffer.dataType() == DataBuffer.Type.FLOAT) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().convertFloatsToHalfs(extras, x, (int) buffer.length(), z);
            pointDst.tickDeviceWrite();
        } else if (buffer.dataType() == DataBuffer.Type.DOUBLE) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().convertDoublesToHalfs(extras, x, (int) buffer.length(), z);
            pointDst.tickDeviceWrite();
        } else if (buffer.dataType() == DataBuffer.Type.HALF) {
            log.info("Buffer is already HALF-precision");
            return buffer;
        } else {
            throw new UnsupportedOperationException("Conversion INT->HALF isn't supported yet.");
        }
    
        allocator.getFlowController().registerAction(context, pointDst, pointSrc);
    
        return halfsBuffer;
    }
    
    public DataBuffer restoreFromHalfs(DataBuffer buffer) {
        if (buffer.dataType() != DataBuffer.Type.HALF)
            throw new IllegalStateException("Input DataBuffer should contain Halfs");
    
        DataBuffer outputBuffer = null;
    
    
    
        if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            outputBuffer = new CudaFloatDataBuffer(buffer.length());
    
        } else if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            outputBuffer = new CudaDoubleDataBuffer(buffer.length());
    
        } else throw new UnsupportedOperationException("DataType ["+Nd4j.dataType()+"] isn't supported yet");
    
        AtomicAllocator allocator = AtomicAllocator.getInstance();
    
        AllocationPoint pointSrc = allocator.getAllocationPoint(buffer);
        AllocationPoint pointDst = allocator.getAllocationPoint(outputBuffer);
    
        CudaContext context =  allocator.getFlowController().prepareAction(pointDst, pointSrc);
    
        PointerPointer extras = new PointerPointer(
                null, // not used for conversion
                context.getOldStream(),
                AtomicAllocator.getInstance().getDeviceIdPointer());
    
        Pointer x = AtomicAllocator.getInstance().getPointer(buffer, context);
        Pointer z = AtomicAllocator.getInstance().getPointer(outputBuffer, context);
    
        if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().convertHalfsToFloats(extras, x, (int) buffer.length(), z);
            pointDst.tickDeviceWrite();
        } else if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().convertHalfsToDoubles(extras, x, (int) buffer.length(), z);
            pointDst.tickDeviceWrite();
        } else if (Nd4j.dataType() == DataBuffer.Type.HALF) {
            log.info("Buffer is already HALF-precision");
            return buffer;
        }
    
        allocator.getFlowController().registerAction(context, pointDst, pointSrc);
    
        return outputBuffer;
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
    public void convertDataEx(DataBuffer.TypeEx typeSrc, Pointer source, DataBuffer.TypeEx typeDst, Pointer target,
                    long length) {
        nativeOps.convertTypes(null, typeSrc.ordinal(), source, length, typeDst.ordinal(), target);
    }



    @Override
    public void convertDataEx(DataBuffer.TypeEx typeSrc, DataBuffer source, DataBuffer.TypeEx typeDst,
                    DataBuffer target) {
        convertDataEx(typeSrc, source.addressPointer(), typeDst, target.addressPointer(), target.length());
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

        // flushQueue should be blocking here, because typeConversion happens on cpu side
        Nd4j.getExecutioner().commit();

        DataBuffer buffer = null;

        if (!(source instanceof CompressedDataBuffer))
            AtomicAllocator.getInstance().synchronizeHostData(source);

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
            buffer = Nd4j.createBuffer(descriptor.getNumberOfElements(), false);

            AllocationPoint point = AtomicAllocator.getInstance().getAllocationPoint(buffer);
            point.tickHostWrite();
        }

        convertDataEx(typeSrc, source, typeDst, buffer);

        return buffer;
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

        DataBuffer shapeBuffer = Nd4j.createBuffer(shapeBufferPointer, DataBuffer.Type.INT,length, IntIndexer.create(intPointer));

        dataPointer.position(0);
        dataPointer.limit(dataBufferElementSize * Shape.length(shapeBuffer));
        dataPointer.capacity(dataBufferElementSize * Shape.length(shapeBuffer));

        // we don't care about pointers here, they will be copied in BaseCudaDataBuffer method, and indexer will be recreated
        if(dataBufferElementSize == (Float.SIZE / 8)) {
            data = Nd4j.createBuffer(dataPointer,
                    DataBuffer.Type.FLOAT,
                    Shape.length(shapeBuffer),
                    FloatIndexer.create(new FloatPointer(dataPointer)));
        }
        else if(dataBufferElementSize == (Double.SIZE / 8)) {
            data = Nd4j.createBuffer(dataPointer,
                    DataBuffer.Type.DOUBLE,
                    Shape.length(shapeBuffer),
                    DoubleIndexer.create(new DoublePointer(dataPointer)));
        }

        INDArray ret = Nd4j.create(data,Shape.shape(shapeBuffer),
                Shape.strideArr(shapeBuffer),Shape.offset(shapeBuffer),Shape.order(shapeBuffer));
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
        /*Pointer pointer = nativeOps.numpyFromFile(new BytePointer(file.getAbsolutePath().getBytes()));
        log.info("Pointer here: {}", pointer.address());
        return createFromNpyPointer(pointer);

        */

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

        long[] xPointers = new long[numTads];

        CudaContext context = AtomicAllocator.getInstance().getFlowController().prepareAction(null, tensor);

        for (int x = 0; x < numTads; x++) {
            result[x] = Nd4j.createUninitialized(shape);

            context = AtomicAllocator.getInstance().getFlowController().prepareAction(result[x]);

            xPointers[x] = AtomicAllocator.getInstance().getPointer(result[x], context).address();
        }

        CudaDoubleDataBuffer tempX = new CudaDoubleDataBuffer(numTads);

        AtomicAllocator.getInstance().memcpyBlocking(tempX, new LongPointer(xPointers), xPointers.length * 8, 0);

        PointerPointer extraz = new PointerPointer(null, // not used
                context.getOldStream(), AtomicAllocator.getInstance().getDeviceIdPointer());

        if (Nd4j.dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.tearDouble(extraz,
                    (DoublePointer) AtomicAllocator.getInstance().getPointer(tensor, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tensor.shapeInfoDataBuffer(), context),
                    new PointerPointer(AtomicAllocator.getInstance().getPointer(tempX, context)),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(result[0].shapeInfoDataBuffer(), context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper((IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context))
            );
        } else if (Nd4j.dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.tearFloat(extraz,
                    (FloatPointer) AtomicAllocator.getInstance().getPointer(tensor, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tensor.shapeInfoDataBuffer(), context),
                    new PointerPointer(AtomicAllocator.getInstance().getPointer(tempX, context)),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(result[0].shapeInfoDataBuffer(), context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper(AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context))
            );
        } else if (Nd4j.dataType() == DataBuffer.Type.HALF) {
            nativeOps.tearHalf(extraz,
                    (ShortPointer) AtomicAllocator.getInstance().getPointer(tensor, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tensor.shapeInfoDataBuffer(), context),
                    new PointerPointer(AtomicAllocator.getInstance().getPointer(tempX, context)),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(result[0].shapeInfoDataBuffer(), context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper(AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context))
            );
        }

        AtomicAllocator.getInstance().getFlowController().registerActionAllWrite(context, result);
        AtomicAllocator.getInstance().getFlowController().registerAction(context,null, result);

        return result;
    }


    @Override
    public INDArray sort(INDArray x, boolean descending) {
        if (x.isScalar())
            return x;

        Nd4j.getExecutioner().push();

        CudaContext context = AtomicAllocator.getInstance().getFlowController().prepareAction(x);

        Pointer ptr = AtomicAllocator.getInstance().getHostPointer(x.shapeInfoDataBuffer());

        PointerPointer extraz = new PointerPointer(ptr, // 0
                context.getOldStream(), // 1
                AtomicAllocator.getInstance().getDeviceIdPointer(), // 2
                context.getBufferAllocation(), // 3
                context.getBufferReduction(), // 4
                context.getBufferScalar(), // 5
                context.getBufferSpecial(), // 6
                ptr, // 7
                AtomicAllocator.getInstance().getHostPointer(x.shapeInfoDataBuffer()), // 8
                ptr, // 9
                ptr, // 10
                ptr, // 11
                ptr, // 12
                ptr, // 13
                ptr, // 14
                ptr, // special pointer for IsMax  // 15
                ptr, // special pointer for IsMax  // 16
                ptr, // special pointer for IsMax // 17
                new CudaPointer(0));

        // we're sending > 10m elements to radixSort
        boolean isRadix = !x.isView() && (x.lengthLong() > 1024 * 1024 * 10);
        INDArray tmpX = x;

        // we need to guarantee all threads are finished here
        if (isRadix)
            Nd4j.getExecutioner().commit();

        if (x.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.sortFloat(extraz,
                    (FloatPointer) AtomicAllocator.getInstance().getPointer(tmpX, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tmpX.shapeInfoDataBuffer(), context),
                    descending
                    );
        } else if (x.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.sortDouble(extraz,
                    (DoublePointer) AtomicAllocator.getInstance().getPointer(tmpX, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tmpX.shapeInfoDataBuffer(), context),
                    descending
            );
        } else if (x.data().dataType() == DataBuffer.Type.HALF) {
            nativeOps.sortHalf(extraz,
                    (ShortPointer) AtomicAllocator.getInstance().getPointer(tmpX, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tmpX.shapeInfoDataBuffer(), context),
                    descending
            );
        } else {
            throw new UnsupportedOperationException("Unknown dataType " + x.data().dataType());
        }

        AtomicAllocator.getInstance().getFlowController().registerAction(context, x);

        return x;
    }

    @Override
    public INDArray sort(INDArray x, boolean descending, int... dimension) {
        if (x.isScalar())
            return x;

        Arrays.sort(dimension);

        Nd4j.getExecutioner().push();

        Pair<DataBuffer, DataBuffer> tadBuffers = Nd4j.getExecutioner().getTADManager().getTADOnlyShapeInfo(x, dimension);

        CudaContext context = AtomicAllocator.getInstance().getFlowController().prepareAction(x);

        PointerPointer extraz = new PointerPointer(AtomicAllocator.getInstance().getHostPointer(x.shapeInfoDataBuffer()), // not used
                context.getOldStream(), AtomicAllocator.getInstance().getDeviceIdPointer());


        Pointer dimensionPointer = AtomicAllocator.getInstance()
                .getPointer(AtomicAllocator.getInstance().getConstantBuffer(dimension), context);

        if (x.data().dataType() == DataBuffer.Type.FLOAT) {
            nativeOps.sortTadFloat(extraz,
                    (FloatPointer) AtomicAllocator.getInstance().getPointer(x, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(x.shapeInfoDataBuffer(), context),
                    (IntPointer) dimensionPointer,
                    dimension.length,
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper(AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context)),
                    descending
            );
        } else if (x.data().dataType() == DataBuffer.Type.DOUBLE) {
            nativeOps.sortTadDouble(extraz,
                    (DoublePointer) AtomicAllocator.getInstance().getPointer(x, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(x.shapeInfoDataBuffer(), context),
                    (IntPointer) dimensionPointer,
                    dimension.length,
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper(AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context)),
                    descending
            );
        } else if (x.data().dataType() == DataBuffer.Type.HALF) {
            nativeOps.sortTadHalf(extraz,
                    (ShortPointer) AtomicAllocator.getInstance().getPointer(x, context),
                    (IntPointer) AtomicAllocator.getInstance().getPointer(x.shapeInfoDataBuffer(), context),
                    (IntPointer) dimensionPointer,
                    dimension.length,
                    (IntPointer) AtomicAllocator.getInstance().getPointer(tadBuffers.getFirst(), context),
                    new LongPointerWrapper(AtomicAllocator.getInstance().getPointer(tadBuffers.getSecond(), context)),
                    descending
            );
        } else {
            throw new UnsupportedOperationException("Unknown dataType " + x.data().dataType());
        }

        AtomicAllocator.getInstance().getFlowController().registerAction(context, x);

        return x;
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
    public INDArray sortCooIndices(INDArray x) {
        throw new UnsupportedOperationException();
    }
}
