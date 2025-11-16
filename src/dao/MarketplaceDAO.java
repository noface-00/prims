package dao;

import entities.Marketplace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class MarketplaceDAO extends genericDAO<Marketplace> {

    public MarketplaceDAO() {
        super(Marketplace.class);
    }

    // 🔍 Buscar Marketplace por nombre
    public Marketplace findByName(String name) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Marketplace m WHERE m.nameMarketplace = :name", Marketplace.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
}
