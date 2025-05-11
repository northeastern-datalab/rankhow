package wny.util;

/** 
 * A class that measures the ranking quality
 * @author Zixuan Chen
*/
public class RankingMeasurer
{
    private int[] ranking;
    private int[] given_ranking;
    private int num_tuples;

    public RankingMeasurer(int[] ranking) {
        this.ranking = ranking;
        num_tuples = ranking.length;
        given_ranking = new int[num_tuples];
        for (int i = 0; i < num_tuples; i++) {
            given_ranking[i] = i + 1;
        }
    }

    public RankingMeasurer(int[] ranking, int[] given_ranking) {
        this.ranking = ranking;
        num_tuples = ranking.length;
        this.given_ranking = given_ranking;
    }
    
    /** 
     * @param k k in top-k. 0 means full dataset
     * @return The absolute error between the correct ranking and the output ranking
     */
    public int error(int k) {
        k = (k == 0) ? num_tuples : k;
        int error = 0;
        for (int i = 0; i < k; i++) {
            error += Math.abs(ranking[i] - given_ranking[i]);
        }
        return error;
    }
}