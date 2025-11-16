package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "atributtes_products", schema = "PRIMS", indexes = {
        @Index(name = "fk_products_attri_idx", columnList = "id_item")
})
public class AtributtesProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Producto idItem;

    @Column(name = "atributte", nullable = false, length = 100)
    private String atributte;

    @Column(name = "value", nullable = false, length = 500)
    private String value;

    public AtributtesProduct() {
    }

    public AtributtesProduct(Producto idItem, String atributte, String value) {
        this.idItem = idItem;
        this.atributte = atributte;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Producto getIdItem() {
        return idItem;
    }

    public void setIdItem(Producto idItem) {
        this.idItem = idItem;
    }

    public String getAtributte() {
        return atributte;
    }

    public void setAtributte(String atributte) {
        this.atributte = atributte;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}