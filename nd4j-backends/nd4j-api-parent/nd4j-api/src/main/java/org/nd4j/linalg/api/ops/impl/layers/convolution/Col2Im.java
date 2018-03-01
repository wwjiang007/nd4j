package org.nd4j.linalg.api.ops.impl.layers.convolution;

import lombok.Builder;
import lombok.Getter;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.Conv2DConfig;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.List;
import java.util.Map;

/**
 * Created by agibsonccc on 3/9/16.
 */
@Getter
public class Col2Im extends DynamicCustomOp {

    protected Conv2DConfig conv2DConfig;

    @Builder(builderMethodName = "builder")
    public Col2Im(SameDiff sameDiff, SDVariable[] inputFunctions, INDArray[] inputArrays, INDArray[] outputs, Conv2DConfig conv2DConfig) {
        super(null,inputArrays,outputs);
        if(sameDiff != null) {
            this.sameDiff = sameDiff;
        }


        this.conv2DConfig = conv2DConfig;

        addArgs();
    }

    public Col2Im() {}

    protected void addArgs() {
        addIArgument(conv2DConfig.getSy());
        addIArgument(conv2DConfig.getSx());
        addIArgument(conv2DConfig.getPh());
        addIArgument(conv2DConfig.getPw());
        addIArgument(conv2DConfig.getKh());
        addIArgument(conv2DConfig.getKw());
        addIArgument(conv2DConfig.getDh());
        addIArgument(conv2DConfig.getDw());
        addIArgument(ArrayUtil.fromBoolean(conv2DConfig.isSameMode()));

    }

    @Override
    public Map<String, Object> propertiesForFunction() {
        return conv2DConfig.toProperties();
    }



    @Override
    public String opName() {
        return "col2im";
    }


    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        throw new UnsupportedOperationException("Unable to run derivative op on col2im");
    }
}
