package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "condition_products", schema = "PRIMS", uniqueConstraints = {
        @UniqueConstraint(name = "id_condition_UNIQUE", columnNames = {"id_condition"})
})
public class ConditionProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "id_condition", nullable = false)
    private Integer idCondition;

    @Column(name = "condition_path", nullable = false, length = 100)
    private String conditionPath;

    public ConditionProduct() {
    }

    public ConditionProduct(Integer idCondition, String conditionPath) {
        this.idCondition = idCondition;
        this.conditionPath = conditionPath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCondition() {
        return idCondition;
    }

    public void setIdCondition(Integer idCondition) {
        this.idCondition = idCondition;
    }

    public String getConditionPath() {
        return conditionPath;
    }

    public void setConditionPath(String conditionPath) {
        this.conditionPath = conditionPath;
    }

}