package wny;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gurobi.GRBException;
import wny.data.Generator;
import wny.entities.Constraint;
import wny.entities.Relation;
import wny.entities.Tuple;
import wny.solver.GurobiSolver;
import wny.solver.Sampling;
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
     * Write integer results into a file
     * @param result The experimental result
     * @param filename
    */
    private static void write(int[][] result, String filename) throws IOException {
        FileWriter out = new FileWriter(filename);
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length - 1; j++) {
                out.write(result[i][j] + ",");
            }
            out.write(result[i][result[i].length - 1] + "\n");
        }
        out.close();
    }

    /** 
     * Example1 in the paper
    */
    public static void example() throws GRBException {
        String input_file = "data/mvp.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);

        int[] given_ranking = relation.getRanking();

        int[] indices = {0,3,4,5,6,7};
        relation.project(indices);

        GurobiSolver gs = new GurobiSolver(relation.getAll(), given_ranking, 1e-4);
        
        long start, end;
        RankingMeasurer rm;

        int k = 3;
        start = System.currentTimeMillis();
        gs.optimize_position(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();

        Constraint c = new Constraint(0, "raw_min", 0.1);
        start = System.currentTimeMillis();
        gs.addConstraint(c);
        gs.optimize_position(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        if (rm.error(k) != gs.getError()) {
            System.out.println("Numerical issue!");
        }
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();
    }

    /** 
     * Example3 in the paper
    */
    public static void example2() throws GRBException {
        String input_file = "data/example2.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);

        int[] given_ranking = relation.getRankingfromScore();
        int n = relation.get_size();
        int m = 2;

        GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4);
        
        long start, end;
        RankingMeasurer rm;

        int k = 0;
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
    }

    /** 
     * Case study on NBA MVP ranking
     * Corresponding to Section 6.2
    */
    public static void case_study() throws GRBException {
        String input_file = "data/mvp.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);

        int[] given_ranking = relation.getRanking();

        int[] indices = {0,3,4,5,6,7,8,9,10};
        relation.project(indices);

        int n = relation.get_size();
        int m = 8;
        int k = 13;

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

        start = System.currentTimeMillis();
        gs.optimize_tree(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();
    }

    /** 
     * Return the error and execution time of all approaches
     * Corresponding to Section 6.3 Figure 3a
    */
    public static void approaches() throws GRBException, IOException {
        String input_file = "data/per.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        
        long start, end;
        int[] given_ranking = relation.getRankingfromScore();

        // By default n is the relation size, m is 5, k is 3
        int n = relation.get_size();
        int m = 5;
        int k = 6;
        double gap = 1e-4;
        GurobiSolver gs;
        Sampling s;
        RankingMeasurer rm;

        System.out.println("k: " + k + ", n: " + n + ", m: " + m);
        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap);
        s = new Sampling(getTuples(relation.getAll(), n, m), given_ranking, gap);
        
        System.out.println("RankHow-OPT");
        start = System.currentTimeMillis();
        gs.optimize_position(k, 0);
        end = System.currentTimeMillis();
        System.out.println("Running time: " + (end - start) + "ms");
        int unit = (int) (end - start);
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        if (rm.error(k) != gs.getError()) {
            System.out.println("Numerical issue!");
        }
        System.out.println();

        System.out.println("Ordinal Regression");
        start = System.currentTimeMillis();
        gs.optimize_score(k);
        end = System.currentTimeMillis();
        long point_time = end - start;
        System.out.println("Running time: " + point_time + "ms");
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println();

        for (int i = 1; i <= 10; i++) {
            System.out.println("Sampling " + i);
            int error = 0, execution_time = 0;
            for (int j = 0; j < 3; j++) {
                start = System.currentTimeMillis();
                s.sample_rankings_timeout(unit / 5 * i, k);
                end = System.currentTimeMillis();
                execution_time += end - start;
                rm = new RankingMeasurer(s.getRanking(k), given_ranking);
                error += rm.error(k);
            }
            System.out.println("Running time: " + execution_time / 3 + "ms");
            System.out.println("Error: " + error / 3);
            System.out.println();
        }

        for (int i = 1; i <= 10; i++) {
            System.out.println("RankHow-SGD " + i);
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(k, gap, unit / 1000 / 500 * i, 1);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            gs.clearConstraint();
        }
    }

    /** 
     * OPT experiments on NBA data
     * Corresponding to Section 6.3 Figure 3b, 3c, 3d
    */
    public static void opt_nba() throws GRBException, IOException {
        String input_file = "data/per.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        
        long start, end;
        int[] given_ranking = relation.getRankingfromScore();

        // By default n is the relation size, m is 5, k is 5
        int n = relation.get_size();
        int m = 5;
        int k = 5;
        double gap = 1e-4;
        GurobiSolver gs;
        Sampling s;
        int[][] result;

        System.out.println("Vary n on NBA data");
        result = new int[3][5];
        for (int i = 1; i <= 5; i++) {
            int num_tuples = (i == 5)? relation.get_size() : i * 5000;
            System.out.println("k: " + k + ", n: " + num_tuples + ", m: " + m);
            gs = new GurobiSolver(getTuples(relation.getAll(), num_tuples, m), given_ranking, gap); 
            s = new Sampling(getTuples(relation.getAll(), num_tuples, m), given_ranking, gap);

            System.out.println("RankHow-OPT");
            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 1] = rm.error(k);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            System.out.println();
            result[1][i - 1] = rm2.error(k);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(k), given_ranking);
            System.out.println("Error: " + rm3.error(k));
            System.out.println();
            result[2][i - 1] = rm3.error(k);

            System.out.println("RankHow-SGD");
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(k, 0.1, 0, 1);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm4 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm4.error(k));
            if (rm4.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
        }
        write(result, "result/nba_error_n_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        System.out.println("Vary m on NBA data");
        result = new int[3][5];
        for (int i = 4; i <= 8; i++) {
            System.out.println("k: " + k + ", n: " + n + ", m: " + i);
            gs = new GurobiSolver(getTuples(relation.getAll(), n, i), given_ranking, gap); 
            s = new Sampling(getTuples(relation.getAll(), n, i), given_ranking, gap);

            System.out.println("RankHow");
            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 4] = rm.error(k);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            System.out.println();
            result[1][i - 4] = rm2.error(k);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(k), given_ranking);
            System.out.println("Error: " + rm3.error(k));
            System.out.println();
            result[2][i - 4] = rm3.error(k);

            System.out.println("RankHow-SGD");
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(k, 0.1, 0, 1);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm4 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm4.error(k));
            if (rm4.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
        }
        write(result, "result/nba_error_m_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        System.out.println("Vary k on NBA data");
        result = new int[3][5];
        for (int i = 2; i <= 6; i ++) {
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 
            s = new Sampling(getTuples(relation.getAll(), n, m), given_ranking, gap);
            System.out.println("k: " + i + ", n: " + n + ", m: " + m);

            System.out.println("RankHow");
            start = System.currentTimeMillis();
            gs.optimize_position(i, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(i), given_ranking);
            System.out.println("Error: " + rm.error(i));
            if (rm.error(i) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 2] = rm.error(i);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(i), given_ranking);
            System.out.println("Error: " + rm2.error(i));
            System.out.println();
            result[1][i - 2] = rm2.error(i);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(i), given_ranking);
            System.out.println("Error: " + rm3.error(i));
            System.out.println();
            result[2][i - 2] = rm3.error(i);

            System.out.println("RankHow-SGD");
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(i, 0.1, 0, 1);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm4 = new RankingMeasurer(gs.getRanking(i), given_ranking);
            System.out.println("Error: " + rm4.error(i));
            if (rm4.error(i) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
        }
        write(result, "result/nba_error_k_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /** 
     * OPT experiments on CSRankings data
     * Corresponding to Section 6.3 Figure 3e, 3f, 3g
    */
    public static void opt_csrankings() throws GRBException, IOException {
        String input_file = "data/csrankings.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        
        long start, end;
        int[] given_ranking = relation.getRankingfromScore();

        // By default n is the relation size, m is 5, k is 5
        int n = relation.get_size();
        int m = 5;
        int k = 5;
        double gap = 1e-2;
        GurobiSolver gs;
        Sampling s;
        int[][] result;

        System.out.println("Vary n on CSRankings data");
        result = new int[3][7];
        for (int i = 1; i <= 7; i++) {
            int num_tuples = (i == 7)? relation.get_size() : i * 100;
            System.out.println("k: " + k + ", n: " + num_tuples + ", m: " + m);
            gs = new GurobiSolver(getTuples(relation.getAll(), num_tuples, m), given_ranking, gap); 
            s = new Sampling(getTuples(relation.getAll(), num_tuples, m), given_ranking, gap);

            System.out.println("RankHow");
            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 1] = rm.error(k);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            System.out.println();
            result[1][i - 1] = rm2.error(k);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(k), given_ranking);
            System.out.println("Error: " + rm3.error(k));
            System.out.println();
            result[2][i - 1] = rm3.error(k);
        }
        write(result, "result/csrankings_error_n_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        System.out.println("Vary m on CSRankings data");
        result = new int[3][6];
        for (int i = 1; i <= 6; i++) {
            int num_attributes = (i == 6)? 27 : i * 5;
            System.out.println("k: " + k + ", n: " + n + ", m: " + num_attributes);
            gs = new GurobiSolver(getTuples(relation.getAll(), n, num_attributes), given_ranking, gap); 
            s = new Sampling(getTuples(relation.getAll(), n, num_attributes), given_ranking, gap);

            System.out.println("RankHow");
            start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 1] = rm.error(k);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm2.error(k));
            System.out.println();
            result[1][i - 1] = rm2.error(k);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, k);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(k), given_ranking);
            System.out.println("Error: " + rm3.error(k));
            System.out.println();
            result[2][i - 1] = rm3.error(k);
        }
        write(result, "result/csrankings_error_m_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, gap); 
        s = new Sampling(getTuples(relation.getAll(), n, m), given_ranking, gap);
        System.out.println("Vary k on CSRankings data");
        result = new int[3][5];
        for (int i = 5; i <= 25; i += 5) {
            System.out.println("k: " + i + ", n: " + n + ", m: " + m);

            System.out.println("RankHow");
            start = System.currentTimeMillis();
            gs.optimize_position(i, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(i), given_ranking);
            System.out.println("Error: " + rm.error(i));
            if (rm.error(i) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i / 5 - 1] = rm.error(i);
            int sampling_timeout = (int) (end - start);

            System.out.println("Ordinal Regression");
            start = System.currentTimeMillis();
            gs.optimize_score(i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm2 = new RankingMeasurer(gs.getRanking(i), given_ranking);
            System.out.println("Error: " + rm2.error(i));
            System.out.println();
            result[1][i / 5 - 1] = rm2.error(i);

            System.out.println("Sampling");
            start = System.currentTimeMillis();
            s.sample_rankings_timeout(sampling_timeout, i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm3 = new RankingMeasurer(s.getRanking(i), given_ranking);
            System.out.println("Error: " + rm3.error(i));
            System.out.println();
            result[2][i / 5 - 1] = rm3.error(i);
        }
        write(result, "result/csrankings_error_k_opt.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /** 
     * Experiments to verify the correctness of our numerical-correction approach
     * Corresponding to Section 6.4
    */
    public static void numerical() throws GRBException, IOException {
        String input_file = "data/per.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        
        long start, end;
        int[] given_ranking = relation.getRankingfromScore();

        int n = 10;
        int m = 8;
        int[][] result = new int[5][10];

        System.out.println("Numerical issue correction experiments with k from 1 to 10");
        GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4);
        GurobiSolver gs2 = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-10);
        for (int i = 1; i <= 10; i++) {
            System.out.println("k: " + i + ", n: " + n + ", m: " + m);
            result[0][i - 1] = i;

            System.out.println("OPT with numerical corrections for position error");
            start = System.currentTimeMillis();
            gs.optimize_position(i, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(i), given_ranking);
            result[1][i - 1] = rm.error(i);
            System.out.println("Error: " + result[1][i - 1]);
            if (result[1][i - 1] != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();

            System.out.println("OPT without numerical corrections for position error");
            start = System.currentTimeMillis();
            gs2.optimize_position(i, 0);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            rm = new RankingMeasurer(gs2.getRanking(i), given_ranking);
            result[2][i - 1] = rm.error(i);
            System.out.println("Error: " + result[2][i - 1]);
            if (result[2][i - 1] != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();

            System.out.println("OrdinalRegression with numerical corrections for position error");
            start = System.currentTimeMillis();
            gs.optimize_score(i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            rm = new RankingMeasurer(gs.getRanking(i), given_ranking);
            result[3][i - 1] = rm.error(i);
            System.out.println("Error: " + result[3][i - 1]);
            if (result[3][i - 1] != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();

            System.out.println("OrdinalRegression without numerical corrections for position error");
            start = System.currentTimeMillis();
            gs2.optimize_score(i);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            rm = new RankingMeasurer(gs2.getRanking(i), given_ranking);
            result[4][i - 1] = rm.error(i);
            System.out.println("Error: " + result[4][i - 1]);
            if (result[4][i - 1] != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
        }
        write(result, "result/numerical_issues.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /** 
     * Experiments to explore the trade-off between cell_size and approximation quality/execution time
     * Corresponding to Sec 6.5 Figure 3i
    */
    public static void cell_size() throws GRBException, IOException {
        String input_file = "data/per.csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        
        long start, end;
        int[] given_ranking = relation.getRankingfromScore();

        int n = relation.get_size();
        int m = 8;
        int k = 10;
        int[][] result;

        System.out.println("Vary cell size for approximation experiments on NBA data");
        result = new int[2][10];

        for (int i = 1; i <= 10; i++) {
            GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4); 
            double cell_size = i * 0.001;
            System.out.println("k: " + k + ", n: " + n + ", m: " + m + ", cell size: " + cell_size);

            System.out.println("RankHow-SGD");
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(k, cell_size, 0, 1);
            end = System.currentTimeMillis();
            System.out.println("Running time: " + (end - start) + "ms");
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
            }
            System.out.println();
            result[0][i - 1] = rm.error(k);
            result[1][i - 1] = (int) (end - start);
        }
        write(result, "result/cell_size.csv");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /** 
     * Experiments to verify the scalability of Sym-GD
     * Corresponding to Section 6.6 Figure 3j, 3k, 3l
    */
    public static void distribution(String distribution) throws GRBException, IOException {
        // Generator g = new Generator(1000000, 5, "data/" + distribution + ".csv");
        // g.create(distribution);

        String input_file = "data/" + distribution + ".csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);
        Relation relation = database.get(0);
        ArrayList<Tuple> tuples = relation.getAll();

        class ranking_function implements Comparator<Tuple> {
            public int compare(Tuple r, Tuple s) {
                double score1 = Math.pow(Double.valueOf(r.values[1]), 3) + Math.pow(Double.valueOf(r.values[2]), 3) + Math.pow(Double.valueOf(r.values[3]), 3) + Math.pow(Double.valueOf(r.values[4]), 3) + Math.pow(Double.valueOf(r.values[5]), 3);
                double score2 = Math.pow(Double.valueOf(s.values[1]), 3) + Math.pow(Double.valueOf(s.values[2]), 3) + Math.pow(Double.valueOf(s.values[3]), 3) + Math.pow(Double.valueOf(s.values[4]), 3) + Math.pow(Double.valueOf(s.values[5]), 3);

                if (score1 < score2) return -1;
                else if (score1 > score2) return 1;
                else return 0;
            }
        }
        Collections.sort(tuples, new ranking_function());
        Collections.reverse(tuples);

        long start, end;
        RankingMeasurer rm;

        System.out.println("Scalability");
        for (int k = 5; k <= 25; k += 5) {
            System.out.println("k: " + k);
            int[] given_ranking = new int[1000000];
            for (int i = 0; i < k; i++) {
                given_ranking[i] = i + 1;
            }
            for (int i = k; i < 1000000; i++) {
                given_ranking[i] = k + 1;
            }
            
            GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), 1000000, 5), given_ranking, 1e-5);
            start = System.currentTimeMillis();
            gs.optimize_gradient_descent(k, 0.01, 0, 1);
            end = System.currentTimeMillis();
            rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            System.out.println("Error: " + rm.error(k));
            System.out.println("Running time: " + (end - start) + "ms");
        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /** 
     * Experiments to verify the generalizability of Sym-GD
     * Corresponding to Section 6.6 Figure 3m, 3n, 3o
    */
    public static void function(String distribution) throws GRBException, IOException {
        Generator g = new Generator(1000000, 5, "data/" + distribution + ".csv");
        g.create(distribution);

        String input_file = "data/" + distribution + ".csv";
        DatabaseParser db_parser = new DatabaseParser(null);
        List<Relation> database = db_parser.parse_file(input_file);

        int k = 10, n = 1000000;
        long start, end;
        RankingMeasurer rm;
        
        for (int l = 1; l <= 2; l++) {
            System.out.println("Attribute derivation with exponent " + l);
            Relation relation = database.get(0);
            start = System.currentTimeMillis();
            relation.derive(l);
            end = System.currentTimeMillis();
            System.out.println("Attribute derivation time: " + (end - start) + "ms");
            ArrayList<Tuple> tuples = relation.getAll();

            for (int i = 1; i <= 5; i++) {
                // A1 ^ i + A2 ^ i
                int e = i;
                class ranking_function implements Comparator<Tuple> {
                    public int compare(Tuple r, Tuple s) {
                        double score1 = Math.pow(Double.valueOf(r.values[1]), e) + Math.pow(Double.valueOf(r.values[2]), e) + Math.pow(Double.valueOf(r.values[3]), e) + Math.pow(Double.valueOf(r.values[4]), e) + Math.pow(Double.valueOf(r.values[5]), e);
                        double score2 = Math.pow(Double.valueOf(s.values[1]), e) + Math.pow(Double.valueOf(s.values[2]), e) + Math.pow(Double.valueOf(s.values[3]), e) + Math.pow(Double.valueOf(s.values[4]), e) + Math.pow(Double.valueOf(s.values[5]), e);

                        if (score1 < score2) return -1;
                        else if (score1 > score2) return 1;
                        else return 0;
                    }
                }

                Collections.sort(tuples, new ranking_function());
                Collections.reverse(tuples);
                
                System.out.println("function: A1 ^ " + i + " + A2 ^ " + i);
                int[] given_ranking = new int[n];
                for (int j = 0; j < k; j++) {
                    given_ranking[j] = j + 1;
                    System.out.println(Math.pow(Double.valueOf(tuples.get(j).values[1]), e) + Math.pow(Double.valueOf(tuples.get(j).values[2]), e) + Math.pow(Double.valueOf(tuples.get(j).values[3]), e) + Math.pow(Double.valueOf(tuples.get(j).values[4]), e) + Math.pow(Double.valueOf(tuples.get(j).values[5]), e));
                }
                for (int j = k; j < n; j++) {
                    given_ranking[j] = 11;
                }
                
                GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), 1000000, 5 * l), given_ranking, 1e-5);
                start = System.currentTimeMillis();
                gs.optimize_gradient_descent(k, 0.01, 0, 1);
                end = System.currentTimeMillis();
                rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
                System.out.println("Error: " + rm.error(k));
                System.out.println("Running time: " + (end - start) + "ms");
            }
        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    public static void main(String args[]) throws Exception 
    {
        // System.out.println("Example 1");
        // example();
        // System.out.println("Example 2");
        // example2();
        System.out.println("Case study");
        case_study();
        // System.out.println("Approaches");
        // approaches();
        // System.out.println("NBA OPT");
        // opt_nba();
        // System.out.println("CSRankings OPT");
        // opt_csrankings();
        // System.out.println("Numerical issues");
        // numerical();
        // System.out.println("Cell size");
        // cell_size();
        // System.out.println("Function uniform");
        // function("uniform");
        // System.out.println("Function correlated");
        // function("correlated");
        // System.out.println("Function anti-correlated");
        // function("anti-correlated");
        // System.out.println("Scalability uniform");
        // distribution("uniform");
        // System.out.println("Scalability correlated");
        // distribution("correlated");
        // System.out.println("Scalability anti-correlated");
        // distribution("anti-correlated");
    }
}