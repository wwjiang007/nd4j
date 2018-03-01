package org.nd4j.linalg.api.ops.impl.shape;

import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.descriptors.properties.PropertyMapping;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unstack op conversion
 *
 * @author raver119@gmail.com
 */
public class Unstack extends DynamicCustomOp {

    private int num;
    private int axis;

    @Override
    public String[] tensorflowNames() {
        return new String[] {"Unstack","Unpack"};
    }

    @Override
    public String tensorflowName() {
        return "Unstack";
    }




    @Override
    public String opName() {
        return "unstack";
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        val attrAxis = nodeDef.getAttrOrThrow("axis");
        int axis = (int) attrAxis.getI();
        this.axis = axis;
        addIArgument(axis);

    }


    @Override
    public Map<String, Object> propertiesForFunction() {
        Map<String,Object> ret = new LinkedHashMap<>();
        ret.put("axis",axis);
        return ret;
    }



    @Override
    public Map<String, Map<String, PropertyMapping>> mappingsForFunction() {
        Map<String,Map<String,PropertyMapping>> ret = new HashMap<>();
        Map<String,PropertyMapping> map = new HashMap<>();

        val axisMapping = PropertyMapping.builder()
                .onnxAttrName("axis")
                .tfInputPosition(-1)
                .propertyNames(new String[]{"axis"})
                .build();

        map.put("axis",axisMapping);

        ret.put(tensorflowName(),map);

        return ret;
    }


    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        throw new UnsupportedOperationException("No analog found for onnx for " + opName());
    }
}
