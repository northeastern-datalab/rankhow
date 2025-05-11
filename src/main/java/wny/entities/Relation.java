package wny.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** 
 * A relation is an arraylist of tuples with a common list of attributes
 * The schema of the relation specifies those attributes
 * The relation is empty upon construction
 * Modified by Zixuan Chen 
 * The original code of Nikolaos is from the any-k repository, https://github.com/northeastern-datalab/anyk-code.
*/
public class Relation
{
    private String relation_name;
    private String[] schema;
    private ArrayList<Tuple> tuples;

	/** 
     * @param relation_name A string identifier for the relation
     * @param relation_schema An array of attribute names
    */
    public Relation(String relation_name, String[] relation_schema)
    {
        this.relation_name = relation_name;
        this.schema = relation_schema;
        this.tuples = new ArrayList<Tuple>();
    }

    /** 
     * @param t The tuple to be inserted at the end of the relation
     */
    public void insert(Tuple t)
    {
        this.tuples.add(t);
    }

    /** 
     * @param ts A collection of tuples to be inserted all at once
     */
    // Inserts a collection of tuple in the relation
    public void insertAll(Collection<Tuple> ts)
    {
        this.tuples.addAll(ts);
    }

    /** 
     * @param index The index of a tuple
     * @return Tuple The tuple at the specified index
     */
    public Tuple get(int index)
    {
        return this.tuples.get(index);
    }

    /** 
     * @return ArrayList<Tuple> All tuples in the relation
     */
    public ArrayList<Tuple> getAll()
    {
        return this.tuples;
    }

    /** 
     * @param num The number of tuples to keep
     */
    // Only keep the top-num tuples in the relation
    public void top(int num) {
        int num_attributes = get_num_attributes();
        ArrayList<Tuple> new_tuples = new ArrayList<Tuple>();

        for (int i = 0; i < num; i++) {
            Tuple tuple = new Tuple(new String[num_attributes]);
            for (int j = 0; j < num_attributes; j++) {
                tuple.values[j] = tuples.get(i).values[j];
            }
            new_tuples.add(tuple);
        }
        tuples = new_tuples;
    }

    /** 
     * @param indices the indices of attributes to keep
     */
    // Only keep the columns at the input indices
    public void project(int indices[]) {
        int num_attributes = indices.length;
        ArrayList<Tuple> new_tuples = new ArrayList<Tuple>();

        for (int i = 0; i < tuples.size(); i++) {
            Tuple tuple = new Tuple(new String[num_attributes]);
            for (int j = 0; j < num_attributes; j++) {
                tuple.values[j] = tuples.get(i).values[indices[j]];
            }
            new_tuples.add(tuple);
        }
        tuples = new_tuples;

        String[] new_schema = new String[num_attributes];
        for (int i = 0; i < num_attributes; i++) {
            new_schema[i] = schema[indices[i]];
        }
        schema = new_schema;
    }

    // Derive new attributes
    public void derive(int exponent) {
        int num_tuples = tuples.size();

        int num_attributes = (schema.length - 1) * exponent;
        String[] new_schema = new String[1 + num_attributes];
        ArrayList<Tuple> new_tuples = new ArrayList<Tuple>();

        for (int i = 0; i < schema.length; i++) {
            new_schema[i] = schema[i];
        }
        int count = schema.length;
        for (int i = 1; i <= schema.length - 1; i++) {
            for (int j = 2; j <= exponent; j++) {
                new_schema[count++] = schema[i] + "^" + j;
            }
        }

        for (int l = 0; l < num_tuples; l++) {
            Tuple tuple = new Tuple(new String[1 + num_attributes]);
            for (int i = 0; i < schema.length; i++) {
                tuple.values[i] = tuples.get(l).values[i];
            }
            count = schema.length;
            for (int i = 1; i <= schema.length - 1; i++) {
                for (int j = 2; j <= exponent; j++) {
                    tuple.values[count++] = "" + Math.pow(Double.valueOf(tuples.get(l).values[i]), j);
                }
            }
            new_tuples.add(tuple);
        }

        schema = new_schema;
        tuples = new_tuples;
    }

    /** 
     * Get the ranking directly from a column of ranks
     * @return int[] the ranking of tuples
     */
    public int[] getRanking() {
        int size = tuples.size();
        int rank_index = tuples.get(0).values.length - 1;
        int[] ranking = new int[size];
        for (int i = 0; i < size; i++) {
            ranking[i] = Integer.parseInt(tuples.get(i).values[rank_index]);
        }
        return ranking;
    }

    /** 
     * Get the ranking from a column of scores
     * @return int[] the ranking of tuples
     */
    public int[] getRankingfromScore() {
        double precision = 1e-10;

        int size = tuples.size();
        int rank_index = tuples.get(0).values.length - 1;
        int[] ranking = new int[size];

        for (int i = 0; i < size; i++) {
            ranking[i] = i + 1;
            double score = Double.parseDouble(tuples.get(i).values[rank_index]);
            for (int j = i - 1; j >= 0; j--) {
                if (Double.parseDouble(tuples.get(j).values[rank_index]) - score <= precision) {
                    ranking[i]--;
                } else {
                    break;
                }
            }
        }
        return ranking;
    }

	/** 
	 * Sorts the tuples in the relation according to their compareTo() method
	 * @see entities.Tuple#compareTo
     */
    public void sort()
    {
        Collections.sort(this.tuples);
    }
    
    /** 
     * @return int The number of tuples in the relation
     */
    public int get_size()
    {
        return tuples.size();
    }

    /** 
     * @return int The number of attributes in the relation
     */
    public int get_num_attributes()
    {
        return tuples.get(0).values.length;
    }

    /** 
     * @return String The ID of the relation
     */
    public String get_relation_name()
    {
        return relation_name;
    }
    
    /** 
     * @return String The contents of the relation in string format
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("Relation " + this.relation_name + "\n");
        for (String attribute : this.schema)
            str.append(attribute + " ");
        str.append("\n");
        for (Tuple t : this.tuples)
            str.append(t.flat_format() + "\n");
        str.append("End of " + this.relation_name + "\n");
        return str.toString();
    }
}
