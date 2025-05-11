package wny.entities;

/** 
 * A flexible constraint defined by users
 * @author Zixuan Chen
*/

public class Constraint {
    public int attribute;
    public String type;
    public Double value;
    public Double[] weightsweights;

    /** 
     * @param attribute The attribute to add this constraint to
     * @param type The type of the constraint can be min or max,
     * limiting the minimum of the lower bound or maximum of the upper bound
     * @param value The value of min or max
    */
    public Constraint(int attribute, String type, Double value) {
        this.attribute = attribute;
        this.type = type;
        this.value = value;
    }

    /** 
     * @param weightsweights The weight of each weight in this constraint
     * @param value The value
     * The constraint is sum(weightsweights[i] * W[i]) <= value
    */
    public Constraint(Double[] weightsweights, Double value) {
        this.weightsweights = weightsweights;
        this.type = "multiple";
        this.value = value;
    }
}
