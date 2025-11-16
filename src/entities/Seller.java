package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "seller", schema = "PRIMS", indexes = {
        @Index(name = "fk_market_seller_idx", columnList = "id_marketplace")
}, uniqueConstraints = {
        @UniqueConstraint(name = "username_UNIQUE", columnNames = {"username"})
})
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "feedback_score", nullable = false)
    private Integer feedbackScore;

    @Column(name = "feedback_porcentage", nullable = false)
    private Double feedbackPorcentage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_marketplace", nullable = false)
    private Marketplace marketplace;

    @Column(name = "created_at", length = 45)
    private String createdAt;

    public Seller() {
    }

    // Constructor simplificado (usa ID del marketplace directamente)
    public Seller(String username, int feedbackScore, double feedbackPercentage, Marketplace marketplace, String createdAt) {
        this.username = username;
        this.feedbackScore = feedbackScore;
        this.feedbackPorcentage = feedbackPercentage;
        this.marketplace = marketplace;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getFeedbackScore() {
        return feedbackScore;
    }

    public void setFeedbackScore(Integer feedbackScore) {
        this.feedbackScore = feedbackScore;
    }

    public Double getFeedbackPorcentage() {
        return feedbackPorcentage;
    }

    public void setFeedbackPorcentage(Double feedbackPorcentage) {
        this.feedbackPorcentage = feedbackPorcentage;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(Marketplace marketplace) {this.marketplace = marketplace;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

}