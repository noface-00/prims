package dao;

import entities.ProductAnalysis;
import entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;

/**
 * DAO para la entidad Producto, con métodos adicionales
 * relacionados al análisis de mercado asociado a cada producto.
 */
public class ProductDAO extends genericDAO<Producto> {

    public ProductDAO() {
        super(Producto.class);
    }

    /**
     * Obtiene todos los análisis de mercado registrados para un producto específico.
     *
     * @param itemId Identificador del producto (itemId en la tabla productos)
     * @return Lista de objetos ProductAnalysis ordenados del más reciente al más antiguo
     */
    public List<ProductAnalysis> obtenerAnalisisPorProducto(String itemId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT a FROM ProductAnalysis a " +
                                    "WHERE a.item.itemId = :id " +
                                    "ORDER BY a.analysisDate DESC", ProductAnalysis.class)
                    .setParameter("id", itemId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Busca un producto por su itemId (guardado desde eBay o importado).
     *
     * @param itemId Identificador único del producto (por ejemplo: v1%7C126889945760%7C0)
     * @return el Producto si existe, o null si no se encuentra
     */
    public Producto findByItemId(String itemId) {
        EntityManager em = emf.createEntityManager();
        Producto producto = null;

        try {
            producto = em.createQuery(
                            "SELECT p FROM Producto p WHERE p.itemId = :itemId", Producto.class)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            System.out.println("✅ Producto encontrado en BD con itemId: " + itemId);

        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró producto con itemId: " + itemId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en ProductDAO.findByItemId()");
        } finally {
            em.close();
        }

        return producto;
    }
}
