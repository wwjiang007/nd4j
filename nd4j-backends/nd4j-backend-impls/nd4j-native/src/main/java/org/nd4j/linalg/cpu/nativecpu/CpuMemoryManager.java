package org.nd4j.linalg.cpu.nativecpu;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.memory.BasicMemoryManager;
import org.nd4j.linalg.api.memory.enums.MemoryKind;
import org.nd4j.nativeblas.NativeOpsHolder;

/**
 * @author raver119@gmail.com
 */
@Slf4j
public class CpuMemoryManager extends BasicMemoryManager {
    /**
     * This method returns
     * PLEASE NOTE: Cache options depend on specific implementations
     *
     * @param bytes
     * @param kind
     * @param initialize
     */
    @Override
    public Pointer allocate(long bytes, MemoryKind kind, boolean initialize) {
        Pointer ptr = NativeOpsHolder.getInstance().getDeviceNativeOps().mallocHost(bytes, 0);

        if (ptr == null || ptr.address() == 0L)
            throw new ND4JIllegalStateException("Failed to allocate [" + bytes + "] bytes");

        //log.info("Allocating {} bytes at MemoryManager", bytes);


        if (initialize)
            Pointer.memset(ptr, 0, bytes);

        return ptr;
    }

    /**
     * This method releases previously allocated memory chunk
     *
     * @param pointer
     * @param kind
     * @return
     */
    @Override
    public void release(@NonNull Pointer pointer, MemoryKind kind) {
        Pointer.free(pointer);
    }

    /**
     * This method detaches off-heap memory from passed INDArray instances, and optionally stores them in cache for future reuse
     * PLEASE NOTE: Cache options depend on specific implementations
     *
     * @param arrays
     */
    @Override
    public void collect(INDArray... arrays) {
        super.collect(arrays);
    }

    /**
     * Nd4j-native backend doesn't use periodic GC. This method will always return false.
     *
     * @return
     */
    @Override
    public boolean isPeriodicGcActive() {
        return false;
    }

    @Override
    public void memset(INDArray array) {
        if (array.isView()) {
            array.assign(0.0);
            return;
        }

        Pointer.memset(array.data().addressPointer(), 0, array.data().length() * Nd4j.sizeOfDataType(array.data().dataType()));
    }
}
