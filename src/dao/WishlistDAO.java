package dao;

import entities.WishlistProduct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;

public class WishlistDAO extends genericDAO<WishlistProduct> {

    public WishlistDAO() {
        super(WishlistProduct.class);
    }

    /**
     * Obtiene el itemId del producto guardado por un usuario.
     *
     * @param userId ID del usuario logueado
     * @return itemId del producto guardado o null si no hay ninguno
     */
    public String getItemIdByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        String itemId = null;

        try {
            TypedQuery<String> query = em.createQuery(
                    "SELECT w.idItem.itemId FROM WishlistProduct w WHERE w.idUser.id = :userId",
                    String.class
            );
            query.setParameter("userId", userId);
            query.setMaxResults(1);
            itemId = query.getSingleResult();

        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró ningún producto en wishlist para el usuario ID: " + userId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }

        return itemId;
    }

    public boolean existsWishlist(int userId, String itemId) {
        EntityManager em = emf.createEntityManager();

        try {
            Long count = em.createQuery(
                            "SELECT COUNT(w) FROM WishlistProduct w " +
                                    "WHERE w.idUser.id = :userId AND w.idItem.itemId = :itemId",
                            Long.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            return count > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    public List<String> getAllItemIdsByUser(int userId) {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT w.idItem.itemId FROM WishlistProduct w WHERE w.idUser.id = :userId",
                            String.class
                    )
                    .setParameter("userId", userId)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }
    public int countWishlistByUser(int userId) {
        EntityManager em = emf.createEntityManager();

        try {
            Long count = em.createQuery(
                            "SELECT COUNT(w) FROM WishlistProduct w WHERE w.idUser.id = :userId",
                            Long.class
                    )
                    .setParameter("userId", userId)
                    .getSingleResult();

            return count.intValue();

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            em.close();
        }
    }

}
