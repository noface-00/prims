package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class genericDAO<T> implements inter_CRUD {
    protected static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
    private Class<T> entityClass;
    public static EntityManagerFactory getEmf() {
        return emf;
    }
    public genericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }


    @Override
    public void create(Object entitty) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entitty);
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public T read(Object id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(entityClass, id);
        } finally {
            em.close();
        }
    }

    @Override
    public void update(Object entitty) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.merge(entitty);
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public void delete(Object id) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        T entity = em.find(entityClass, id);
        if (entity != null) em.remove(entity);
        em.getTransaction().commit();
        em.close();
    }

    public boolean exists(Object id) {
        EntityManager em = emf.createEntityManager();
        boolean ext = false;
        try {
            T entity = em.find(entityClass, id);
            ext = (entity != null);
        } finally {
            em.close();
        }
        return ext;
    }

}
