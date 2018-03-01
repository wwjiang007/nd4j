package org.nd4j.linalg.api.ops.impl.layers.convolution;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.descriptors.properties.PropertyMapping;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.Pooling2DConfig;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.*;


/**
 * Pooling2D operation
 */
@Slf4j
@Getter
public class AvgPooling2D extends DynamicCustomOp {

    protected Pooling2DConfig config;

    public enum Pooling2DType {
        MAX, AVG, PNORM,
    }

    public AvgPooling2D() {}

    @Builder(builderMethodName = "builder")
    @SuppressWarnings("Used in lombok")
    public AvgPooling2D(SameDiff sameDiff, SDVariable[] inputs, INDArray[] arrayInputs, INDArray[] arrayOutputs, Pooling2DConfig config) {
        super(null,sameDiff, inputs, false);
        if(arrayInputs != null) {
            addInputArgument(arrayInputs);
        }

        if(arrayOutputs != null) {
            addOutputArgument(arrayOutputs);
        }

        this.config = config;


        addArgs();
    }

    @Override
    public Map<String, Map<String, PropertyMapping>> mappingsForFunction() {
        Map<String,Map<String,PropertyMapping>> ret = new HashMap<>();
        Map<String,PropertyMapping> map = new HashMap<>();
        val strideMapping = PropertyMapping.builder()
                .tfAttrName("strides")
                .onnxAttrName("strides")
                .propertyNames(new String[]{"sx","sy"})
                .build();

        val paddingMapping = PropertyMapping.builder()
                .onnxAttrName("padding")
                .tfAttrName("padding")
                .propertyNames(new String[]{"px","py"})
                .build();

        val kernelMapping = PropertyMapping.builder()
                .propertyNames(new String[]{"kh","kw"})
                .tfInputPosition(1)
                .onnxAttrName("ksize")
                .build();

        val dilationMapping = PropertyMapping.builder()
                .onnxAttrName("dilations")
                .propertyNames(new String[]{"dw","dh"})
                .tfAttrName("rates")
                .build();


        //data_format
        val dataFormatMapping = PropertyMapping.builder()
                .propertyNames(new String[]{"isNHWC"})
                .tfAttrName("data_format")
                .build();

        map.put("sx", strideMapping);
        map.put("sy", strideMapping);
        map.put("kh", kernelMapping);
        map.put("kw", kernelMapping);
        map.put("dw", dilationMapping);
        map.put("dh", dilationMapping);
        map.put("ph",paddingMapping);
        map.put("pw",paddingMapping);
        map.put("isNHWC",dataFormatMapping);

        ret.put(onnxName(),map);
        ret.put(tensorflowName(),map);


        return ret;
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
    public Map<String, Object> propertiesForFunction() {
        return config.toProperties();
    }

    private void addArgs() {
        addIArgument(
                new int[]{config.getKh(),
                        config.getKw(),
                        config.getSy(),
                        config.getSx(),
                        config.getPh(),
                        config.getPw(),
                        config.getDh(),
                        config.getDw(),
                        ArrayUtil.fromBoolean(config.isSameMode()),
                        (int) config.getExtra(),
                        ArrayUtil.fromBoolean(config.isNHWC())});

    }

    @Override
    public String opName() {
        return getPoolingPrefix() + "pool2d";
    }


    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        List<SDVariable> ret = new ArrayList<>();
        List<SDVariable> inputs = new ArrayList<>();
        inputs.addAll(Arrays.asList(args()));
        inputs.add(f1.get(0));
        Pooling2DDerivative pooling2DDerivative = Pooling2DDerivative.derivativeBuilder()
                .inputs(inputs.toArray(new SDVariable[inputs.size()]))
                .sameDiff(sameDiff)
                .config(config)
                .build();
        ret.addAll(Arrays.asList(pooling2DDerivative.outputVariables()));
        return ret;
    }


    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        val aStrides = nodeDef.getAttrOrThrow("strides");
        val tfStrides = aStrides.getList().getIList();

        val aKernels = nodeDef.getAttrOrThrow("ksize");
        val tfKernels = aKernels.getList().getIList();

        int sY = 0;
        int sX = 0;

        int ph = 0;
        int pw = 0;

        int kY = 0;
        int kX = 0;

        val aPadding = nodeDef.getAttrOrThrow("padding");
        val padding = aPadding.getList().getIList();

        val paddingMode = aPadding.getS().toStringUtf8().replaceAll("\"","");

        boolean isSameMode = paddingMode.equalsIgnoreCase("SAME");

        String data_format = "nhwc";
        if (nodeDef.containsAttr("data_format")) {
            val attr = nodeDef.getAttrOrThrow("data_format");

            data_format = attr.getS().toStringUtf8().toLowerCase();
        }

        if (data_format.equalsIgnoreCase("nhwc")) {
            sY = tfStrides.get(1).intValue();
            sX = tfStrides.get(2).intValue();

            kY = tfKernels.get(1).intValue();
            kX = tfKernels.get(2).intValue();

            ph = padding.size() > 0 ? padding.get(1).intValue() : 0;
            pw = padding.size() > 0 ? padding.get(2).intValue() : 0;
        } else {
            sY = tfStrides.get(2).intValue();
            sX = tfStrides.get(3).intValue();

            kY = tfKernels.get(2).intValue();
            kX = tfKernels.get(3).intValue();

            ph = padding.size() > 0 ? padding.get(2).intValue() : 0;
            pw = padding.size() > 0 ? padding.get(3).intValue() : 0;
        }

        Pooling2DConfig pooling2DConfig = Pooling2DConfig.builder()
                .sy(sY)
                .sx(sX)
                .type(Pooling2D.Pooling2DType.AVG)
                .isSameMode(isSameMode)
                .kh(kY)
                .kw(kX)
                .ph(ph)
                .pw(pw)
                .virtualWidth(1)
                .virtualHeight(1)
                .isNHWC(data_format.equalsIgnoreCase("nhwc"))
                .extra(0.0) // averaging only for non-padded values
                .build();
        this.config = pooling2DConfig;
        addArgs();
        log.debug("Pooling: k: [{},{}]; s: [{}, {}], padding: {}", kY, kX, sY, sX, aPadding);


    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        val paddingVal = !attributesForNode.containsKey("auto_pad") ? "VALID" : attributesForNode.get("auto_pad").getS().toStringUtf8();
        val kernelShape = attributesForNode.get("kernel_shape").getIntsList();
        val padding = !attributesForNode.containsKey("pads") ? Arrays.asList(1L) : attributesForNode.get("pads").getIntsList();
        val strides = attributesForNode.get("strides").getIntsList();

        Pooling2DConfig pooling2DConfig = Pooling2DConfig.builder()
                .sy(strides.get(0).intValue())
                .sx(strides.get(1).intValue())
                .type(Pooling2D.Pooling2DType.AVG)
                .isSameMode(paddingVal.equalsIgnoreCase("SAME"))
                .kh(kernelShape.get(0).intValue())
                .kw(kernelShape.get(1).intValue())
                .ph(padding.get(0).intValue())
                .pw(padding.size() < 2 ? padding.get(0).intValue() : padding.get(1).intValue())
                .virtualWidth(1)
                .virtualHeight(1)
                .build();
        this.config = pooling2DConfig;
        addArgs();
    }




    @Override
    public String onnxName() {
        return "AveragePool";
    }

    @Override
    public String tensorflowName() {
        return "AvgPool";
    }


    public String getPoolingPrefix() {
        return "avg";
    }

}
