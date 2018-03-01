package org.nd4j.linalg.jcublas;

import org.nd4j.linalg.primitives.Pair;
import org.nd4j.jita.constant.ProtectedCudaShapeInfoProvider;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.BaseShapeInfoProvider;
import org.nd4j.linalg.api.ndarray.ShapeInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author raver119@gmail.com
 */
public class CachedShapeInfoProvider extends BaseShapeInfoProvider {
    private static Logger logger = LoggerFactory.getLogger(CachedShapeInfoProvider.class);

    protected ShapeInfoProvider provider = ProtectedCudaShapeInfoProvider.getInstance();

    public CachedShapeInfoProvider() {

    }

    @Override
    public Pair<DataBuffer, int[]> createShapeInformation(int[] shape, int[] stride, long offset, int elementWiseStride,
                                                          char order) {
        return provider.createShapeInformation(shape, stride, offset, elementWiseStride, order);
    }

    /**
     * This method forces cache purge, if cache is available for specific implementation
     */
    @Override
    public void purgeCache() {
        provider.purgeCache();
    }
}
