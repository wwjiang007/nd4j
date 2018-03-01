package org.nd4j.linalg.api.ops.impl.layers.convolution;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.descriptors.properties.PropertyMapping;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.DeConv2DConfig;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.lang.reflect.Field;
import java.util.*;


/**
 * DeConv2D operation
 */
@Slf4j
@Getter
public class DeConv2D extends DynamicCustomOp {


    protected DeConv2DConfig config;

    public DeConv2D() {}


    @Builder(builderMethodName = "builder")
    public DeConv2D(SameDiff sameDiff, SDVariable[] inputs,INDArray[] inputArrays, INDArray[] outputs,boolean inPlace, DeConv2DConfig config) {
        super(null,sameDiff, inputs, inPlace);
        this.config = config;
        if(inputArrays != null) {
            addInputArgument(inputArrays);
        }

        if(outputs != null) {
            addOutputArgument(outputs);
        }


        addArgs();
    }


    @Override
    public boolean isConfigProperties() {
        return true;
    }

    @Override
    public String configFieldName() {
        return "config";
    }

    @Override
    public void setValueFor(Field target, Object value) {
        config.setValueFor(target,value);
    }


    @Override
    public Map<String, Object> propertiesForFunction() {
        return config.toProperties();
    }

    private void addArgs() {
       addIArgument(config.getKY());
       addIArgument(config.getKX());
       addIArgument(config.getSY());
       addIArgument(config.getSX());
       addIArgument(config.getPY());
       addIArgument(config.getPX());
       addIArgument(config.getDY());
       addIArgument(config.getDX());
       addIArgument(ArrayUtil.fromBoolean(config.isSameMode()));

    }


    @Override
    public Map<String, Map<String, PropertyMapping>> mappingsForFunction() {
        Map<String,Map<String,PropertyMapping>> ret = new HashMap<>();
        Map<String,PropertyMapping> map = new HashMap<>();
        val strideMapping = PropertyMapping.builder()
                .tfAttrName("strides")
                .onnxAttrName("strides")
                .build();



        val kernelMapping = PropertyMapping.builder()
                .propertyNames(new String[]{"kh","kw"})
                .tfInputPosition(1)
                .onnxAttrName("kernel_shape")
                .build();

        val dilationMapping = PropertyMapping.builder()
                .onnxAttrName("dilations")
                .propertyNames(new String[]{"dw","dh"})
                .tfAttrName("rates")
                .build();



        val sameMode = PropertyMapping.builder()
                .onnxAttrName("auto_pad")
                .propertyNames(new String[]{"isSameMode"})
                .tfAttrName("padding")
                .build();

        val paddingWidthHeight = PropertyMapping.builder()
                .onnxAttrName("padding")
                .propertyNames(new String[]{"ph","pw"})
                .build();


        map.put("sx", strideMapping);
        map.put("sy", strideMapping);
        map.put("kh", kernelMapping);
        map.put("kw", kernelMapping);
        map.put("dw", dilationMapping);
        map.put("dh", dilationMapping);
        map.put("isSameMode",sameMode);
        map.put("ph", paddingWidthHeight);
        map.put("pw", paddingWidthHeight);

        ret.put(onnxName(),map);
        ret.put(tensorflowName(),map);
        return ret;
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        val aStrides = nodeDef.getAttrOrThrow("strides");
        val tfStrides = aStrides.getList().getIList();
        int sY = 1;
        int sX = 1;
        int kY = 1;
        int kX = 1;

        val aPadding = nodeDef.getAttrOrDefault("padding", null);

        val paddingMode = aPadding.getS().toStringUtf8();

        val args = args();
        INDArray arr = sameDiff.getVariable(args[1].getVarName()).getArr();
        if(arr == null) {
            arr = TFGraphMapper.getInstance().getNDArrayFromTensor(nodeDef.getInput(0), nodeDef, graph);
            // TODO: arguable. it might be easier to permute weights once
            //arr = (arr.permute(3, 2, 0, 1).dup('c'));
            val  varForOp = initWith.getVariable(args[1].getVarName());
            if(arr != null)
                initWith.associateArrayWithVariable(arr, varForOp);


        }

        String dataFormat = "nhwc";
        if (nodeDef.containsAttr("data_format")) {
            val attr = nodeDef.getAttrOrThrow("data_format");
            dataFormat = attr.getS().toStringUtf8().toLowerCase();
        }


        if (dataFormat.equalsIgnoreCase("nchw")) {
            sY = tfStrides.get(2).intValue();
            sX = tfStrides.get(3).intValue();

            kY = arr.size(2);
            kX = arr.size(3);
        } else {
            sY = tfStrides.get(1).intValue();
            sX = tfStrides.get(2).intValue();

            kY = arr.size(0);
            kX = arr.size(1);
        }


        boolean isSameMode = paddingMode.equalsIgnoreCase("SAME");
        DeConv2DConfig conv2DConfig = DeConv2DConfig.builder()
                .kY(kY)
                .kX(kX)
                .sX(sX)
                .sY(sY)
                .isSameMode(isSameMode)
                //c++ check checks for nchw
                .isNHWC(dataFormat.equalsIgnoreCase("nhwc"))
                .build();
        this.config = conv2DConfig;

        addArgs();


    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        val autoPad = !attributesForNode.containsKey("auto_pad") ? "VALID" : attributesForNode.get("auto_pad").getS().toStringUtf8();
        val dilations = attributesForNode.get("dilations");
        val dilationY = dilations == null ? 1 : dilations.getIntsList().get(0).intValue();
        val dilationX = dilations == null ? 1 : dilations.getIntsList().get(1).intValue();
        val group = attributesForNode.get("group");

        val kernelShape = attributesForNode.get("kernel_shape");
        int kY = kernelShape.getIntsList().get(0).intValue();
        int kX = kernelShape.getIntsList().size() < 2 ? kY : kernelShape.getIntsList().get(1).intValue();

        val vertexId = args()[0];

        INDArray arr = vertexId.getArr();
        arr = (arr.permute(3, 2, 0, 1).dup('c'));
        initWith.associateArrayWithVariable(arr, vertexId);

       String dataFormat = "nhwc";

        val strides = attributesForNode.get("strides");
        val sY = strides.getIntsList().get(0);
        val sX = strides.getIntsList().size() < 2 ? sY : strides.getIntsList().get(1);
        boolean isSameMode = autoPad
                .equalsIgnoreCase("SAME");


        DeConv2DConfig conv2DConfig = DeConv2DConfig.builder()
                .kY(kY)
                .kX(kX)
                .sX(sX.intValue())
                .sY(sY.intValue())
                .isSameMode(isSameMode)
                //c++ check checks for nchw
                .isNHWC(dataFormat.equalsIgnoreCase("nhwc"))
                .build();
        this.config = conv2DConfig;

        addArgs();

        addOutputArgument(arr);
    }




    @Override
    public String opName() {
        return "deconv2d";
    }

    @Override
    public String onnxName() {
        return "ConvTranspose";
    }

    @Override
    public String tensorflowName() {
        return "Conv2DTranspose";
    }


    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        List<SDVariable> ret = new ArrayList<>();
        List<SDVariable> inputs = new ArrayList<>();
        inputs.addAll(Arrays.asList(args()));
        inputs.addAll(f1);
        DeConv2DDerivative deConv2DDerivative = DeConv2DDerivative.derivativeBuilder()
                .sameDiff(sameDiff)
                .config(config)
                .inputs(inputs.toArray(new SDVariable[inputs.size()]))
                .build();
        ret.addAll(Arrays.asList(deConv2DDerivative.outputVariables()));
        return ret;
    }

}
