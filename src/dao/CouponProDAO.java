package dao;

import entities.CouponPro;
import entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class CouponProDAO extends genericDAO<CouponPro> {
    public CouponProDAO() {
        super(CouponPro.class);
    }

    public boolean existsByCodeAndItemId(String code, String itemId) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(c) FROM CouponPro c " +
                                    "WHERE c.couponRedemption = :code AND c.itemId = :itemId", Long.class)
                    .setParameter("code", code)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            return count > 0;
        } finally {
            em.close();
        }
    }

    public CouponPro findByCodeAndItemId(String code, String itemId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM CouponPro c WHERE c.couponRedemption = :code AND c.itemId = :itemId",
                            CouponPro.class
                    )
                    .setParameter("code", code)
                    .setParameter("itemId", itemId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Busca un cupón válido por itemId (el más reciente o activo).
     *
     * @param itemId Identificador único del producto.
     * @return Objeto CouponPro si existe, o null si no se encuentra.
     */
    public CouponPro findByItemId(String itemId) {
        EntityManager em = emf.createEntityManager();
        CouponPro result = null;

        try {
            result = em.createQuery(
                            "SELECT c FROM CouponPro c WHERE c.itemId = :itemId ORDER BY c.id DESC",
                            CouponPro.class)
                    .setParameter("itemId", itemId)
                    .setMaxResults(1)
                    .getSingleResult();

        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró cupón para itemId: " + itemId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }

        return result;
    }
}
