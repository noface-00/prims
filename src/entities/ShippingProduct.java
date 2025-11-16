package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "shipping_products", schema = "PRIMS", indexes = {
        @Index(name = "fk_shipping_pro_idx", columnList = "itemId")
})
public class ShippingProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // Asegúrate de que coincida con el nombre de tu tabla
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itemId", nullable = false)
    private Producto item;

    @Column(name = "type", nullable = false, length = 45)
    private String type;

    @Column(name = "shipping_carrier", nullable = false, length = 45)
    private String shippingCarrier;

    @Column(name = "shipping_cost", nullable = false)
    private Double shippingCost;

    public ShippingProduct() {
    }

    public ShippingProduct(Producto item, String type, String shippingCarrier, Double shippingCost) {
        this.item = item;
        this.type = type;
        this.shippingCarrier = shippingCarrier;
        this.shippingCost = shippingCost;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getShippingCarrier() {
        return shippingCarrier;
    }

    public void setShippingCarrier(String shippingCarrier) {
        this.shippingCarrier = shippingCarrier;
    }

    public Double getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost;
    }

}