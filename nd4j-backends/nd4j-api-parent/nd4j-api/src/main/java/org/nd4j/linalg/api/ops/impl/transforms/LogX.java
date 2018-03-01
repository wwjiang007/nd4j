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

import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseTransformOp;

import java.util.List;

/**
 * Log on arbitrary base op
 *
 * @author raver119@gmail.com
 */
public class LogX extends BaseTransformOp {
    private double base;

    public LogX(SameDiff sameDiff, SDVariable i_v, boolean inPlace, double base) {
        super(sameDiff, i_v, inPlace);
        this.base = base;
    }

    public LogX(SameDiff sameDiff, SDVariable i_v, int[] shape, boolean inPlace, Object[] extraArgs, double base) {
        super(sameDiff, i_v, shape, inPlace, extraArgs);
        this.base = base;
    }

    public LogX(SameDiff sameDiff, SDVariable i_v, Object[] extraArgs, double base) {
        super(sameDiff, i_v, extraArgs);
        this.base = base;
    }

    public LogX() {}

    public LogX(INDArray x, INDArray z, double base) {
        super(x, z);
        this.base = base;
        this.extraArgs = new Object[] {base};
    }

    public LogX(INDArray x, INDArray z, double base, long n) {
        super(x, z, n);
        this.base = base;
        this.extraArgs = new Object[] {base};
    }

    public LogX(INDArray x, INDArray y, INDArray z, double base, long n) {
        super(x, y, z, n);
        this.base = base;
        this.extraArgs = new Object[] {base};
    }

    public LogX(INDArray x, double base) {
        super(x);
        this.base = base;
        this.extraArgs = new Object[] {base};
    }

    @Override
    public int opNum() {
        return 55;
    }

    @Override
    public String opName() {
        return "log_x";
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        return null;
    }

    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " +  opName());
    }


    @Override
    public String tensorflowName() {
        return "Log1p";
    }
}
