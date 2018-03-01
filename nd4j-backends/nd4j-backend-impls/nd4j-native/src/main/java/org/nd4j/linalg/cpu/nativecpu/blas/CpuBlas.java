package org.nd4j.linalg.cpu.nativecpu.blas;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.mkl_rt;
import org.nd4j.nativeblas.Nd4jBlas;

import static org.bytedeco.javacpp.openblas.*;

/**
 * Implementation of Nd4jBlas with OpenBLAS/MKL
 *
 * @author saudet
 */
@Slf4j
public class CpuBlas extends Nd4jBlas {

    /**
     * Converts a character
     * to its proper enum
     * for row (c) or column (f) ordering
     * default is row major
     */
    static int convertOrder(int from) {
        switch (from) {
            case 'c':
            case 'C':
                return CblasRowMajor;
            case 'f':
            case 'F':
                return CblasColMajor;
            default:
                return CblasColMajor;
        }
    }

    /**
     * Converts a character to its proper enum
     * t -> transpose
     * n -> no transpose
     * c -> conj
     */
    static int convertTranspose(int from) {
        switch (from) {
            case 't':
            case 'T':
                return CblasTrans;
            case 'n':
            case 'N':
                return CblasNoTrans;
            case 'c':
            case 'C':
                return CblasConjTrans;
            default:
                return CblasNoTrans;
        }
    }

    /**
     * Upper or lower
     * U/u -> upper
     * L/l -> lower
     *
     * Default is upper
     */
    static int convertUplo(int from) {
        switch (from) {
            case 'u':
            case 'U':
                return CblasUpper;
            case 'l':
            case 'L':
                return CblasLower;
            default:
                return CblasUpper;
        }
    }


    /**
     * For diagonals:
     * u/U -> unit
     * n/N -> non unit
     *
     * Default: unit
     */
    static int convertDiag(int from) {
        switch (from) {
            case 'u':
            case 'U':
                return CblasUnit;
            case 'n':
            case 'N':
                return CblasNonUnit;
            default:
                return CblasUnit;
        }
    }

    /**
     * Side of a matrix, left or right
     * l /L -> left
     * r/R -> right
     * default: left
     */
    static int convertSide(int from) {
        switch (from) {
            case 'l':
            case 'L':
                return CblasLeft;
            case 'r':
            case 'R':
                return CblasRight;
            default:
                return CblasLeft;
        }
    }

    @Override
    public void setMaxThreads(int num) {
        try {
            // this is required to work around some loading issue with MKL under Linux
            mkl_rt.MKL_Set_Num_Threads(num);
            //mkl_rt.MKL_Domain_Set_Num_Threads(num);
            mkl_rt.MKL_Set_Num_Threads_Local(num);
        } catch (UnsatisfiedLinkError e) {
            log.trace("Could not load MKL", e);
        } catch (NoClassDefFoundError e) {
            log.trace("Could not load MKL", e);
        }
        blas_set_num_threads(num);
    }

    @Override
    public int getMaxThreads() {
        return blas_get_num_threads();
    }

    @Override
    public int getBlasVendorId() {
        return blas_get_vendor();
    }
}
