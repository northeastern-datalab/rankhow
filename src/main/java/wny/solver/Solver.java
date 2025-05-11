package wny.solver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.javatuples.Pair;

import wny.entities.Constraint;
import wny.entities.Tuple;

/** 
 * A solver class which implements all pre-processing tasks
 * It needs to be extended to be some specific solver or optimizer
 * @author Zixuan Chen
*/
public class Solver {
    protected ArrayList<Tuple> tuples;
    protected int num_attributes;
    protected int num_tuples;
    protected double[] standard_deviation;
    protected BigDecimal[] weights;
    protected int[] ranking;
    protected int[] given_ranking;
    protected ArrayList<Constraint> constraints;
    protected double gap;
    protected int error;

    /** 
     * @param tuples All tuples of a relation
     * @param given_ranking The given ranking
     * @param gap A gap for dealing with numerical issues
    */
    public Solver (ArrayList<Tuple> tuples, int[] given_ranking, double gap) {
        this.tuples = tuples;
        this.given_ranking = given_ranking;
        this.gap = gap;
        num_attributes = tuples.get(0).values.length - 1;
        num_tuples = tuples.size();
        compute_standard_deviation();
        weights = new BigDecimal[num_attributes];
        constraints = new ArrayList<Constraint>();
	}

    /** 
     * Add one flexible constraint
     * @param c The constraint to be added
    */
    public void addConstraint(Constraint c) {
        constraints.add(c);
    }

    /** 
     * Constrain the solution space to a cell given the center of the cell
     * @param weight The center point of the cell
     * @param size The size of the cell
    */
    public void build_cell(double[] point, double size) {
        for (int i = 0; i < num_attributes; i++) {
            if (point[i] - size / 2 > 0) {
                Constraint c_l = new Constraint(i, "raw_min", point[i] - size / 2);
                constraints.add(c_l);
            }
            if (point[i] + size / 2 < 1) {
                Constraint c_u = new Constraint(i, "raw_max", point[i] + size / 2);
                constraints.add(c_u);
            }
        }
    }

    /** 
     * Constrain the solution space to a cell given a corner of the cell
     * @param weight One corner point of the cell
     * @param direction An array indicating the direction to explore in the cell
     * @param size The size of the cell
    */
    public void build_cell_corner(double[] point, double[] direction, double size) {
        for (int i = 0; i < num_attributes; i++) {
            if (direction[i] >= 0) {
                Constraint c_l = new Constraint(i, "raw_min", point[i]);
                constraints.add(c_l);
                if (point[i] + size < 1) {
                    Constraint c_u = new Constraint(i, "raw_max", point[i] + size);
                    constraints.add(c_u);
                }
            } else {
                Constraint c_u = new Constraint(i, "raw_max", point[i]);
                constraints.add(c_u);
                if (point[i] - size > 0) {
                    Constraint c_l = new Constraint(i, "raw_min", point[i] - size);
                    constraints.add(c_l);
                }
            }
        }
    }

    /** 
     * Clear all constraints
    */
    public void clearConstraint() {
        constraints = new ArrayList<Constraint>();
    }

    /** 
     * Compute the standard deviation of each column so that the standard weight can also be printed
    */
    protected void compute_standard_deviation() {
        standard_deviation = new double[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            double sum = 0, average;
            for (int j = 0; j < num_tuples; j++) {
                sum += Double.valueOf(tuples.get(j).values[i + 1]);
            }
            average = sum / num_tuples;
            sum = 0;
            for (int j = 0; j < num_tuples; j++) {
                sum += (Double.valueOf(tuples.get(j).values[i + 1]) - average) * (Double.valueOf(tuples.get(j).values[i + 1]) - average);
            }
            standard_deviation[i] = Math.sqrt(sum / num_tuples);
        }
    }

    /** 
     * @return The weights
    */
    public double[] getWeights() {
        double[] weights_double = new double[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            weights_double[i] = this.weights[i].doubleValue();
        }
        return weights_double;
    }

    /** 
     * @return The error
    */
    public int getError() {
        return error;
    }

    /** 
     * Rank the tuples based on the weights
     * The data structure of weights is BigDecimal
     * @param k k in top-k
    */
    protected void rank(int k) {
        ranking = new int[num_tuples];
        ArrayList<Pair<BigDecimal, Integer>> scores = new ArrayList<Pair<BigDecimal, Integer>>();
        for (int i = 0; i < num_tuples; i++) {
            BigDecimal score = new BigDecimal(0.0);
            for (int j = 0; j < num_attributes; j++) {
                BigDecimal value = new BigDecimal(tuples.get(i).values[j + 1]);
                score = score.add(weights[j].multiply(value));
            }
            // System.out.println(tuples.get(i).values[0] + '\n' + score);
            Pair<BigDecimal, Integer> pair = new Pair<BigDecimal, Integer>(score, i);
            scores.add(pair);
        }

        Collections.sort(scores, Comparator.reverseOrder());

        BigDecimal center = new BigDecimal(gap).divide(new BigDecimal(2));

        int count = 0;
        for (int i = 0; i < num_tuples; i++) {
            if (scores.get(i).getValue1() < k) {
                ranking[scores.get(i).getValue1()] = i + 1;
                BigDecimal score = scores.get(i).getValue0();
                for (int j = i - 1; j >= 0; j--) {
                    if (scores.get(j).getValue0().subtract(score).compareTo(center) == -1) {
                        ranking[scores.get(i).getValue1()]--;
                    } else {
                        break;
                    }
                }
                count ++;
                if (count == k) {
                    break;
                }
            }
        }
    }

    /** 
     * @param k k in top-k. 0 means full dataset
     * @return The ranking
    */
    public int[] getRanking(int k) {
        k = (k == 0) ? num_tuples : k;

        System.out.print("Ranking:");
        for (int i = 0; i < k; i++) {
            System.out.print(" " + ranking[i]);
        }
        System.out.println();
        return ranking;
    }
}