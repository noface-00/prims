package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "category_products", schema = "PRIMS", uniqueConstraints = {
        @UniqueConstraint(name = "id_category_UNIQUE", columnNames = {"id_category"})
})
public class CategoryProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "id_category", nullable = false)
    private Integer idCategory;

    @Column(name = "category_path", nullable = false)
    private String categoryPath;

    public CategoryProduct() {
    }

    public CategoryProduct(Integer idCategory, String categoryPath) {
        this.idCategory = idCategory;
        this.categoryPath = categoryPath;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCategory() {
        return idCategory;
    }

    public void setIdCategory(Integer idCategory) {
        this.idCategory = idCategory;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

}