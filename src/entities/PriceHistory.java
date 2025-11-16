package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "price_history", schema = "PRIMS")
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "itemId", nullable = false, length = 45)
    private String itemId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "currency", nullable = false, length = 45)
    private String currency;

    @Column(name = "recorded_at", nullable = false, length = 45)
    private String recordedAt;

    public PriceHistory() {
    }

    public PriceHistory(String itemId, Double price, String currency, String recordedAt) {
        this.itemId = itemId;
        this.price = price;
        this.currency = currency;
        this.recordedAt = recordedAt;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

}