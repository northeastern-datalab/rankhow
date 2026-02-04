package wny.solver;

import java.math.BigDecimal;
import java.util.ArrayList;

import gurobi.*;

import wny.entities.Constraint;
import wny.entities.Treenode;
import wny.entities.Tuple;
import wny.util.CellFinder;
import wny.util.RankingMeasurer;

/** 
 * An optimizer using the gurobi library
 * @author Zixuan Chen
*/
public class GurobiSolver extends Solver {
    private GRBEnv env;
    private GRBModel model;

    private double epsilon1;
    private BigDecimal[] best_weights;
    private long solver_time;
    private int program_count;
    private int node_count;
    private int fake_leaf_count;
    private int leaf_count;
    
    /** 
     * @param tuples All tuples of a relation
     * @param given_ranking
     * @param gap A gap for dealing with numerical issues
    */
    public GurobiSolver(ArrayList<Tuple> tuples, int[] given_ranking, double gap) {
        super(tuples, given_ranking, gap);
        epsilon1 = gap;
    }
    
    /** 
     * Set up the environment and model
     * @timeout The time limit for the model
    */
    private void setup(int timeout) throws GRBException {
        env = new GRBEnv(true);
        env.set("logFile","gurobi.log");
        env.set(GRB.IntParam.OutputFlag, 0);
        env.start();

        model = new GRBModel(env);
        model.set(GRB.IntParam.LogToConsole, 0);
        if (timeout != 0) { 
            model.set(GRB.DoubleParam.TimeLimit, timeout);
        }

        weights = new BigDecimal[num_attributes];
    }
    
    /** 
     * Dispose the environment and model
    */
    private void close() throws GRBException {
        model.dispose();
        env.dispose();
    }

    /** 
     * Apply flexible constraints
     * To constrain the weight to be larger or smaller than a value, simply add the value as the constraint
     * To constrain the standard weight to be larger or smaller than a value, 
     * the ratio of the standard weight on given attribute to the sum standard weight is constrained to be larger or smaller than the value 
     * The reason why we cannot directly constrain the product of the original weight and the standard deviation to be larger or smaller than the value is
     * the standard weight shown needs to be adjusted to make sure the standard weights sum to 1 
     * so we can only constrain the ratio (also the weight after adjustment) instead of the weight before adjustment
     * @param X All variables of the problem (lower bounds and upper bounds)
     * @throws GRBException
    */
    private void apply_constraints(GRBVar[] W) throws GRBException {
        for (Constraint c:constraints) {
            if (c.type == "min") {
                GRBLinExpr sum_expr = new GRBLinExpr();
                for (int i = 0; i < num_attributes; i++) { 
                    sum_expr.addTerm(standard_deviation[i], W[i]);
                }
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(standard_deviation[c.attribute] / c.value, W[c.attribute]);
                model.addConstr(expr, GRB.GREATER_EQUAL, sum_expr, "min" + c.attribute);
            } else if (c.type == "max") {
                GRBLinExpr sum_expr = new GRBLinExpr();
                for (int i = 0; i < num_attributes; i++) { 
                    sum_expr.addTerm(standard_deviation[i], W[i]);
                }
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(standard_deviation[c.attribute] / c.value, W[c.attribute]);
                model.addConstr(expr, GRB.LESS_EQUAL, sum_expr, "max" + c.attribute);
            } else if (c.type == "raw_min") {
                model.addConstr(W[c.attribute], GRB.GREATER_EQUAL, c.value, "min" + c.attribute);
            } else if (c.type == "raw_max") {
                model.addConstr(W[c.attribute], GRB.LESS_EQUAL, c.value, "max" + c.attribute);
            } else if (c.type == "multiple") {
                GRBLinExpr expr = new GRBLinExpr();
                for (int i = 0; i < num_attributes; i++) {
                    expr.addTerm(c.weightsweights[i], W[i]);
                }
                model.addConstr(expr, GRB.LESS_EQUAL, c.value, "multiple" + c.value);
            }
        }
    }
    
    /** 
     * Print the result for an OPT solution
     * @param status the solution status of the model
     * @param num_solution the number of solutions found by the model
     * @param W weight variables in the gurobi solver
     * @throws GRBException 
    */
    private void print(int status, int num_solution, GRBVar[] W) throws GRBException {
        if (status == 2) {
            System.out.println("OPTIMAL");
        } else if (status == 3) {
            System.out.println("INFEASIBLE");
            return;
        } else if (status == 9) {
            System.out.println("TIMEOUT");
            if (num_solution == 0) {
                return;
            }
        }
        System.out.println("Optimization goal: " + model.get(GRB.DoubleAttr.ObjVal));
        System.out.print("Weight: ");
        double[] weights_double = new double[num_attributes];
        for (int i = 0; i < W.length; i++) {
            weights_double[i] = W[i].get(GRB.DoubleAttr.X);
            System.out.print(String.format("%.10f", weights_double[i]) + " ");
            weights[i] = new BigDecimal(weights_double[i]);
        }
        System.out.println();
        System.out.print("Standard weight:");
        double weight_sum = 0;
        double[] standard_weights = new double[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            standard_weights[i] = weights_double[i] * standard_deviation[i];
            weight_sum += standard_weights[i];
        }
        for (int i = 0; i < num_attributes; i++) {
            standard_weights[i] /= weight_sum;
            System.out.print(String.format("%.10f", standard_weights[i] ) + " ");
        }
        System.out.println();
    }

    /** 
     * Verify the result by checking whether all indicators in the solver are correct
     * @throws GRBException 
    */
    private void verify() throws GRBException {
        BigDecimal[] scores = new BigDecimal[num_tuples];
        
        for (int i = 0; i < num_tuples; i++) {
            scores[i] = new BigDecimal(0.0);
            for (int j = 0; j < num_attributes; j++) {
                BigDecimal value = new BigDecimal(Double.valueOf(tuples.get(i).values[j + 1]));
                scores[i] = scores[i].add(weights[j].multiply(value));
            }
        }

        for (GRBVar v : model.getVars()) {
            if (v.get(GRB.StringAttr.VarName).indexOf("indicator") != -1) {
                String[] pair = v.get(GRB.StringAttr.VarName).substring(9).split(" ");
                int i = Integer.parseInt(pair[0]);
                int j = Integer.parseInt(pair[1]);

                boolean solver_indicator = v.get(GRB.DoubleAttr.X) > 0.99 ? true : false;
                boolean verified_indicator = scores[j].subtract(scores[i]).compareTo(new BigDecimal(gap).divide(new BigDecimal(2))) > 0.99 ? true : false;
                if (solver_indicator != verified_indicator) {
                    System.out.println("Numerical issues found in indicator verification");
                }
            }
        }
    }

    /** 
     * Solve the ranking explanation optimization (OPT) problem for minimum individual position error
     * When the individual position error is required to be zero, the problem becomes the ranking explanation satisfiability (SAT) problem
     * Corresponding to Sec 3.2 in the paper
     * @param k k in top-k. 0 means full dataset
     * @param timeout The timeout parameter for the solver, in ms
     * @return Whether the solver gets an optimal result or an infeasible result
     * @throws GRBException
    */
    public void optimize_position(int k, int timeout) throws GRBException {
        setup(timeout);

        k = (k == 0) ? num_tuples : k;

        GRBLinExpr expr = new GRBLinExpr();
        GRBVar W[] = new GRBVar[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            W[i] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "w" + String.valueOf(i));
            expr.addTerm(1.0, W[i]);
        }
        model.addConstr(expr, GRB.EQUAL, 1.0, "one");
        GRBVar difference[] = new GRBVar[k];
        GRBVar position_error[] = new GRBVar[k];
        for (int i = 0; i < k; i++) {
            difference[i] = model.addVar(-num_tuples, num_tuples, 0.0, GRB.INTEGER, "difference" + String.valueOf(i));
            position_error[i] = model.addVar(0.0, num_tuples, 0.0, GRB.INTEGER, "position_error" + String.valueOf(i));
            model.addGenConstrAbs(position_error[i], difference[i], "abs" + String.valueOf(i));
        }

        for (int i = 0; i < k; i++) {
            int num_dominatees = 0, num_dominators = 0;
            GRBLinExpr sum_expr = new GRBLinExpr();
            for (int j = 0; j < num_tuples; j++) {
                if (i != j) {
                    int comparison = tuples.get(i).isDominating(tuples.get(j), gap);
                    if (comparison == 0) {
                        GRBVar indicator = model.addVar(0, 1, 0.0, GRB.BINARY, "indicator" + i + ' ' + j);
                        expr = new GRBLinExpr();
                        for (int l = 0; l < num_attributes; l++) {
                            expr.addTerm(Double.valueOf(tuples.get(j).values[l + 1]) - Double.valueOf(tuples.get(i).values[l + 1]), W[l]);
                        }
                        model.addGenConstrIndicator(indicator, 1, expr, GRB.GREATER_EQUAL, epsilon1, "win_inequality" + i + ' ' + j);
                        model.addGenConstrIndicator(indicator, 0, expr, GRB.LESS_EQUAL, 0, "lose_inequality" + i + ' ' + j);
                        sum_expr.addTerm(1, indicator);
                    } else if (comparison == 1) {
                        num_dominatees++;
                    } else if (comparison == -1) {
                        num_dominators++;
                    }
                }
            }
            sum_expr.addConstant(num_dominators - (given_ranking[i] - 1));
            model.addConstr(sum_expr, GRB.EQUAL, difference[i], "getDifference" + i);
        }

        apply_constraints(W);

        expr = new GRBLinExpr();
        for (int i = 0; i < k; i++) {
            expr.addTerm(1, position_error[i]);
        }
        
        model.setObjective(expr, GRB.MINIMIZE);

        model.optimize();

        int status = model.get(GRB.IntAttr.Status);
        int num_solution = model.get(GRB.IntParam.SolutionNumber);
        print(status, num_solution, W);
        if (status == 2 || (status == 9 && num_solution != 0)) {
            error = (int) Math.round(model.get(GRB.DoubleAttr.ObjVal));
            verify();
            rank(k);
        }
        close();
    }

    /** 
     * Solve the ranking explanation optimization (OPT) problem for minimum pairwise score error
     * In the original paper, another weight constraint is used (commented lines) which results in very small weights
     * Here we use the same weight constraint as above which makes sure the weight sum equals to 1 
     * @param k k in top-k. 0 means full dataset    
     * @throws GRBException
    */
    public void optimize_score(int k) throws GRBException {
        setup(0);

        k = (k == 0) ? num_tuples : k;

        GRBLinExpr expr = new GRBLinExpr();
        GRBVar W[] = new GRBVar[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            W[i] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "w" + String.valueOf(i));
            expr.addTerm(1.0, W[i]);
        }
        model.addConstr(expr, GRB.EQUAL, 1.0, "one");

        GRBLinExpr objective = new GRBLinExpr(); 
        // GRBLinExpr normalization_expr = new GRBLinExpr();
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < num_tuples; j++) {
                if (given_ranking[i] < given_ranking[j]) {
                    GRBVar penalty = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "penalty" + i + ' ' + j);

                    expr = new GRBLinExpr();
                    for (int l = 0; l < num_attributes; l++) {
                        expr.addTerm(Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]), W[l]);
                        // normalization_expr.addTerm(Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]), W[l]);
                    }
                
                    expr.addTerm(1, penalty);
                    model.addConstr(expr, GRB.GREATER_EQUAL, epsilon1, "inequality" + i + ' ' + j);
                    objective.addTerm(1, penalty);
                } else if (given_ranking[i] == given_ranking[j]) {
                    GRBVar penalty = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "penalty" + i + ' ' + j);
                    GRBVar penalty2 = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "penalty" + j + ' ' + i);

                    expr = new GRBLinExpr();
                    for (int l = 0; l < num_attributes; l++) {
                        expr.addTerm(Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]), W[l]);
                        // normalization_expr.addTerm(Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]), W[l]);
                    }

                    expr.addTerm(1, penalty);
                    model.addConstr(expr, GRB.GREATER_EQUAL, 0, "inequality" + i + ' ' + j);
                    objective.addTerm(1, penalty);

                    expr = new GRBLinExpr();
                    for (int l = 0; l < num_attributes; l++) {
                        expr.addTerm(Double.valueOf(tuples.get(j).values[l + 1]) - Double.valueOf(tuples.get(i).values[l + 1]), W[l]);
                        // normalization_expr.addTerm(Double.valueOf(tuples.get(j).values[l + 1]) - Double.valueOf(tuples.get(i).values[l + 1]), W[l]);
                    }

                    expr.addTerm(1, penalty2);
                    model.addConstr(expr, GRB.GREATER_EQUAL, 0, "inequality" + j + ' ' + i);
                    objective.addTerm(1, penalty2);
                }
            }
        }

        apply_constraints(W);

        model.setObjective(objective, GRB.MINIMIZE);

        model.optimize();

        int status = model.get(GRB.IntAttr.Status);
        int num_solution = model.get(GRB.IntParam.SolutionNumber);
        print(status, num_solution, W);
        close();
        if (status == 2 || (status == 9 && num_solution != 0)) {
            rank(k);
        }
    }

    /** 
     * Solve the ranking explanation optimization (OPT) problem for minimum individual position error using symbolic gradient descent
     * @param k
     * @param cell_size
     * @param timeout The timeout parameter for the solver, in ms, 0 for no timeout given
     * If a timeout parameter is given, the algorithm increases the size of the cell when using current cell size does not improve the error any more;
     * otherwise the algorithm stops when gradient descent does not improve the error any more
     * @param cell_selection The method to select a cell. 1 for Ordinal Regression, 2 for cell bounds
     * @return Whether the solver gets an optimal result or an infeasible result
     * @throws GRBException
    */
    public void optimize_gradient_descent(int k, double cell_size, int timeout, int cell_selection) throws GRBException {
        long start = System.currentTimeMillis();
        double point[] = new double[num_attributes];
        if (cell_selection == 1) {
            optimize_score(k);
            point = getWeights();
        } else if (cell_selection == 2) {
            CellFinder cf = new CellFinder(tuples, given_ranking, gap);
            point = cf.find(k, cell_size);
        }
        build_cell(point, cell_size);

        int e = num_tuples * k;
        int step = 0;

        if (timeout == 0) {
            while (true) {
                optimize_position(k, timeout);
                if (error >= e) {
                    System.out.println("Number of steps: " + step);
                    error = e;
                    break;
                } else if (error == 0) {
                    System.out.println("Number of steps: " + step);
                    break;
                } else {
                    e = error;
                    step++;
                    clearConstraint();
                    build_cell(getWeights(), cell_size);
                }
            }
        } else {
            while (true) {
                optimize_position(k, timeout - (int) (System.currentTimeMillis() - start) / 1000);
                if (error >= e) {
                    if (System.currentTimeMillis() - start > timeout * 1000) {
                        System.out.println("Step: " + step);
                        error = e;
                        break;
                    }
                    clearConstraint();
                    cell_size *= 2;
                    if (cell_size > 1) break;
                    System.out.println("Updated Cell size: " + cell_size);
                    build_cell(getWeights(), cell_size);
                } else if (error == 0) {
                    System.out.println("Number of steps: " + step);
                    break;
                } else {
                    e = error;
                    step++;
                    if (System.currentTimeMillis() - start > timeout * 1000) {
                        System.out.println("Step: " + step);
                        break;
                    }
                    clearConstraint();
                    build_cell(getWeights(), cell_size);
                }
            }
        }
    }

    /** 
     * Add a hyperplane to the tree
     * @param n The node to check intersection relationship with the hyperplane
     * @param inequality The hyperplane
     * @param k
    */
    private void add_hyperplane(Treenode n, ArrayList<Double> inequality, int k) throws GRBException {
        long start = System.currentTimeMillis();
        model = new GRBModel(env);
        model.set(GRB.IntParam.LogToConsole, 0);
        GRBLinExpr expr = new GRBLinExpr();
        GRBVar W[] = new GRBVar[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            W[i] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "w" + String.valueOf(i));
            expr.addTerm(1.0, W[i]);
        }
        model.addConstr(expr, GRB.EQUAL, 1.0, "one");

        ArrayList<ArrayList<Double>> win_inequalities = n.getInequalities(1);
        ArrayList<ArrayList<Double>> lose_inequalities = n.getInequalities(-1);

        for (int i = 0; i < win_inequalities.size(); i++) {
            expr = new GRBLinExpr();
            for (int j = 0; j < num_attributes; j++) {
                Double c = win_inequalities.get(i).get(j);
                expr.addTerm(c, W[j]);
            }
            model.addConstr(expr, GRB.GREATER_EQUAL, epsilon1, "win_constraint" + i);
        }
        for (int i = 0; i < lose_inequalities.size(); i++) {
            expr = new GRBLinExpr();
            for (int j = 0; j < num_attributes; j++) {
                Double c = lose_inequalities.get(i).get(j);
                expr.addTerm(c, W[j]);
            }
            model.addConstr(expr, GRB.LESS_EQUAL, -epsilon1, "lose_constraint" + i);
        }

        expr = new GRBLinExpr();
        for (int j = 0; j < num_attributes; j++) {
            expr.addTerm(inequality.get(j), W[j]);
        }
        model.addConstr(expr, GRB.EQUAL, 0, "equal_constraint"); 

        model.optimize();
        solver_time += System.currentTimeMillis() - start;

        int status = model.get(GRB.IntAttr.Status);
        model.dispose();
        if (status == 2) {
            if (n.left == null) {
                Treenode left = new Treenode(n);
                left.addInequality(inequality, 1);

                Treenode right = new Treenode(n);
                right.addInequality(inequality, -1);

                n.left = left;
                n.right = right;
            } else {
                add_hyperplane(n.left, inequality, k);
                add_hyperplane(n.right, inequality, k);
            }
        } else if (status == 3) {
            
        }
    }

    /** 
     * Find the optimal error of a subtree
     * @param Treenode The root of the subtree
     * @param k
    */
    private int optimize_node(Treenode n, int k) throws GRBException {
        if (n.left == null) {
            leaf_count++;
            
            long start = System.currentTimeMillis();
            model = new GRBModel(env);
            model.set(GRB.IntParam.LogToConsole, 0);
            GRBLinExpr expr = new GRBLinExpr();
            GRBVar W[] = new GRBVar[num_attributes];
            for (int i = 0; i < num_attributes; i++) {
                W[i] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "w" + String.valueOf(i));
                expr.addTerm(1.0, W[i]);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "one");

            ArrayList<ArrayList<Double>> win_inequalities = n.getInequalities(1);
            ArrayList<ArrayList<Double>> lose_inequalities = n.getInequalities(-1);

            for (int i = 0; i < win_inequalities.size(); i++) {
                expr = new GRBLinExpr();
                for (int j = 0; j < num_attributes; j++) {
                    Double c = win_inequalities.get(i).get(j);
                    expr.addTerm(c, W[j]);
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, epsilon1, "win_constraint" + i);
            }
            for (int i = 0; i < lose_inequalities.size(); i++) {
                expr = new GRBLinExpr();
                for (int j = 0; j < num_attributes; j++) {
                    Double c = lose_inequalities.get(i).get(j);
                    expr.addTerm(c, W[j]);
                }
                model.addConstr(expr, GRB.LESS_EQUAL, -epsilon1, "lose_constraint" + i);
            }

            model.optimize();
            program_count++;
            solver_time += System.currentTimeMillis() - start;
            
            int status = model.get(GRB.IntAttr.Status);
            if (status == 2) {
                double[] weights_double = new double[num_attributes];
                for (int i = 0; i < num_attributes; i++) {
                    weights_double[i] = W[i].get(GRB.DoubleAttr.X);
                    weights[i] = new BigDecimal(weights_double[i]);
                }
                rank(k);
                RankingMeasurer rm = new RankingMeasurer(ranking, given_ranking);
                int e = rm.error(k);
                if (e < error) {
                    error = e;
                    best_weights = new BigDecimal[num_attributes];
                    for (int i = 0; i < num_attributes; i++) {
                        best_weights[i] = new BigDecimal(weights_double[i]);
                    }
                }

                model.dispose();

                for (int i = 0; i < win_inequalities.size(); i++) {
                    BigDecimal value = new BigDecimal(0.0);
                    for (int j = 0; j < num_attributes; j++) {
                        value = value.add(weights[j].multiply(new BigDecimal(win_inequalities.get(i).get(j))));
                    }
                    if (value.compareTo(new BigDecimal(0.0)) == -1) {
                        fake_leaf_count++;
                        return error;
                    }
                }

                for (int i = 0; i < lose_inequalities.size(); i++) {
                    BigDecimal value = new BigDecimal(0.0);
                    for (int j = 0; j < num_attributes; j++) {
                        value = value.add(weights[j].multiply(new BigDecimal(lose_inequalities.get(i).get(j))));
                    }
                    if (value.compareTo(new BigDecimal(0.0)) == 1) {
                        fake_leaf_count++;
                        return error;
                    }
                }

                return error;
            } else {
                return k * num_tuples;
            }
        } else {
            return Math.min(optimize_node(n.left, k), optimize_node(n.right, k));
        }
    }

    /** 
     * Solve the ranking explanation optimization (OPT) problem for minimum individual position error using a tree-based polynomial approach
     * The approach corresponds to Algorithm 5 in Abolfazl Asudeh, H. V. Jagadish, Julia Stoyanovich, Gautam Das: Designing Fair Ranking Schemes. SIGMOD 2019
     * @param k
     * @param timeout The timeout parameter for the solver, in ms, 0 for no timeout given
    */
    public void optimize_tree(int k, int timeout) throws GRBException {
        long start = System.currentTimeMillis();
        setup(0);
        error = k * num_tuples;
        solver_time = 0;

        ArrayList<ArrayList<Double>> inequalities = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < num_tuples; j++) {
                ArrayList<Double> inequality = new ArrayList<Double>();
                for (int l = 0; l < num_attributes; l++) {
                    inequality.add(Double.valueOf(tuples.get(i).values[l + 1]) - Double.valueOf(tuples.get(j).values[l + 1]));
                }
                inequalities.add(inequality);
            }
        }

        Treenode root = new Treenode(new ArrayList<ArrayList<Double>>(), new ArrayList<ArrayList<Double>>(), new ArrayList<ArrayList<Double>>());

        for (int i = 0; i < inequalities.size(); i++) {
            System.out.println(i);
            if (timeout > 0 && System.currentTimeMillis() - start > timeout) {
                System.out.println(i + " hyperplanes have been added to the tree");
                System.out.println("Solver time: " + solver_time + "ms");
                weights = best_weights;
                System.out.print("Weight: ");
                for (int j = 0; i < weights.length; i++) {
                    System.out.print(weights[j].doubleValue() + " ");
                }
                System.out.println();
                rank(k);
                return;
            }
            add_hyperplane(root, inequalities.get(i), k);
            System.out.println("Number of programs: " + program_count);
            System.out.println("Number of nodes: " + node_count);
        }
        error = num_tuples * k;
        optimize_node(root, k);
        System.out.println("Number of programs: " + program_count);
        System.out.println("Number of nodes: " + node_count);
        System.out.println("Number of leaf nodes: " + leaf_count);
        System.out.println("Number of fake leaf nodes: " + fake_leaf_count);
        weights = best_weights;
        System.out.print("Weight: ");
        for (int i = 0; i < weights.length; i++) {
            System.out.print(weights[i].doubleValue() + " ");
        }
        System.out.println();
        rank(k);
        System.out.println("Solver time: " + solver_time + "ms");
    }
}