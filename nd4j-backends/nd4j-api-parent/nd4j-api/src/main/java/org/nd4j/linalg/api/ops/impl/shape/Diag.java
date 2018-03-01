package org.nd4j.linalg.api.ops.impl.shape;

import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Computes a diagonal matrix of shape (n, n) from a vector of length n.
 * More generally puts a an n-dimensional tensor on the diagonal part
 * of a tensor of 2*n dimensions.
 *
 * @author Max Pumperla
 */
public class Diag extends DynamicCustomOp {

    public Diag() {
    }

    public Diag(SameDiff sameDiff, SDVariable[] args, boolean inPlace) {
        super(null, sameDiff, args, inPlace);

    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v) {
        SDVariable grad = i_v.get(0);
        SDVariable ret = sameDiff.diagPart(grad);
        return Collections.singletonList(ret);
    }


    @Override
    public String opName() {
        return "diag";
    }

    @Override
    public String tensorflowName() {
        return "Diag";
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        super.initFromTensorFlow(nodeDef, initWith, attributesForNode, graph);
    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        super.initFromOnnx(node, initWith, attributesForNode, graph);
    }

}
