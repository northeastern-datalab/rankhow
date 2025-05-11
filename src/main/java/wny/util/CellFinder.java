package wny.util;

import java.util.ArrayList;

import org.javatuples.Pair;

import wny.entities.Cell;
import wny.entities.Tuple;

/** 
 * The algorithm to find the most promising cell, corresponding to Sec 4.2
 * @author Zixuan Chen
*/
public class CellFinder {
    private ArrayList<Tuple> tuples;
    private int num_attributes;
    private int num_tuples;
    private int[] given_ranking;
    private double gap;

    /** 
     * @param tuples All tuples of a relation
     * @param given_ranking The given ranking
     * @param gap A gap for dealing with numerical issues
    */
    public CellFinder (ArrayList<Tuple> tuples, int[] given_ranking, double gap) {
        this.tuples = tuples;
        this.given_ranking = given_ranking;
        this.gap = gap;
        num_attributes = tuples.get(0).values.length - 1;
        num_tuples = tuples.size();
    }

    /** 
     * Find the most promising cell
     * @param k
     * @param cell_size
     * @return the center of the most promising cell
    */
    public double[] find(int k, double cell_size) {
        // long start = System.currentTimeMillis();

        double[] center = new double[num_attributes];
        for (int i = 0; i < num_attributes; i++) center[i] = 0.5;
        Cell C = new Cell(center, 1.0);

        double[][] centers = C.divide((int) (1 / cell_size));

        int min = num_tuples * k * 2;
        Pair<Integer, Integer> min_p = new Pair<Integer,Integer>(-1, -1);
        Cell best_cell = C;
        for (int i = 0; i < centers.length; i++) {
            double sum_upper = 0, sum_lower = 0;
            for (int j = 0; j < num_attributes; j++) {
                sum_upper += centers[i][j] + cell_size / 2;
                sum_lower += centers[i][j] - cell_size / 2;
            }
            if (sum_upper < 1 || sum_lower > 1) continue;
            Cell c = new Cell(centers[i], cell_size);
            Pair<Integer, Integer> p = c.getBounds(tuples, given_ranking, k, gap);
            if (p.getValue0() + p.getValue1() < min) {
                min = p.getValue0() + p.getValue1();
                min_p = p;
                best_cell = c;
            }
        }

        System.out.print("Cell center: ");
        for (int i = 0; i < num_attributes; i++) {
            System.out.print(best_cell.getCenter()[i] + " ");
        }
        System.out.print("Upper bound: " + min_p.getValue0() + " ");
        System.out.print("Lower bound: " + min_p.getValue1() + " ");
        System.out.println();
        // System.out.println("Cell find time: " + (System.currentTimeMillis() - start) + "ms");
        return best_cell.getCenter();
    }
}
