package dao;

import entities.ProductAnalysis;
import entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import utils.CacheManager;

import java.util.List;

/**
 * DAO optimizado para la entidad Producto.
 * Usa JOIN FETCH para reducir consultas N+1 y caché para consultas repetidas.
 */
public class ProductDAO extends genericDAO<Producto> {

    public ProductDAO() {
        super(Producto.class);
    }

    /**
     * Obtiene todos los análisis de mercado registrados para un producto específico.
     */
    public List<ProductAnalysis> obtenerAnalisisPorProducto(String itemId) {
        // Verificar caché primero
        String cacheKey = "analysis_list:" + itemId;
        @SuppressWarnings("unchecked")
        List<ProductAnalysis> cached = (List<ProductAnalysis>) CacheManager.get(cacheKey, List.class);

        if (cached != null) {
            System.out.println("♻️ Análisis obtenidos desde caché");
            return cached;
        }

        EntityManager em = emf.createEntityManager();
        try {
            List<ProductAnalysis> result = em.createQuery(
                            "SELECT a FROM ProductAnalysis a " +
                                    "WHERE a.item.itemId = :id " +
                                    "ORDER BY a.analysisDate DESC", ProductAnalysis.class)
                    .setParameter("id", itemId)
                    .getResultList();

            // Guardar en caché
            CacheManager.put(cacheKey, result);

            return result;
        } finally {
            em.close();
        }
    }

    /**
     * Busca un producto por su itemId (versión simple sin relaciones).
     */
    public Producto findByItemId(String itemId) {
        // Verificar caché primero
        String cacheKey = "product:" + itemId;
        Producto cached = CacheManager.get(cacheKey, Producto.class);

        if (cached != null) {
            System.out.println("♻️ Producto obtenido desde caché");
            return cached;
        }

        EntityManager em = emf.createEntityManager();
        Producto producto = null;

        try {
            producto = em.createQuery(
                            "SELECT p FROM Producto p WHERE p.itemId = :itemId", Producto.class)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            System.out.println("✅ Producto encontrado en BD con itemId: " + itemId);

            // Guardar en caché
            CacheManager.put(cacheKey, producto);

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

    /**
     * 🚀 OPTIMIZADO: Obtiene un producto con TODAS sus relaciones cargadas en una sola consulta.
     * Usa JOIN FETCH para evitar el problema N+1 y lazy loading.
     *
     * @param itemId Identificador del producto
     * @return Producto con todas sus relaciones inicializadas
     */
    public Producto findByItemIdWithRelations(String itemId) {
        // Verificar caché primero
        String cacheKey = "product_full:" + itemId;
        Producto cached = CacheManager.get(cacheKey, Producto.class);

        if (cached != null) {
            System.out.println("♻️ Producto completo obtenido desde caché");
            return cached;
        }

        EntityManager em = emf.createEntityManager();
        try {
            Producto p = em.createQuery(
                            "SELECT DISTINCT p FROM Producto p " +
                                    "LEFT JOIN FETCH p.idSeller s " +
                                    "LEFT JOIN FETCH p.idCategory c " +
                                    "LEFT JOIN FETCH p.idCondition cond " +
                                    "LEFT JOIN FETCH p.idCoupon cup " +
                                    "WHERE p.itemId = :itemId",
                            Producto.class)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            System.out.println("✅ Producto con relaciones cargado: " + itemId);

            // Guardar en caché
            CacheManager.put(cacheKey, p);

            return p;

        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró producto con itemId: " + itemId);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en ProductDAO.findByItemIdWithRelations()");
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * 🚀 OPTIMIZADO: Busca múltiples productos con sus relaciones en una sola consulta.
     *
     * @param itemIds Lista de identificadores de productos
     * @return Lista de productos con relaciones cargadas
     */
    public List<Producto> findMultipleWithRelations(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT DISTINCT p FROM Producto p " +
                                    "LEFT JOIN FETCH p.idSeller " +
                                    "LEFT JOIN FETCH p.idCategory " +
                                    "LEFT JOIN FETCH p.idCondition " +
                                    "WHERE p.itemId IN :itemIds",
                            Producto.class)
                    .setParameter("itemIds", itemIds)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en ProductDAO.findMultipleWithRelations()");
            return List.of();
        } finally {
            em.close();
        }
    }

    /**
     * Limpia el caché de un producto específico
     */
    public void clearCache(String itemId) {
        CacheManager.remove("product:" + itemId);
        CacheManager.remove("product_full:" + itemId);
        CacheManager.remove("analysis_list:" + itemId);
    }

    /**
     * Limpia todo el caché de productos
     */
    public static void clearAllCache() {
        CacheManager.clear();
    }
}