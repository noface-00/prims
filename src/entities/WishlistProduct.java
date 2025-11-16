package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "wishlist_products", schema = "PRIMS", indexes = {
        @Index(name = "fk_user_wish_idx", columnList = "id_user"),
        @Index(name = "fk_products_wish_idx", columnList = "id_item")
})
public class WishlistProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", referencedColumnName = "id", nullable = false)
    private Auth idUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Producto idItem;

    public WishlistProduct() {
    }

    public WishlistProduct(Integer id, Auth idUser, Producto idItem) {
        this.id = id;
        this.idUser = idUser;
        this.idItem = idItem;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Auth getIdUser() {
        return idUser;
    }

    public void setIdUser(Auth idUser) {
        this.idUser = idUser;
    }

    public Producto getIdItem() {
        return idItem;
    }

    public void setIdItem(Producto idItem) {
        this.idItem = idItem;
    }

}