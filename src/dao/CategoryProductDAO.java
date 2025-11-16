package dao;

import entities.CategoryProduct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class CategoryProductDAO extends genericDAO<CategoryProduct> {
    public CategoryProductDAO() {
        super(CategoryProduct.class);
    }
    public Integer getIdBycategoryID(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT s.id FROM CategoryProduct s WHERE s.idCategory = :id", Integer.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return 0; //TODO Cambiar para que muestre una excepcion
        } finally {
            em.close();
        }
    }
    public boolean existsByCategoryId(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(c) FROM CategoryProduct c WHERE c.idCategory = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public CategoryProduct findByCategoryId(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM CategoryProduct c WHERE c.idCategory = :id", CategoryProduct.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

}
