package org.nd4j.linalg.api.blas.params;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class MMulTranspose implements Serializable {
    private boolean transposeA = false;
    private boolean transposeB = false;
    private boolean transposeResult = false;
    private INDArray a,b;
    private static MMulTranspose allFalse = MMulTranspose.builder().build();


    /**
     * Returns the default transpose
     * where all are false
     * @return
     */
    public static MMulTranspose allFalse() {
        return allFalse;
    }



    @Builder
    public MMulTranspose(boolean transposeA,
                         boolean transposeB,
                         boolean transposeResult,
                         INDArray a,
                         INDArray b) {
        this.transposeA = transposeA;
        this.transposeB = transposeB;
        this.transposeResult = transposeResult;
        if(transposeResult) {
            this.transposeA = !transposeA;
            this.transposeB = !transposeB;
            this.a = b;
            this.b = a;
        }

        this.transposeA = transposeA;
        this.transposeB = transposeB;

        if(this.transposeA && a != null)
            this.a = a.transpose();
        else
            this.a = a;
        if(this.transposeB && b != null)
            this.b = b.transpose();
        else
            this.b = b;
    }
}
