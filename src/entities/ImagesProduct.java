package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "images_products", schema = "PRIMS", indexes = {
        @Index(name = "fk_products_img_idx", columnList = "itemId")
})
public class ImagesProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itemId", nullable = false)
    private Producto item;

    @Column(name = "url_img", nullable = false)
    private String urlImg;

    public ImagesProduct() {
    }

    public ImagesProduct(Producto item, String urlImg) {
        this.item = item;
        this.urlImg = urlImg;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Producto getItem() {
        return item;
    }

    public void setItem(Producto item) {
        this.item = item;
    }

    public String getUrlImg() {
        return urlImg;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }

}