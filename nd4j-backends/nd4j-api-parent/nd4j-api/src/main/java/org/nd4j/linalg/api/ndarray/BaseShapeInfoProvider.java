package org.nd4j.linalg.api.ndarray;

import org.nd4j.linalg.primitives.Pair;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author raver119@gmail.com
 */
public abstract class BaseShapeInfoProvider implements ShapeInfoProvider {
    protected AtomicLong bytes = new AtomicLong(0);

    /**
     * This method creates shapeInformation buffer, based on shape being passed in
     *
     * @param shape
     * @return
     */
    @Override
    public Pair<DataBuffer, int[]> createShapeInformation(int[] shape) {
        char order = Nd4j.order();

        return createShapeInformation(shape, order);
    }

    /**
     * This method creates shapeInformation buffer, based on shape & order being passed in
     *
     * @param shape
     * @param order
     * @return
     */
    @Override
    public Pair<DataBuffer, int[]> createShapeInformation(int[] shape, char order) {
        int[] stride = Nd4j.getStrides(shape, order);

        // this won't be view, so ews is 1
        int ews = 1;

        return createShapeInformation(shape, stride, 0, ews, order);
    }

    @Override
    public Pair<DataBuffer, int[]> createShapeInformation(int[] shape, int[] stride, long offset, int elementWiseStride,
                    char order) {
        DataBuffer buffer = Shape.createShapeInformation(shape, stride, offset, elementWiseStride, order);
        buffer.setConstant(true);
        return Pair.create(buffer, buffer.asInt());
    }

    @Override
    public long getCachedBytes() {
        return bytes.get();
    }
}
