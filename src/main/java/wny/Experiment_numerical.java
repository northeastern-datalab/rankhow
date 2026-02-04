package wny;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gurobi.GRBException;
import wny.entities.Relation;
import wny.entities.Tuple;
import wny.solver.GurobiSolver;
import wny.util.DatabaseParser;
import wny.util.RankingMeasurer;

/** 
 * An experiment class which contains experiments in the RankHow paper
 * @author Zixuan Chen
*/
public class Experiment {
     /** 
     * Get tuples to rank from a relation
     * @param relation All original tuples in the given relation
     * @param num_tuple Number of tuples in the ranking
     * @param num_attribute Number of ranking attributes
     * @return Tuples to rank
    */
    private static ArrayList<Tuple> getTuples(ArrayList<Tuple> relation, int num_tuple, int num_attribute) {
        ArrayList<Tuple> tuples = new ArrayList<Tuple>();
        for (int i = 0; i < num_tuple; i++) {
            Tuple tuple = new Tuple(new String[num_attribute + 1]);
            for (int j = 0; j <= num_attribute; j++) {
                tuple.values[j] = relation.get(i).values[j];
            }
            tuples.add(tuple);
        }
        return tuples;
    }
    
    /** 
     * Experiment: how frequent are numerical issues and fix
     * Corresponding to Sections 3.2 and 4.4
    */
    public static void numerical() throws GRBException, IOException {
        int count1 = 0, count2 = 0, error1 = 0, error2 = 0, execution_time1 = 0, execution_time2 = 0, perfect1 = 0, perfect2 = 0, min = 100, max = 0;

        for (int i = 1; i <= 100; i++) {
            String input_file = "data/per/" + i + ".csv";
            DatabaseParser db_parser = new DatabaseParser(null);
            List<Relation> database = db_parser.parse_file(input_file);
            Relation relation = database.get(0);
            
            int[] given_ranking = relation.getRankingfromScore();

            // By default n is the relation size, m is 5, k is 5
            int n = relation.get_size();
            int m = 5;
            int k = 5;
            double gap = 5 * 1e-4;
            GurobiSolver gs;

            System.out.println("k: " + k + ", n: " + n + ", m: " + m);
            System.out.println("-");
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 

            long start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            execution_time1 += System.currentTimeMillis() - start;
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
                count1++;
            }
            error1 += rm.error(k);
            if (rm.error(k) == 0) perfect1++;
            System.out.println();

            System.out.println("+");
            gap = 1e-15;
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 

            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            execution_time2 += System.currentTimeMillis() - start;
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            if (rm2.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
                count2++;
            }
            error2 += rm2.error(k);
            if (rm2.error(k) == 0) perfect2++;
            System.out.println();

            int difference = rm2.error(k) - rm.error(k);
            if (difference < min) {
                min = difference;
            }
            if (difference > max) {
                max = difference;
            }
        }
        
        System.out.println("NBA data: RankHow+ " + count1 + " error " + error1 + " execution time " + execution_time1 + " perfect ranking: " + perfect1);
        System.out.println("NBA data: RankHow- " + count2 + " error " + error2 + " execution time " + execution_time2 + " perfect ranking: " + perfect2);
        System.out.println("Extra error: " + min + ' ' + (double) (error2 - error1) / 100 + ' ' + max);
        System.out.println("Execution time: " + (double) execution_time2 / execution_time1);

        count1 = 0;
        count2 = 0;
        error1 = 0;
        error2 = 0;
        execution_time1 = 0;
        execution_time2 = 0;
        perfect1 = 0;
        perfect2 = 0;

        for (int i = 1; i <= 100; i++) {
            String input_file = "data/csrankings/" + i + ".csv";
            DatabaseParser db_parser = new DatabaseParser(null);
            List<Relation> database = db_parser.parse_file(input_file);
            Relation relation = database.get(0);
            
            int[] given_ranking = relation.getRankingfromScore();

            // By default n is the relation size, m is 5, k is 5
            int n = relation.get_size();
            int m = 5;
            int k = 5;
            double gap = 1e-2;
            GurobiSolver gs;

            System.out.println("k: " + k + ", n: " + n + ", m: " + m);
            System.out.println("-");
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 

            long start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            execution_time1 += System.currentTimeMillis() - start;
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
                count1++;
            }
            if (rm.error(k) == 0) perfect1++;
            error1 += rm.error(k);
            System.out.println();

            System.out.println("+");
            gap = 1e-15;
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 

            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            execution_time2 += System.currentTimeMillis() - start;
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            if (rm2.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
                count2++;
            }
            error2 += rm2.error(k);
            if (rm2.error(k) == 0) perfect2++;
            System.out.println();

            int difference = rm2.error(k) - rm.error(k);
            if (difference < min) {
                min = difference;
            }
            if (difference > max) {
                max = difference;
            }
        }
        
        System.out.println("CSRankings data: RankHow+ " + count1 + " error " + error1 + " execution time " + execution_time1 + " perfect ranking: " + perfect1);
        System.out.println("CSRankings data: RankHow- " + count2 + " error " + error2 + " execution time " + execution_time2 + " perfect ranking: " + perfect2);
        System.out.println("Extra error: " + min + ' ' + (double) (error2 - error1) / 100 + ' ' + max);
        System.out.println("Execution time: " + (double) execution_time2 / execution_time1);
    }

    /** 
     * Experiment: impact on scalability
     * Corresponding to Section 3.2
    */
    public static void case_study() throws GRBException {
        String input_file = "data/mvp_small.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);

        int[] given_ranking = relation.getRanking();

        int[] indices = {0,3,4,5,6,7,8,9,10};
        relation.project(indices);

        int n = relation.get_size();
        int m = 8;
        int k = 8;

        GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4);
        
        long start, end;
        RankingMeasurer rm;

        start = System.currentTimeMillis();
        gs.optimize_position(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        if (rm.error(k) != gs.getError()) {
            System.out.println("Numerical issue!");
        }
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();

        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 0);

        start = System.currentTimeMillis();
        gs.optimize_tree(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();

        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4);

        start = System.currentTimeMillis();
        gs.optimize_tree(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();
    }

    public static void main(String args[]) throws Exception 
    {
        System.out.println("Experiment: Numerical issues and fix");
        numerical();

        System.out.println("Experiment: Impact on scalability");
        case_study();
    }
}