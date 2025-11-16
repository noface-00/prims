package dao;

import entities.PriceHistory;
import entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;

public class PriceHistoryDAO extends genericDAO<PriceHistory> {

    public PriceHistoryDAO() {
        super(PriceHistory.class);
    }

    /**
     * 🔍 Verifica si ya existe un registro de precio para un producto en una fecha dada.
     *
     * @param itemId   ID del producto
     * @param recordedAt Fecha del registro (yyyy-MM-dd)
     * @return true si existe, false si no
     */
    public boolean existeHistorial(String itemId, String recordedAt) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(p) FROM PriceHistory p " +
                                    "WHERE p.itemId = :itemId AND p.recordedAt = :recordedAt", Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("recordedAt", recordedAt)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            System.err.println("⚠️ Error verificando historial de precio: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }
    /**
     * Obtiene el último precio registrado para un itemId.
     *
     * @param itemId identificador del producto (v1%7C...).
     * @return el registro de precio más reciente, o null si no hay.
     */
    public PriceHistory findLatestByItemId(String itemId) {
        EntityManager em = emf.createEntityManager();
        PriceHistory result = null;

        try {
            result = em.createQuery(
                            "SELECT ph FROM PriceHistory ph WHERE ph.itemId = :itemId ORDER BY ph.recordedAt DESC",
                            PriceHistory.class)
                    .setParameter("itemId", itemId)
                    .setMaxResults(1)
                    .getSingleResult();

        } catch (NoResultException e) {
            System.out.println("⚠️ No hay precios registrados para itemId: " + itemId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }

        return result;
    }

    public List<PriceHistory> findAllByItemId(String itemId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM PriceHistory p WHERE p.itemId = :id ORDER BY p.recordedAt ASC",
                            PriceHistory.class
                    )
                    .setParameter("id", itemId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

}
