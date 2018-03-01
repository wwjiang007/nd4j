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

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseTransformOp;

import java.util.Collections;
import java.util.List;

/**
 * Hard tanh elementwise function
 *
 * @author Adam Gibson
 */
public class HardTanh extends BaseTransformOp {
    public HardTanh(SameDiff sameDiff, SDVariable i_v, boolean inPlace) {
        super(sameDiff, i_v, inPlace);
    }

    public HardTanh(SameDiff sameDiff, SDVariable i_v, int[] shape, boolean inPlace, Object[] extraArgs) {
        super(sameDiff, i_v, shape, inPlace, extraArgs);
    }

    public HardTanh(SameDiff sameDiff, SDVariable i_v, Object[] extraArgs) {
        super(sameDiff, i_v, extraArgs);
    }

    public HardTanh() {}

    public HardTanh(INDArray x, INDArray z) {
        super(x, z);
    }

    public HardTanh(INDArray x, INDArray z, long n) {
        super(x, z, n);
    }

    public HardTanh(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
    }

    public HardTanh(INDArray x) {
        super(x);
    }

    @Override
    public int opNum() {
        return 19;
    }

    @Override
    public String opName() {
        return "hardtanh";
    }


    @Override
    public String onnxName() {
        return "HardTanh";
    }

    @Override
    public String tensorflowName() {
        return "HardTanh";
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v) {
        SDVariable ret = f().hardTanhDerivative(arg()).mul(i_v.get(0));
        return Collections.singletonList(ret);
    }
}
