package wny.entities;

import java.util.ArrayList;

/** 
 * A node of the arrangement tree
 * The idea of applying the arragement tree to this context refers to the following paper:
 * Abolfazl Asudeh, H. V. Jagadish, Julia Stoyanovich, and Gautam Das. 2019. Designing Fair Ranking Schemes. In SIGMOD 2019. ACM, 1259–1276. 
 * https://doi.org/10.1145/3299869.3300079
 * @author Zixuan Chen 
*/
public class Treenode {
    private ArrayList<ArrayList<Double>> win_inequalities;
    private ArrayList<ArrayList<Double>> lose_inequalities;
    public Treenode left;
    public Treenode right;
    
    /** 
     * @param i The inequality (hyperplane) to look at 
     * @param win_inequalities The inequality set that contains all >= inequalities which need to be applied at this node
     * @param lose_inequalities The inequality set that contains all <= inequalities which need to be applied at this node
    */
    public Treenode(ArrayList<ArrayList<Double>> win_inequalities, ArrayList<ArrayList<Double>> lose_inequalities, ArrayList<ArrayList<Double>> equal_inequalities) {
        this.win_inequalities = win_inequalities;
        this.lose_inequalities = lose_inequalities;
        left = null;
        right = null;
    }

    /** 
     * @param t The tree node
    */
    public Treenode(Treenode t) {
        this.win_inequalities = new ArrayList<ArrayList<Double>>();
        this.lose_inequalities = new ArrayList<ArrayList<Double>>();
        for (int j = 0; j < t.win_inequalities.size(); j++) {
            this.win_inequalities.add(t.win_inequalities.get(j));
        }
        for (int j = 0; j < t.lose_inequalities.size(); j++) {
            this.lose_inequalities.add(t.lose_inequalities.get(j));
        }
    }

    /** 
     * @param inequality Add one inequality to the inequality set
    */
    public void addInequality(ArrayList<Double> inequality, int which) {
        if (which == 1) {
            win_inequalities.add(inequality);
        } else if (which == -1) {
            lose_inequalities.add(inequality);
        }
    }

    /** 
     * @param which Whether to get the win inequalities or lose inequalities or equal inequalities
     * @return The specific inequality set
    */
    public ArrayList<ArrayList<Double>> getInequalities(int which) {
        if (which == 1) {
            return this.win_inequalities;
        } else {
            return this.lose_inequalities;
        }
    }
}