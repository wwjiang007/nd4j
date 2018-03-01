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

package org.nd4j.linalg.api.ops.impl.scalar.comparison;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseScalarOp;

import java.util.Collections;
import java.util.List;

/**
 * Return a binary (0 or 1) when greater than a number
 *
 * @author Adam Gibson
 */
public class ScalarEquals extends BaseScalarOp {
    public ScalarEquals() {}

    public ScalarEquals(INDArray x, INDArray y, INDArray z, long n, Number num) {
        super(x, y, z, n, num);
    }

    public ScalarEquals(INDArray x, Number num) {
        super(x, num);
    }



    public ScalarEquals(INDArray x) {
        this(x, 0);
    }

    public ScalarEquals(SameDiff sameDiff, SDVariable i_v, Number scalar) {
        super(sameDiff, i_v, scalar);
    }

    public ScalarEquals(SameDiff sameDiff, SDVariable i_v, Number scalar, boolean inPlace) {
        super(sameDiff, i_v, scalar, inPlace);
    }

    public ScalarEquals(SameDiff sameDiff, SDVariable i_v, Number scalar, boolean inPlace, Object[] extraArgs) {
        super(sameDiff, i_v, scalar, inPlace, extraArgs);
    }

    public ScalarEquals(SameDiff sameDiff, SDVariable i_v, Number scalar, Object[] extraArgs) {
        super(sameDiff, i_v, scalar, extraArgs);
    }

    @Override
    public int opNum() {
        return 9;
    }

    @Override
    public String opName() {
        return "equals_scalar";
    }


    @Override
    public int[] getDimension() {
        return null;
    }

    @Override
    public void setDimension(int... dimension) {

    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        //Not continuously differentiable, but 0 gradient in most places
        return Collections.singletonList(sameDiff.zerosLike(arg()));
    }

    @Override
    public String onnxName() {
        return "Equal";
    }

    @Override
    public String tensorflowName() {
        return "equal";
    }


}
