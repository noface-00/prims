package dao;

import entities.AtributtesProduct;
import jakarta.persistence.EntityManager;

public class AtributtesProductDAO extends genericDAO<AtributtesProduct> {

    public AtributtesProductDAO() {
        super(AtributtesProduct.class);
    }

    /**
     * 🔍 Verifica si un atributo ya existe para un producto específico.
     * Se compara por itemId y nombre de atributo.
     */
    public boolean existeAtributo(String itemId, String atributte) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(a) FROM AtributtesProduct a " +
                                    "WHERE a.idItem.itemId = :itemId AND a.atributte = :atributte", Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("atributte", atributte)
                    .getSingleResult();

            return count > 0;

        } catch (Exception e) {
            System.err.println("⚠️ Error verificando atributo: " + e.getMessage());
            return false;

        } finally {
            em.close();
        }
    }
}
