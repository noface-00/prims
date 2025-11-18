package dao;

import entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class ImagesProductDAO extends genericDAO<ImagesProductDAO> {
    public ImagesProductDAO() {
        super(ImagesProductDAO.class);
    }

    public boolean existeImagen(String itemId, String urlImg) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(i) FROM ImagesProduct i " +
                                    "WHERE i.item.itemId = :itemId AND i.urlImg = :urlImg", Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("urlImg", urlImg)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    /**
     * Obtiene la URL de imagen principal asociada a un producto.
     *
     * @param producto Entidad Producto
     * @return La URL de imagen, o null si no se encuentra
     */
    public String findMainImageByProduct(Producto producto) {
        EntityManager em = emf.createEntityManager();
        String url = null;

        try {
            url = em.createQuery(
                            "SELECT i.urlImg FROM ImagesProduct i WHERE i.item = :producto ORDER BY i.id ASC",
                            String.class)
                    .setParameter("producto", producto)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            System.out.println("No se encontró imagen para el producto " + producto.getItemId());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }

        return url;
    }
}
