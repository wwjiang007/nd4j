package org.nd4j.linalg.api.ops.impl.controlflow.compat;

import lombok.NonNull;
import lombok.val;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Exit extends BaseCompatOp {
    @Override
    public String opName() {
        return "exit";
    }

    @Override
    public List<int[]> calculateOutputShape() {
        if(args()[0].getArr() != null) {
            return Arrays.asList(args()[0].getShape(),args()[0].getShape());
        }
        else
            return Collections.emptyList();
    }

    @Override
    public SDVariable[] outputVariables() {
        return super.outputVariables();
    }

    @Override
    public String tensorflowName() {
        return "Exit";
    }

    @Override
    public Op.Type opType() {
        return Op.Type.EXIT;
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        super.initFromTensorFlow(nodeDef, initWith, attributesForNode, graph);
    }
}
