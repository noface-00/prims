package entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos", schema = "PRIMS", indexes = {
        @Index(name = "fk_seller_pro_idx", columnList = "id_seller"),
        @Index(name = "fk_coupon_pro_idx", columnList = "id_coupon")
})
public class Producto {

    @Id
    @Column(name = "itemId", nullable = false, length = 200)
    private String itemId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // ===== RELACIONES =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seller", nullable = false)
    private Seller idSeller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_category", nullable = false)
    private CategoryProduct idCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_condition",  nullable = false)
    private ConditionProduct idCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_coupon", referencedColumnName = "id")
    private CouponPro idCoupon;

    // ===== CAMPOS NORMALES =====
    @Column(name = "available")
    private Byte available;

    @Column(name = "returns")
    private Byte returns;

    @Column(name = "rated_prduct", nullable = false)
    private Byte ratedProduct;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    @Column(name = "URL_product", nullable = false , length = 1000)
    private String urlProduct;

    @Column(name = "created_at", nullable = false, length = 45)
    private String createdAt;

    // 🔹 Campos temporales para precios (no persistentes)
    @Transient
    private PriceHistory priceHistory; // No se guarda en la BD, solo en memoria

    @Transient
    private List<String> imageUrls = new ArrayList<>();

    @Transient
    private List<AtributtesProduct> atributos; // 🔹 No se guarda en BD, solo memoria

    @Transient
    private List<ShippingProduct> envios = new ArrayList<>();

    // ===== CONSTRUCTOR VACÍO =====
    public Producto() {}

    // ===== CONSTRUCTOR COMPLETO =====
    public Producto(String itemId, String name, Seller idSeller,
                    CategoryProduct idCategory,ConditionProduct idCondition, Byte ratedProduct,
                    String urlProduct, String createdAt) {
        this.itemId = itemId;
        this.name = name;
        this.idSeller = idSeller;
        this.idCategory = idCategory;
        this.idCondition = idCondition;
        this.ratedProduct = ratedProduct;
        this.urlProduct = urlProduct;
        this.createdAt = createdAt;
    }

    // ===== GETTERS Y SETTERS =====
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Seller getIdSeller() {
        return idSeller;
    }

    public void setIdSeller(Seller idSeller) {
        this.idSeller = idSeller;
    }

    public CategoryProduct getIdCategory() {
        return idCategory;
    }

    public void setIdCategory(CategoryProduct idCategory) {
        this.idCategory = idCategory;
    }

    public ConditionProduct getIdCondition() {
        return idCondition;
    }

    public void setIdCondition(ConditionProduct idCondition) {
        this.idCondition = idCondition;
    }

    public CouponPro getIdCoupon() {
        return idCoupon;
    }

    public void setIdCoupon(CouponPro idCoupon) {
        this.idCoupon = idCoupon;
    }

    public Byte getAvailable() {
        return available;
    }

    public void setAvailable(Byte available) {
        this.available = available;
    }

    public Byte getReturns() {
        return returns;
    }

    public void setReturns(Byte returns) {
        this.returns = returns;
    }

    public Byte getRatedProduct() {
        return ratedProduct;
    }

    public void setRatedProduct(Byte ratedProduct) {
        this.ratedProduct = ratedProduct;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getUrlProduct() {
        return urlProduct;
    }

    public void setUrlProduct(String urlProduct) {
        this.urlProduct = urlProduct;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public PriceHistory getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(PriceHistory priceHistory) {
        this.priceHistory = priceHistory;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    public List<AtributtesProduct> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<AtributtesProduct> atributos) {
        this.atributos = atributos;
    }

    public List<ShippingProduct> getEnvios() {
        return envios;
    }

    public void setEnvios(List<ShippingProduct> envios) {
        this.envios = envios;
    }
}

