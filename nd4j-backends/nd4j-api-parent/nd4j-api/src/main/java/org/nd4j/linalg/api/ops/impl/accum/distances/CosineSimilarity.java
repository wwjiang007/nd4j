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

package org.nd4j.linalg.api.ops.impl.accum.distances;

import org.nd4j.autodiff.functions.DifferentialFunctionFactory;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseAccumulation;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cosine similarity
 * Note that you need to initialize
 * a scaling constant equal to the norm2 of the
 * vector
 *
 * @author Adam Gibson
 */
public class CosineSimilarity extends BaseAccumulation {
    public static final String OP_NAME = "cosinesimilarity";

    private Number constantNormalizedByNorm2X, constantNormalizedByNorm2Y;

    public CosineSimilarity(SameDiff sameDiff, SDVariable i_v, int[] dimensions) {
        super(sameDiff, i_v, dimensions);
    }

    public CosineSimilarity(SameDiff sameDiff, SDVariable i_v, SDVariable i_v2, int[] dimensions) {
        super(sameDiff, i_v, i_v2, dimensions);
    }

    public CosineSimilarity() {
        passThrough = true;
    }

    public CosineSimilarity(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public CosineSimilarity(INDArray x, INDArray y, long n) {
        super(x, y, n);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public CosineSimilarity(INDArray x) {
        super(x);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public CosineSimilarity(INDArray x, INDArray y) {
        super(x, y);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public CosineSimilarity(INDArray x, INDArray y, INDArray z, boolean allDistances) {
        this(x, y, z, x.lengthLong());
        this.isComplex = allDistances;
    }

    public CosineSimilarity(INDArray x, INDArray y, boolean allDistances) {
        this(x, y);
        this.isComplex = allDistances;
    }

    @Override
    public Type opType() {
        return Type.REDUCE3;
    }

    @Override
    public Type getOpType() {
        return opType();
    }

    @Override
    public int opNum() {
        return 2;
    }

    @Override
    public String opName() {
        return OP_NAME;
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v1) {
        //Let cosine(x,y) = a / b
        //a = sum_i (x_i * y_i)
        //b = sqrt(sum_i x_i^2) * sqrt(sum_i y_i^2) = l2(x) * l2(y)
        //Then:
        // dc(x,y)/dx_i = 1/b * (y - x * a / (l2(x))^2)

        return doDiff(sameDiff, f(), larg(), rarg(), i_v1.get(0), dimensions);
    }

    public static List<SDVariable> doDiff(SameDiff sameDiff, DifferentialFunctionFactory f, SDVariable x, SDVariable y,
                                          SDVariable gradOut, int... dimensions){
        SDVariable a = sameDiff.sum(x.mul(y),dimensions);
        SDVariable l2x = f.norm2(x, dimensions);
        SDVariable l2y = f.norm2(y, dimensions);
        SDVariable b = l2x.mul(l2y);

        int origRank = Shape.rankFromShape(x.getShape());
        SDVariable broadcastableA = f.reductionBroadcastableWithOrigShape(origRank, dimensions, a);
        SDVariable broadcastableB = f.reductionBroadcastableWithOrigShape(origRank, dimensions, b);
        SDVariable broadcastableL2xSq = f.reductionBroadcastableWithOrigShape(origRank, dimensions, sameDiff.square(l2x));
        SDVariable broadcastableL2ySq = f.reductionBroadcastableWithOrigShape(origRank, dimensions, sameDiff.square(l2y));
        SDVariable broadcastableGrad = f.reductionBroadcastableWithOrigShape(origRank, dimensions, gradOut);

        SDVariable dcdx = y.sub(x.mul(broadcastableA).div(broadcastableL2xSq)).div(broadcastableB);
        SDVariable dcdy = x.sub(y.mul(broadcastableA).div(broadcastableL2ySq)).div(broadcastableB);

        return Arrays.asList(dcdx.mul(broadcastableGrad), dcdy.mul(broadcastableGrad));
    }

    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " +  opName());

    }

    @Override
    public String tensorflowName() {
        throw new NoOpNameFoundException("No tensorflow op opName found for " +  opName());
    }

}
