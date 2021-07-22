/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modifications copyright (C) 2021 University of Pisa, Dept. of Ingegneria dell'Informazione
 */

package simblock.node.consensus;

import static simblock.simulator.Main.random;

import java.math.BigInteger;
/**
 * Import immutable arbitrary-precision signed decimal numbers.
 */
import java.math.BigDecimal;
import simblock.block.Block;
import simblock.block.ProofOfWorkBlock;
import simblock.node.Node;
import simblock.task.MiningTask;

/**
 * The type Proof of work.
 */
@SuppressWarnings("unused")
public class ProofOfWork extends AbstractConsensusAlgo {
  /**
   * Instantiates a new Proof of work consensus algorithm.
   *
   * @param selfNode the self node
   */
  public ProofOfWork(Node selfNode) {
    super(selfNode);
  }

  /**
   * Set of functions on numerical computing based on Newton's method.
   */

  /**
     * Compute e^x to a given scale. Break x into its whole and fraction parts
     * and compute (e^(1 + fraction/whole))^whole using Taylor's formula.
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal exp(BigDecimal x, int scale) {
        // e^0 = 1
        if (x.signum() == 0) {
            return BigDecimal.valueOf(1);
        }

        // If x is negative, return 1/(e^-x).
        else if (x.signum() == -1) {
            return BigDecimal.valueOf(1).divide(exp(x.negate(), scale),
                    scale, BigDecimal.ROUND_HALF_EVEN);
        }

        // Compute the whole part of x.
        BigDecimal xWhole = x.setScale(0, BigDecimal.ROUND_DOWN);

        // If there isn't a whole part, compute and return e^x.
        if (xWhole.signum() == 0) {
            return expTaylor(x, scale);
        }

        // Compute the fraction part of x.
        BigDecimal xFraction = x.subtract(xWhole);

        // z = 1 + fraction/whole
        BigDecimal z = BigDecimal.valueOf(1)
                .add(xFraction.divide(xWhole, scale,
                        BigDecimal.ROUND_HALF_EVEN));

        // t = e^z
        BigDecimal t = expTaylor(z, scale);

        BigDecimal maxLong = BigDecimal.valueOf(Long.MAX_VALUE);
        BigDecimal result = BigDecimal.valueOf(1);

        // Compute and return t^whole using intPower().
        // If whole > Long.MAX_VALUE, then first compute products
        // of e^Long.MAX_VALUE.
        while (xWhole.compareTo(maxLong) >= 0) {
            result = result.multiply(intPower(t, Long.MAX_VALUE, scale))
                    .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            xWhole = xWhole.subtract(maxLong);

            Thread.yield();
        }
        return result.multiply(intPower(t, xWhole.longValue(), scale))
                .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * Compute the natural logarithm of x to a given scale, x > 0.
     */
    public static BigDecimal ln(BigDecimal x, int scale) {
        // Check that x > 0.
        if (x.signum() <= 0) {
            throw new IllegalArgumentException("x <= 0");
        }

        // The number of digits to the left of the decimal point.
        int magnitude = x.toString().length() - x.scale() - 1;

        if (magnitude < 3) {
            return lnNewton(x, scale);
        }

        // Compute magnitude*ln(x^(1/magnitude)).
        else {

            // x^(1/magnitude)
            BigDecimal root = intRoot(x, magnitude, scale);

            // ln(x^(1/magnitude))
            BigDecimal lnRoot = lnNewton(root, scale);

            // magnitude*ln(x^(1/magnitude))
            return BigDecimal.valueOf(magnitude).multiply(lnRoot)
                    .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        }
    }

    /**
     * Compute e^x to a given scale by the Taylor series.
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    private static BigDecimal expTaylor(BigDecimal x, int scale) {
        BigDecimal factorial = BigDecimal.valueOf(1);
        BigDecimal xPower = x;
        BigDecimal sumPrev;

        // 1 + x
        BigDecimal sum = x.add(BigDecimal.valueOf(1));

        // Loop until the sums converge
        // (two successive sums are equal after rounding).
        int i = 2;
        do {
            // x^i
            xPower = xPower.multiply(x).setScale(scale,
                    BigDecimal.ROUND_HALF_EVEN);

            // i!
            factorial = factorial.multiply(BigDecimal.valueOf(i));

            // x^i/i!
            BigDecimal term = xPower.divide(factorial, scale,
                    BigDecimal.ROUND_HALF_EVEN);

            // sum = sum + x^i/i!
            sumPrev = sum;
            sum = sum.add(term);

            ++i;
            Thread.yield();
        } while (sum.compareTo(sumPrev) != 0);

        return sum;
    }

    /**
     * Compute x^exponent to a given scale. Uses the same algorithm as class
     * numbercruncher.mathutils.IntPower.
     * 
     * @param x
     *            the value x
     * @param exponent
     *            the exponent value
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intPower(BigDecimal x, long exponent, int scale) {
        // If the exponent is negative, compute 1/(x^-exponent).
        if (exponent < 0) {
            return BigDecimal.valueOf(1).divide(
                    intPower(x, -exponent, scale), scale,
                    BigDecimal.ROUND_HALF_EVEN);
        }

        BigDecimal power = BigDecimal.valueOf(1);

        // Loop to compute value^exponent.
        while (exponent > 0) {

            // Is the rightmost bit a 1?
            if ((exponent & 1) == 1) {
                power = power.multiply(x).setScale(scale,
                        BigDecimal.ROUND_HALF_EVEN);
            }

            // Square x and shift exponent 1 bit to the right.
            x = x.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            exponent >>= 1;

            Thread.yield();
        }

        return power;
    }

    /**
     * Compute the natural logarithm of x to a given scale, x > 0. Use Newton's
     * algorithm.
     */
    private static BigDecimal lnNewton(BigDecimal x, int scale) {
        int sp1 = scale + 1;
        BigDecimal n = x;
        BigDecimal term;

        // Convergence tolerance = 5*(10^-(scale+1))
        BigDecimal tolerance = BigDecimal.valueOf(5).movePointLeft(sp1);

        // Loop until the approximations converge
        // (two successive approximations are within the tolerance).
        do {

            // e^x
            BigDecimal eToX = exp(x, sp1);

            // (e^x - n)/e^x
            term = eToX.subtract(n)
                    .divide(eToX, sp1, BigDecimal.ROUND_DOWN);

            // x - (e^x - n)/e^x
            x = x.subtract(term);

            Thread.yield();
        } while (term.compareTo(tolerance) > 0);

        return x.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * Compute the integral root of x to a given scale, x >= 0. Use Newton's
     * algorithm.
     * 
     * @param x
     *            the value of x
     * @param index
     *            the integral root value
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intRoot(BigDecimal x, long index, int scale) {
        // Check that x >= 0.
        if (x.signum() < 0) {
            throw new IllegalArgumentException("x < 0");
        }

        int sp1 = scale + 1;
        BigDecimal n = x;
        BigDecimal i = BigDecimal.valueOf(index);
        BigDecimal im1 = BigDecimal.valueOf(index - 1);
        BigDecimal tolerance = BigDecimal.valueOf(5).movePointLeft(sp1);
        BigDecimal xPrev;

        // The initial approximation is x/index.
        x = x.divide(i, scale, BigDecimal.ROUND_HALF_EVEN);

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do {
            // x^(index-1)
            BigDecimal xToIm1 = intPower(x, index - 1, sp1);

            // x^index
            BigDecimal xToI = x.multiply(xToIm1).setScale(sp1,
                    BigDecimal.ROUND_HALF_EVEN);

            // n + (index-1)*(x^index)
            BigDecimal numerator = n.add(im1.multiply(xToI)).setScale(sp1,
                    BigDecimal.ROUND_HALF_EVEN);

            // (index*(x^(index-1))
            BigDecimal denominator = i.multiply(xToIm1).setScale(sp1,
                    BigDecimal.ROUND_HALF_EVEN);

            // x = (n + (index-1)*(x^index)) / (index*(x^(index-1)))
            xPrev = x;
            x = numerator.divide(denominator, sp1, BigDecimal.ROUND_DOWN);

            Thread.yield();
        } while (x.subtract(xPrev).abs().compareTo(tolerance) > 0);

        return x;
    }



  /**
   * Mints a new block by simulating Proof of Work.
   */
  @Override
  public MiningTask minting() {
    Node selfNode = this.getSelfNode();
    ProofOfWorkBlock parent = (ProofOfWorkBlock) selfNode.getBlock();
    BigInteger difficulty = parent.getNextDifficulty();
    //System.out.println("Next Difficulty: " + difficulty.toString());

    double p = 1.0 / difficulty.doubleValue();
    //System.out.println("p: " + p);
    BigDecimal p_bigDec = BigDecimal.valueOf(p);
    
    double u = random.nextDouble();
    //System.out.println("u: " + u);
    BigDecimal u_bigDec = BigDecimal.valueOf(u);

    BigDecimal twoToThePow = BigDecimal.valueOf(Math.pow(2, -70));

    if(p_bigDec.compareTo(twoToThePow) <= 0) {
      //System.out.println("Null!");
      return null;
    }/*
    if(p <= ) {
      //System.out.println("Null!");
      return null;
    }*/

    BigDecimal numerator = ln(u_bigDec,20);

    //System.out.println("numerator: " + numerator.toString());


    BigDecimal one = new BigDecimal("1.0");

    //System.out.println(one.subtract(p_bigDec).toString());

    BigDecimal denominator = ln(one.subtract(p_bigDec),20);

    //System.out.println("denominator: " + denominator.toString());


    BigDecimal ratio = numerator.divide(denominator, 20, BigDecimal.ROUND_HALF_EVEN);

    //System.out.println("ratio: " + ratio.toString());

    BigInteger miningPow = BigInteger.valueOf(selfNode.getMiningPower());

    BigDecimal miningPowBigDec = new BigDecimal(miningPow);


    BigDecimal defRatio = ratio.divide(miningPowBigDec, 20, BigDecimal.ROUND_HALF_EVEN);

    //System.out.println("defRatioBigDec: " + defRatio.toString());

    BigInteger defRatioBigInt = defRatio.toBigInteger();

    //System.out.println("defRatioBigInt: " + defRatioBigInt.toString());

    //double value = Math.log(u) / Math.log( 1.0 - p);
    //System.out.println("value: " + value);
    //value = value / selfNode.getMiningPower();
    //System.out.println("value: " + value);

    //System.out.print("block mining time: " + defRatioBigInt.toString());
    return new MiningTask(selfNode, defRatioBigInt, difficulty);
  }

  /**
   * Tests if the receivedBlock is valid with regards to the current block. The receivedBlock
   * is valid if it is an instance of a Proof of Work block and the received block needs to have
   * a bigger difficulty than its parent next difficulty and a bigger total difficulty compared to
   * the current block.
   *
   * @param receivedBlock the received block
   * @param currentBlock  the current block
   * @return true if block is valid false otherwise
   */
  @Override
  public boolean isReceivedBlockValid(Block receivedBlock, Block currentBlock) {
    if (!(receivedBlock instanceof ProofOfWorkBlock)) {
      return false;
    }
    ProofOfWorkBlock recPoWBlock = (ProofOfWorkBlock) receivedBlock;
    ProofOfWorkBlock currPoWBlock = (ProofOfWorkBlock) currentBlock;
    int receivedBlockHeight = receivedBlock.getHeight();
    ProofOfWorkBlock receivedBlockParent = receivedBlockHeight == 0 ? null :
        (ProofOfWorkBlock) receivedBlock.getBlockWithHeight(receivedBlockHeight - 1);

    //TODO - dangerous to split due to short circuit operators being used, refactor?
    return (
        receivedBlockHeight == 0 ||
            recPoWBlock.getDifficulty().compareTo(receivedBlockParent.getNextDifficulty()) >= 0
    ) && (
        currentBlock == null ||
            recPoWBlock.getTotalDifficulty().compareTo(currPoWBlock.getTotalDifficulty()) > 0
    );
  }

  @Override
  public ProofOfWorkBlock genesisBlock() {
    return ProofOfWorkBlock.genesisBlock(this.getSelfNode());
  }

}
