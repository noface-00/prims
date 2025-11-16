package dao;

import entities.ShippingProduct;
import jakarta.persistence.EntityManager;

public class ShippingProductDAO extends genericDAO<ShippingProduct> {

    public ShippingProductDAO() {
        super(ShippingProduct.class);
    }

    /**
     * 🔍 Verifica si ya existe una opción de envío registrada para un producto específico.
     *
     * @param itemId ID del producto (itemId)
     * @param carrier Nombre del transportista (e.g. FedEx, DHL)
     * @return true si ya existe, false si no
     */
    public boolean existeEnvio(String itemId, String carrier) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(s) FROM ShippingProduct s " +
                                    "WHERE s.item.itemId = :itemId AND s.shippingCarrier = :carrier",
                            Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("carrier", carrier)
                    .getSingleResult();

            return count > 0;

        } catch (Exception e) {
            System.err.println("⚠️ Error verificando envío: " + e.getMessage());
            return false;

        } finally {
            em.close();
        }
    }
}
