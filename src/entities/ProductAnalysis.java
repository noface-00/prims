package entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "product_analysis", schema = "PRIMS", indexes = {
        @Index(name = "itemId", columnList = "itemId"),
        @Index(name = "id_seller", columnList = "id_seller")
})
public class ProductAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itemId", nullable = false)
    private Producto item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seller")
    private Seller idSeller;

    @Column(name = "price_actual")
    private Double priceActual;

    @Column(name = "market_average")
    private Double marketAverage;

    @Column(name = "market_min")
    private Double marketMin;

    @Column(name = "market_max")
    private Double marketMax;

    @Column(name = "std_deviation")
    private Double stdDeviation;

    @Column(name = "price_difference")
    private Double priceDifference;

    @Column(name = "trust_score")
    private Double trustScore;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "analysis_date")
    private Instant analysisDate;

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

    public Seller getIdSeller() {
        return idSeller;
    }

    public void setIdSeller(Seller idSeller) {
        this.idSeller = idSeller;
    }

    public Double getPriceActual() {
        return priceActual;
    }

    public void setPriceActual(Double priceActual) {
        this.priceActual = priceActual;
    }

    public Double getMarketAverage() {
        return marketAverage;
    }

    public void setMarketAverage(Double marketAverage) {
        this.marketAverage = marketAverage;
    }

    public Double getMarketMin() {
        return marketMin;
    }

    public void setMarketMin(Double marketMin) {
        this.marketMin = marketMin;
    }

    public Double getMarketMax() {
        return marketMax;
    }

    public void setMarketMax(Double marketMax) {
        this.marketMax = marketMax;
    }

    public Double getStdDeviation() {
        return stdDeviation;
    }

    public void setStdDeviation(Double stdDeviation) {
        this.stdDeviation = stdDeviation;
    }

    public Double getPriceDifference() {
        return priceDifference;
    }

    public void setPriceDifference(Double priceDifference) {
        this.priceDifference = priceDifference;
    }

    public Double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }

    public Instant getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(Instant analysisDate) {
        this.analysisDate = analysisDate;
    }

}