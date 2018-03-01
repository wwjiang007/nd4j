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

package org.nd4j.linalg.api.ops.impl.accum;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseAccumulation;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Collections;
import java.util.List;

/**
 * Sum of absolute values
 *
 * @author Adam Gibson
 */
public class Norm1 extends BaseAccumulation {
    public Norm1(SameDiff sameDiff, SDVariable i_v, int[] dimensions) {
        super(sameDiff, i_v, dimensions);
    }

    public Norm1(SameDiff sameDiff, SDVariable i_v, SDVariable i_v2, int[] dimensions) {
        super(sameDiff, i_v, i_v2, dimensions);
    }

    public Norm1() {}

    public Norm1(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
    }

    public Norm1(INDArray x, INDArray y, long n) {
        super(x, y, n);
    }

    public Norm1(INDArray x) {
        super(x);
    }

    public Norm1(INDArray x, INDArray y) {
        super(x, y);
    }

    @Override
    public INDArray noOp() {
        return Transforms.abs(x());
    }


    @Override
    public int opNum() {
        return 5;
    }

    @Override
    public String opName() {
        return "norm1";
    }

    @Override
    public String onnxName() {
        return "Norm";
    }

    @Override
    public String tensorflowName() {
        return "norm";
    }


    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v1) {
        //d l1Norm(in)/dx = signum(x)
        SDVariable signum = sameDiff.sign(arg());

        //Note that we need to expand the dimensions of the gradient - auto-broadcast won't work for all cases.
        int origRank = Shape.rankFromShape(arg().getShape());   //TODO shape may not always be defined?
        SDVariable bcGrad = sameDiff.f().reductionBroadcastableWithOrigShape(origRank, dimensions, i_v1.get(0));
        return Collections.singletonList(signum.mul(bcGrad));
    }

    @Override
    public Type getOpType() {
        return Type.REDUCE;
    }
}
