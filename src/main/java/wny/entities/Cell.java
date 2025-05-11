package wny.entities;

import java.util.ArrayList;

import org.javatuples.Pair;

/** 
 * A cell which is a hyper-rectangle
 * @author Zixuan Chen
*/

public class Cell implements Comparable<Cell> {
    private double[] center;
    private double size;
    private int num_attributes;
    public int upper_bound;
    public int lower_bound;

    /** 
     * @param center The center of the cell
     * @param size The size (side length) of the cell
    */
    public Cell(double[] center, double size) {
        this.center = center;
        this.size = size;
        num_attributes = center.length;
    }

    /** 
     * @return all corner points of the cell
    */
    private double[][] getCorners() {
        double[][] corners= {{center[0] + size / 2}, {center[0] - size / 2}};
        //  = new double[(int) Math.pow(2, num_attributes)][num_attributes];

        for (int i = 2; i <= num_attributes; i++) {
            double[][] previous = corners;
            corners = new double[(int) Math.pow(2, i)][num_attributes];

            int length = previous.length;
            for (int j = 0; j < length; j++) {
                for (int l = 0; l < i - 1; l++) {
                    corners[j][l] = previous[j][l];
                    corners[j + length][l] = previous[j][l]; 
                }
                corners[j][i - 1] = center[i - 1] + size / 2;
                corners[j + length][i - 1] = center[i - 1] - size / 2;
            }
        }

        return corners;
    }


    /** 
     * Return the relationship between one hyperplane (inequality with >= 0) and the cell
     * @return 1 for the inequality is always true, -1 for the inequality is always false, 0 for two relationships both exist in the cell
    */
    private int relationship_check(double[] inequality) {
        double[][] corners = getCorners();

        boolean win = false, lost = false;
        
        for (int i = 0; i < corners.length; i++) {
            double value = 0;
            for (int j = 0; j < num_attributes; j++) {
                value += inequality[j] * corners[i][j];
            }
            if (value >= 0) win = true;
            else lost = true;
            if (win && lost) return 0;
        }
        if (win) return 1;
        else return -1;
    }

    /** 
     * Get the bounds of the error in this cell
     * @param tuples All tuples of a relation
     * @param given_ranking The given ranking
     * @param k
     * @param gap A gap for dealing with numerical issues
     * @return a pair of upper bound and lower bound for the cell
    */
    public Pair<Integer, Integer> getBounds(ArrayList<Tuple> tuples, int[] given_ranking, int k, double gap) {
        int num_tuples = tuples.size(), upper = 0, lower = 0;

        for (int i = 0; i < k; i++) {
            int num_dominatees = 0, num_dominators = 0;
            for (int j = 0; j < num_tuples; j++) {
                if (i != j) {
                    int comparison = tuples.get(i).isDominating(tuples.get(j), gap);
                    if (comparison == 0) {
                        double[] inequality = new double[num_attributes];
                        for (int l = 0; l < num_attributes; l++) {
                            inequality[l] = Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]);
                        }
                        int cell_comparison = relationship_check(inequality);
                        if (cell_comparison == 1) {
                            num_dominatees++;
                        } else if (cell_comparison == -1) {
                            num_dominators++;
                        }
                    } else if (comparison == 1) {
                        num_dominatees++;
                    } else if (comparison == -1) {
                        num_dominators++;
                    }
                }
            }
            int high = num_dominators + 1;
            int low = num_tuples - num_dominatees;

            if (given_ranking[i] < high) {
                lower += high - given_ranking[i];
                upper += low - given_ranking[i];
            } else if (given_ranking[i] > low) {
                lower += given_ranking[i] - low;
                upper += given_ranking[i] - high;
            } else {
                // lower += 0;
                upper += Math.max(given_ranking[i] - high, low - given_ranking[i]);
            }
        }

        upper_bound = upper;
        lower_bound = lower;
        return new Pair<Integer,Integer>(upper, lower);
    }

    /** 
     * Divide the cell into (1 / divisor) ^ num_attributes smaller cells 
     * @return their cell centers
    */
    public double[][] divide(int divisor) {
        double[][] centers = new double[divisor][1];
        for (int i = 0; i < divisor; i++) {
            centers[i][0] = center[0] - size / 2 + (2 * i + 1) * size / divisor / 2;
        }

        for (int i = 2; i <= num_attributes; i++) {
            double[][] previous = centers;
            centers = new double[(int) Math.pow(divisor, i)][num_attributes];

            int length = previous.length;
            for (int j = 0; j < length; j++) {
                for (int l = 0; l < divisor; l++) {
                    for (int a = 0; a < i - 1; a++) {
                        centers[j + length * l][a] = previous[j][a];
                    }
                    centers[j + length * l][i - 1] = center[i - 1] - size / 2 + (2 * l + 1) * size / divisor / 2;
                }
            }
        }

        return centers;
    }

    /**
     * @return the center of the cell
     */
    public double[] getCenter() {
        return center;
    }

    /**
     * @return the size of the cell
     */
    public double size() {
        return size;
    }

    /**
     * 
     * A cell is compared with another cell according to the lower bound
     * @param other The other cell
     * @return An integer showing whether the cell has larger, same or smaller size
     */
    @Override
    public int compareTo(Cell other) {
        if (lower_bound > other.lower_bound) return 1;
        else if (lower_bound == other.lower_bound) {
            if (size > other.size)  return 1;
            else if (size == other.size) return 0;
            else return -1;
        }
        else return -1;
    }
}
