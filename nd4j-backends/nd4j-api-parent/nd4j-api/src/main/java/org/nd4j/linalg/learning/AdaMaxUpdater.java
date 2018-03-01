package org.nd4j.linalg.learning;

import lombok.Data;
import org.apache.commons.math3.util.FastMath;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.OldMax;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.AdaMax;
import org.nd4j.linalg.ops.transforms.Transforms;

/**
 * The AdaMax updater, a variant of Adam.
 * http://arxiv.org/abs/1412.6980
 *
 * @author Justin Long
 */
@Data
public class AdaMaxUpdater implements GradientUpdater<AdaMax> {

    private final AdaMax config;

    private INDArray m, u; // moving avg & exponentially weighted infinity norm
    private char gradientReshapeOrder;

    public AdaMaxUpdater(AdaMax config) {
        this.config = config;
    }

    @Override
    public void setStateViewArray(INDArray viewArray, int[] gradientShape, char gradientOrder, boolean initialize) {
        if (!viewArray.isRowVector())
            throw new IllegalArgumentException("Invalid input: expect row vector input");
        if (initialize)
            viewArray.assign(0);
        int length = viewArray.length();
        this.m = viewArray.get(NDArrayIndex.point(0), NDArrayIndex.interval(0, length / 2));
        this.u = viewArray.get(NDArrayIndex.point(0), NDArrayIndex.interval(length / 2, length));

        //Reshape to match the expected shape of the input gradient arrays
        this.m = Shape.newShapeNoCopy(this.m, gradientShape, gradientOrder == 'f');
        this.u = Shape.newShapeNoCopy(this.u, gradientShape, gradientOrder == 'f');
        if (m == null || u == null)
            throw new IllegalStateException("Could not correctly reshape gradient view arrays");

        this.gradientReshapeOrder = gradientOrder;
    }

    /**
     * Calculate the update based on the given gradient
     *
     * @param gradient  the gradient to get the update for
     * @param iteration
     * @return the gradient
     */
    @Override
    public void applyUpdater(INDArray gradient, int iteration, int epoch) {
        if (m == null || u == null)
            throw new IllegalStateException("Updater has not been initialized with view state");

        //m = B_1 * m + (1-B_1)*grad
        m.muli(config.getBeta1()).addi(gradient.mul(1 - config.getBeta1()));

        //u = max(B_2 * u, |grad|)
        u.muli(config.getBeta2());
        Transforms.abs(gradient, false); //In-place should be OK here, original gradient values aren't used again later
        Nd4j.getExecutioner().exec(new OldMax(u, gradient, u, u.length()));

        double beta1t = FastMath.pow(config.getBeta1(), iteration + 1);

        double learningRate = config.getLearningRate(iteration, epoch);
        double alphat = learningRate / (1.0 - beta1t);
        if (Double.isNaN(alphat) || Double.isInfinite(alphat) || alphat == 0.0) {
            alphat = config.getEpsilon();
        }

        u.addi(1e-32); // prevent NaNs in params
        gradient.assign(m).muli(alphat).divi(u);
    }
}
