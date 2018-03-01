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

package org.nd4j.linalg.api.ops.impl.transforms;

import lombok.NonNull;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseTransformOp;

import java.util.List;

/**
 * Boolean AND pairwise transform
 *
 * @author raver119@gmail.com
 */
public class All extends BaseTransformOp {

    protected double comparable = 0.0;

    public All(SameDiff sameDiff, SDVariable i_v, boolean inPlace) {
        super(sameDiff, i_v, inPlace);
    }

    public All(SameDiff sameDiff, SDVariable i_v, int[] shape, boolean inPlace, Object[] extraArgs) {
        super(sameDiff, i_v, shape, inPlace, extraArgs);
    }

    public All(SameDiff sameDiff, SDVariable i_v, Object[] extraArgs) {
        super(sameDiff, i_v, extraArgs);
    }

    public All() {}

    public All(@NonNull INDArray x, @NonNull INDArray y) {
        this(x, y, 0.0);
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, Number comparable) {
        this(x, y, x, comparable, x.lengthLong());
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, INDArray z, Number comparable) {
        this(x, y, z, comparable, x.lengthLong());
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, long n) {
        this(x, y, x, n);
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, INDArray z) {
        this(x, y, z, z.lengthLong());
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, INDArray z, long n) {
        this(x, y, z, 0.0, n);
    }

    public All(@NonNull INDArray x, @NonNull INDArray y, INDArray z, Number comparable, long n) {
        super(x, y, z, n);
        this.comparable = comparable.doubleValue();
        this.extraArgs = new Object[] {this.comparable};
    }


    @Override
    public int opNum() {
        return 56;
    }

    @Override
    public String opName() {
        return "boolean_all";
    }



    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        return null;
    }

    @Override
    public String onnxName() {
        return "All";
    }

    @Override
    public String tensorflowName() {
        return "All";
    }
}
