package wny.solver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.javatuples.Pair;

import wny.entities.Tuple;
import wny.util.RankingMeasurer;

/** 
 * A sampling method returning the best ranking among samples
 * @author Zixuan Chen
*/
public class Sampling extends Solver {    
    /** 
     * @param tuples All tuples of a relation
     * @param given_ranking The given ranking
     * @param gap A gap for dealing with numerical issues
    */
    public Sampling(ArrayList<Tuple> tuples, int[] given_ranking, double gap) {
        super(tuples, given_ranking, gap);
    }

    /** 
     * Rank the tuples based on the weights
     * @param sample The sample weight used to rank the tuples
     * @return The ranking based on the sample
    */
    private int[] rank(int k, double[] sample) {
        int[] sample_ranking = new int[num_tuples];
        ArrayList<Pair<BigDecimal, Integer>> scores = new ArrayList<Pair<BigDecimal, Integer>>();
        for (int i = 0; i < num_tuples; i++) {
            BigDecimal score = new BigDecimal(0.0);
            for (int j = 0; j < num_attributes; j++) {
                BigDecimal value = new BigDecimal(tuples.get(i).values[j + 1]);
                score = score.add(new BigDecimal(sample[j]).multiply(value));
            }
            // System.out.println(tuples.get(i).values[0] + '\n' + score);
            Pair<BigDecimal, Integer> pair = new Pair<BigDecimal, Integer>(score, i);
            scores.add(pair);
        }

        Collections.sort(scores, Comparator.reverseOrder());

        int count = 0;
        for (int i = 0; i < num_tuples; i++) {
            if (scores.get(i).getValue1() < k) {
                sample_ranking[scores.get(i).getValue1()] = i + 1;
                BigDecimal score = scores.get(i).getValue0();
                for (int j = i - 1; j >= 0; j--) {
                    if (scores.get(j).getValue0().subtract(score).compareTo(new BigDecimal(gap).divide(new BigDecimal(2))) == -1) {
                        sample_ranking[scores.get(i).getValue1()]--;
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
        return sample_ranking;
    }

    /** 
     * @return One sample weight 
    */
    private double[] sample() {
        double[] sample = new double[num_attributes];
        Random random = new Random();

        double sum = 0;
        for (int i = 0; i < num_attributes; i++) {
            sample[i] = random.nextDouble();
            sum += sample[i];
        }
        for (int i = 0; i < num_attributes; i++) {
            sample[i] /= sum;
        }

        return sample;
    }

    /** 
     * @param start_weights The center of the cell
     * @param cell_size The side length of the cell
     * @return One sample weight in a cell
    */
    private double[] sample(double[] start_weights, double cell_size) {
        double[] sample = new double[num_attributes];
        Random random = new Random();

        double sum = 0;

        for (int i = 0; i < num_attributes - 1; i++) {
            sample[i] = start_weights[i] + (random.nextDouble() - 0.5) * cell_size;
            sum += sample[i];
        }
        sample[num_attributes - 1] = 1 - sum;

        return sample;
    }

    /** 
     * Use sampling techniques to get an approximately best ranking in a cell with a timeout limit
     * @param start_weights The center of the cell
     * @param cell_size The side length of the cell
     * @param timeout The timeout parameter, in ms
     * @param k k in top-k. 0 means full dataset
    */
    public int[] sample_rankings(double[] start_weights, double cell_size, int n, int k) {
        k = (k == 0) ? num_tuples : k;

        error = 1000000;
        int count = 0;
        int[] error_count = new int[100];
        for (int i = 0; i < 100; i++) {
            error_count[i] = 0;
        }
        while (count++ < n) {
            double[] sample = sample(start_weights, cell_size);
            int[] sample_ranking = rank(k, sample);
            RankingMeasurer rm = new RankingMeasurer(sample_ranking, given_ranking);
            if (rm.error(k) <= 100) {
                error_count[rm.error(k) - 1] += 1;
            }
        }
        return error_count;
    }

    /** 
     * Use sampling techniques to get an approximately best ranking with a timeout limit
     * @param timeout The timeout parameter, in ms
     * @param k k in top-k. 0 means full dataset
    */
    public void sample_rankings_timeout(int timeout, int k) {
        k = (k == 0) ? num_tuples : k;

        double[] weights_double = new double[num_attributes];

        error = 1000000;
        long start = System.currentTimeMillis(), end = System.currentTimeMillis();
        while (end - start < timeout) {
            double[] sample = sample();
            int[] sample_ranking = rank(k, sample);
            RankingMeasurer rm = new RankingMeasurer(sample_ranking, given_ranking);
            if (rm.error(k) < error) {
                error = rm.error(k);
                weights_double = sample;
                ranking = sample_ranking;
            }
            end = System.currentTimeMillis();
        }
        for (int i = 0; i < num_attributes; i++) {
            System.out.print(String.format("%.10f", weights_double[i]) + " ");
        }
        System.out.println();
    }
}