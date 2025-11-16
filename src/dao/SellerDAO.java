package dao;

import entities.Seller;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class SellerDAO extends genericDAO<Seller> {

    public SellerDAO() {
        super(Seller.class);
    }

    public Seller getUserById(int id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Seller.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }



    public boolean existsByUsername(String username) {

        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(s) FROM Seller s WHERE s.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
    /**
     * Busca un vendedor por su nombre de usuario.
     *
     * @param username nombre de usuario del vendedor (eBay)
     * @return objeto Seller si existe, o null si no se encuentra
     */
    public Seller findByUsername(String username) {
        EntityManager em = emf.createEntityManager();
        Seller vendedor = null;

        try {
            vendedor = em.createQuery(
                            "SELECT s FROM Seller s WHERE s.username = :username",
                            Seller.class)
                    .setParameter("username", username)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró vendedor con username: " + username);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en SellerDAO.findByUsername()");
        } finally {
            em.close();
        }

        return vendedor;
    }
}
