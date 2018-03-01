package org.nd4j.linalg.api.ops.impl.transforms;

import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Fill extends DynamicCustomOp {


    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        if(nodeDef.getInputCount() == 2) {
            val targetNode = TFGraphMapper.getInstance().getNodeWithNameFromGraph(graph,nodeDef.getInput(1));
            val mapper = TFGraphMapper.getInstance();
            val secondInputAsScalar = mapper.getNDArrayFromTensor("value",targetNode,graph);
            //must be scalar
            if(secondInputAsScalar.length() == 1) {
                addTArgument(secondInputAsScalar.getDouble(0));
            }
            else {
                throw new ND4JIllegalStateException("Second input to node " + nodeDef + " should be scalar!");
            }
        }

    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        super.initFromOnnx(node, initWith, attributesForNode, graph);
    }

    @Override
    public void assertValidForExecution() {
        val descriptor = getDescriptor();
        if(descriptor.getNumInputs() > 0 && numInputArguments() >  2 || numInputArguments() < 1)
            throw new ND4JIllegalStateException("Op failure for " + opName() + " Number of inputs is invalid for execution. Specified " + numInputArguments() + " but should be " + descriptor.getNumInputs());

        if(descriptor.getNumOutputs() > 0 && numOutputArguments() != descriptor.getNumOutputs())
            throw new ND4JIllegalStateException("Op failure for " + opName() + " Number of outputs is invalid for execution. Specified " + numOutputArguments() + " but should be " + descriptor.getNumInputs());

        //< 0 means dynamic size
        if(descriptor.getNumIArgs() >= 0 && numIArguments() != descriptor.getNumIArgs())
            throw new ND4JIllegalStateException("Op failure for " + opName() + " Number of integer arguments is invalid for execution. Specified " + numIArguments() + " but should be " + descriptor.getNumIArgs());

        if(descriptor.getNumTArgs() >= 0 && numTArguments() < 1)
            throw new ND4JIllegalStateException("Op failure for " + opName() + " Number of inputs is invalid for execution. Specified " + numTArguments() + " but should be " + descriptor.getNumTArgs());

    }


    @Override
    public List<int[]> calculateOutputShape() {
        if(args().length < 2)
            return Collections.emptyList();

        val shape = args()[0].getArr();
        if(shape == null)
            return Collections.emptyList();
        else {
            if(shape.length() == 1) {
                if(shape.getDouble(0) < 1)
                    return Arrays.asList(new int[]{1,1});
                else
                    return Arrays.asList(new int[]{1,shape.getInt(0)});
            }
        }

        return Arrays.asList(shape.data().asInt());
    }

    @Override
    public String opName() {
        return "fill";
    }

    @Override
    public String onnxName() {
        return "ConstantFill";
    }

    @Override
    public String tensorflowName() {
        return "Fill";
    }

    @Override
    public Op.Type opType() {
        return Op.Type.CUSTOM;
    }
}
