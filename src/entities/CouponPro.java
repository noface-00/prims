package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "coupon_pro", schema = "PRIMS")
public class CouponPro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "itemId", nullable = false, length = 100)
    private String itemId;

    @Column(name = "coupon_redemption", nullable = false, length = 150)
    private String couponRedemption;

    @Column(name = "expiration_at", nullable = false, length = 100)
    private String expirationAt;

    public CouponPro() {
    }

    public CouponPro(String itemId, String couponRedemption, String expirationAt) {
        this.itemId = itemId;
        this.couponRedemption = couponRedemption;
        this.expirationAt = expirationAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getCouponRedemption() {
        return couponRedemption;
    }

    public void setCouponRedemption(String couponRedemption) {
        this.couponRedemption = couponRedemption;
    }

    public String getExpirationAt() {
        return expirationAt;
    }

    public void setExpirationAt(String expirationAt) {
        this.expirationAt = expirationAt;
    }

    public String getCode() {
        return couponRedemption;
    }

}