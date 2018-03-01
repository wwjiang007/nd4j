package org.nd4j.linalg.api.ops.impl.layers.convolution.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.nd4j.linalg.api.ops.impl.layers.convolution.Pooling2D;

import java.util.LinkedHashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
@Data
public class Pooling2DConfig extends BaseConvolutionConfig {

    private int kh, kw, sy, sx, ph, pw, virtualHeight,virtualWidth;
    /**
     * Extra is an optional parameter mainly for use with pnorm right now.
     * All pooling implementations take 9 parameters save pnorm.
     * Pnorm takes 10 and is cast to an int.
     */
    private double extra;
    private Pooling2D.Pooling2DType type;
    @Builder.Default private Pooling2D.Divisor divisor = Pooling2D.Divisor.EXCLUDE_PADDING;
    private boolean isSameMode;
    @Builder.Default private int dh = 1;
    @Builder.Default private int dw = 1;
    @Builder.Default private boolean isNHWC = false;


    public Map<String,Object> toProperties() {
        Map<String,Object> ret = new LinkedHashMap<>();
        ret.put("kh",kh);
        ret.put("kw",kw);
        ret.put("sy",sy);
        ret.put("sx",sx);
        ret.put("ph",ph);
        ret.put("pw",pw);
        ret.put("virtualHeight",virtualHeight);
        ret.put("virtualWidth",virtualWidth);
        ret.put("extra",extra);
        ret.put("type",type.toString());
        ret.put("isSameMode",isSameMode);
        ret.put("dh",dh);
        ret.put("dw",dw);
        ret.put("isNHWC",isNHWC);
        return ret;
    }

}
