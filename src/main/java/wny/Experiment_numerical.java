package wny;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gurobi.gurobi.GRBException;
import wny.entities.Relation;
import wny.entities.Tuple;
import wny.solver.GurobiSolver;
import wny.util.DatabaseParser;
import wny.util.RankingMeasurer;

/** 
 * An experiment class which contains experiments in the RankHow paper
 * @author Zixuan Chen
*/
public class Experiment_numerical {
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
     * Experiment: impact on ranking problems
     * Corresponding to Section 5.2
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

        GurobiSolver gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4 / 2, 1e-4 / 2);
        
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

        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4 / 2, 0);

        start = System.currentTimeMillis();
        gs.optimize_tree(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();

        gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, 1e-4 / 2, 1e-4 / 2);

        start = System.currentTimeMillis();
        gs.optimize_tree(k, 0);
        end = System.currentTimeMillis();
        rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
        System.out.println("Error: " + rm.error(k));
        System.out.println("Running time: " + (end - start) + "ms");
        System.out.println();
    }

    /** 
     * Experiment: effectiveness of gap
     * Corresponding to Section 5.3 Table 1
    */
    public static void numerical(String data) throws GRBException, IOException {
        int count[] = new int[4], error[] = new int[4], execution_time[] = new int[4];

        System.out.println("Experiment on " + data);

        for (int i = 1; i <= 100; i++) {
            String input_file = "data/" + data + "/" + i + ".csv";
            DatabaseParser db_parser = new DatabaseParser(null);
            List<Relation> database = db_parser.parse_file(input_file);
            Relation relation = database.get(0);
            
            int[] given_ranking = relation.getRankingfromScore();

            // By default n is the relation size, m is 5, k is 5
            int n = relation.get_size();
            int m = 5;
            int k = 5;

            double epsilon1 = 1e-15, precision = 1e-3;
            GurobiSolver gs;

            System.out.println("Dataset " + i);
            System.out.println("Precision is " + precision);

            System.out.println("Original");
            System.out.println("Gap is " + epsilon1);
            gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, precision, epsilon1); 
            long start = System.currentTimeMillis();
            gs.optimize_position(k, 0);
            execution_time[0] += System.currentTimeMillis() - start;
            RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
            if (rm.error(k) != gs.getError()) {
                System.out.println("Numerical issue!");
                count[0]++;
            }
            error[0] += rm.error(k);
            System.out.println("Error for original: " + rm.error(k));
            System.out.println();
        
            System.out.println("One parameter");

            int j = 0, best = 100;
            boolean correct = false;
            // for (epsilon1 = 1e-10; epsilon1 <= 1e-1; epsilon1 *= Math.pow(10, 0.1)) {
            for (epsilon1 = 1e-10; epsilon1 <= 1e-1; epsilon1 *= 10) {
                System.out.println("Epsilon is " + epsilon1);
                gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, precision, epsilon1); 

                start = System.currentTimeMillis();
                gs.optimize_position(k, 0);
                execution_time[1] += System.currentTimeMillis() - start;
                rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
                System.out.println("Error: " + rm.error(k));
                System.out.println();

                if (correct) {
                    if (rm.error(k) < best && rm.error(k) == gs.getError()) best = rm.error(k);
                } else {
                    if (rm.error(k) == gs.getError()) {
                        correct = true;
                        best = rm.error(k);
                    } else {
                        if (rm.error(k) < best) best = rm.error(k);
                    }
                }
                j++;
            }
            System.out.println("Error for one parameter: " + best);
            if (!correct) {
                count[1]++;
                System.out.println("Numerical issue!");
            }
            error[1] += best;
            System.out.println("Number of programs: " + j);
            System.out.println();

            System.out.println("Two parameters");

            j = 0;
            best = 100;
            correct = false;
            for (epsilon1 = 1e-10; epsilon1 <= 1e-1; epsilon1 *= 10) {
                for (double epsilon2 = 1e-10; epsilon2 <= 1e-1; epsilon2 *= 10) {
                    System.out.println("Epsilon1 is " + epsilon1 + " epsilon2 is " + epsilon2);
                    gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, precision, epsilon1, epsilon2); 

                    start = System.currentTimeMillis();
                    gs.optimize_position(k, 0);
                    execution_time[2] += System.currentTimeMillis() - start;
                    rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
                    System.out.println("Error: " + rm.error(k));
                    System.out.println();

                    if (correct) {
                        if (rm.error(k) < best && rm.error(k) == gs.getError()) best = rm.error(k);
                    } else {
                        if (rm.error(k) == gs.getError()) {
                            correct = true;
                            best = rm.error(k);
                        } else {
                            if (rm.error(k) < best) best = rm.error(k);
                        }
                    }
                    j++;
                }
            }
            System.out.println("Error for two parameters: " + best);
            if (!correct) {
                count[2]++;
                System.out.println("Numerical issue!");
            }
            error[2] += best;
            System.out.println("Number of programs: " + j);
            System.out.println();

            
            System.out.println("Adaptive");
            int depth = 0;
            j = 0;
            best = 100;
            correct = false;
            double min = 1e-10;
            double max = 1e-1;
            double best_epsilon = 0; 
            double grid_size = 10;
            while (depth < 3) {
                for (epsilon1 = min; epsilon1 <= max; epsilon1 *= grid_size) {
                    System.out.println("Epsilon is " + epsilon1);
                    gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, precision, epsilon1); 

                    start = System.currentTimeMillis();
                    gs.optimize_position(k, 0);
                    execution_time[3] += System.currentTimeMillis() - start;
                    rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
                    System.out.println("Error: " + rm.error(k));
                    System.out.println();

                    if (correct) {
                        if (rm.error(k) == gs.getError()) {
                            if (rm.error(k) < best) {
                                best = rm.error(k);
                                best_epsilon = epsilon1;
                            } else if (rm.error(k) == best && epsilon1 < best_epsilon) {
                                best_epsilon = epsilon1;
                            }
                        }
                        
                    } else {
                        if (rm.error(k) == gs.getError()) {
                            correct = true;
                            best = rm.error(k);
                            best_epsilon = epsilon1;
                        } else {
                            if (rm.error(k) < best) {
                                best = rm.error(k);
                                best_epsilon = epsilon1;
                            } else if (rm.error(k) == best && epsilon1 < best_epsilon) {
                                best_epsilon = epsilon1;
                            }
                        }
                    }
                    j++;
                }
                System.out.println("Best epsilon is " + best_epsilon + " in round " + ++depth);
                max = best_epsilon;
                min = max / grid_size;
                grid_size = Math.pow(max / min, 0.1);
            }
            System.out.println("Error for adptive one parameter: " + best);
            if (!correct) {
                count[3]++;
                System.out.println("Numerical issue!");
            }
            error[3] += best;
            System.out.println("Number of programs: " + j);
            System.out.println();
        }

        for (int j = 0; j < 4; j++) {
            System.out.println(data + " data: " + count[j] + " error " + (double) error[j] / 100 + " execution time " + (double) execution_time[j] / 100);
        }
    }

    /** 
     * Experiment: impact of the gap parameter
     * Corresponding to Section 5.3 Figure 3
    */
    public static void impact(String data) throws GRBException, IOException {
        int count[] = new int[10], error[] = new int[10];

        System.out.println("Experiment on " + data);

        for (int i = 1; i <= 100; i++) {
            String input_file = "data/" + data + "/" + i + ".csv";
            DatabaseParser db_parser = new DatabaseParser(null);
            List<Relation> database = db_parser.parse_file(input_file);
            Relation relation = database.get(0);
            
            int[] given_ranking = relation.getRankingfromScore();

            // By default n is the relation size, m is 5, k is 5
            int n = relation.get_size();
            int m = 5;
            int k = 5;

            double precision = 1e-3;
            GurobiSolver gs;
    

            int j = 0;
            for (double epsilon1 = 1e-10; epsilon1 <= 1e-1; epsilon1 *= 10) {
                System.out.println("Epsilon is " + epsilon1);
                gs = new GurobiSolver(getTuples(relation.getAll(), n, m), given_ranking, precision, epsilon1); 

                gs.optimize_position(k, 0);
                RankingMeasurer rm = new RankingMeasurer(gs.getRanking(k), given_ranking);
                error[j] += rm.error(k);
                System.out.println("Error: " + rm.error(k));
                if (rm.error(k) != gs.getError()) {
                    count[j]++;
                    System.out.println("Numerical issue!");
                }
                System.out.println();

                j++;
            }
        }

        for (int j = 0; j < 10; j++) {
            System.out.println(data + " data: " + count[j] + " error " + (double) error[j] / 100);
        }
    }

    public static void main(String args[]) throws Exception 
    {
        System.out.println("Experiment: Impact on scalability");
        case_study();
        
        System.out.println("Experiment: Numerical issues and fix on CSRankings data");
        numerical("csrankings");

        System.out.println("Experiment: Numerical issues and fix on NBA data");
        numerical("per");

        System.out.println("Experiment: Impact of gap parameters");
        impact("csrankings");
    }
}