package entities;

import jakarta.persistence.*;

@Entity
@Table(name = "marketplaces", schema = "PRIMS")
public class Marketplace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name_marketplace", nullable = false, length = 45)
    private String nameMarketplace;

    @Column(name = "country_code", nullable = false, length = 45)
    private String countryCode;

    public Marketplace() {
    }

    public Marketplace( String nameMarketplace, String countryCode) {
        this.nameMarketplace = nameMarketplace;
        this.countryCode = countryCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameMarketplace() {
        return nameMarketplace;
    }

    public void setNameMarketplace(String nameMarketplace) {
        this.nameMarketplace = nameMarketplace;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

}