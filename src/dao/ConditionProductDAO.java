package dao;

import entities.ConditionProduct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class ConditionProductDAO extends genericDAO<ConditionProduct> {

    public ConditionProductDAO() {
        super(ConditionProduct.class);
    }

    // ✅ Devuelve la entidad completa, no un Integer
    public ConditionProduct getIdByConditionID(String idCondition) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM ConditionProduct c WHERE c.idCondition = :id", ConditionProduct.class)
                    .setParameter("id", idCondition)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public boolean existsByConditionId(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(c) FROM ConditionProduct c WHERE c.idCondition = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
    public ConditionProduct findByConditionId(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM ConditionProduct c WHERE c.idCondition = :id", ConditionProduct.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

}
