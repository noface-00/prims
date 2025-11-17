package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import utils.ErrorHandler;

public class genericDAO<T> implements inter_CRUD<T> {
    protected static EntityManagerFactory emf;
    private Class<T> entityClass;

    static {
        try {
            emf = Persistence.createEntityManagerFactory("default");
            System.out.println("EntityManagerFactory inicializado correctamente");
        } catch (Exception e) {
            System.err.println("Error crítico al inicializar EntityManagerFactory");
            ErrorHandler.handleDatabaseError(e, "inicializar conexión a base de datos");
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    public static EntityManagerFactory getEmf() {
        if (emf == null || !emf.isOpen()) {
            try {
                emf = Persistence.createEntityManagerFactory("default");
            } catch (Exception e) {
                ErrorHandler.handleDatabaseError(e, "reconectar a base de datos");
                throw new RuntimeException("No se pudo reconectar a la base de datos", e);
            }
        }
        return emf;
    }

    public genericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public void create(Object entity) {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();

            ErrorHandler.logInfo("✅ Entidad creada: " + entityClass.getSimpleName());

        } catch (PersistenceException e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ErrorHandler.handleSaveError(e);
            throw new RuntimeException("Error al crear entidad: " + entityClass.getSimpleName(), e);

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ErrorHandler.handleDatabaseError(e, "crear " + entityClass.getSimpleName());
            throw new RuntimeException("Error inesperado al crear entidad", e);

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public T read(Object id) {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            T entity = em.find(entityClass, id);

            if (entity == null) {
                ErrorHandler.logWarning("⚠️ No se encontró " + entityClass.getSimpleName() + " con ID: " + id);
            }

            return entity;

        } catch (PersistenceException e) {
            ErrorHandler.handleDatabaseError(e, "leer " + entityClass.getSimpleName());
            return null;

        } catch (Exception e) {
            ErrorHandler.handleDatabaseError(e, "buscar " + entityClass.getSimpleName());
            return null;

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public void update(Object entity) {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();

            ErrorHandler.logInfo("✅ Entidad actualizada: " + entityClass.getSimpleName());

        } catch (PersistenceException e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ErrorHandler.handleDatabaseError(e, "actualizar " + entityClass.getSimpleName());
            throw new RuntimeException("Error al actualizar entidad", e);

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ErrorHandler.handleDatabaseError(e, "modificar " + entityClass.getSimpleName());
            throw new RuntimeException("Error inesperado al actualizar", e);

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public void delete(T entity) {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            em.getTransaction().begin();

            // Adjuntar la entidad al contexto si no lo está
            if (!em.contains(entity)) {
                entity = em.merge(entity);
            }

            em.remove(entity);

            em.getTransaction().commit();

            ErrorHandler.logInfo("✅ Entidad eliminada: " + entityClass.getSimpleName());

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ErrorHandler.handleDatabaseError(e, "eliminar " + entityClass.getSimpleName());
            throw new RuntimeException("Error eliminando entidad", e);

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }


    public boolean exists(Object id) {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            T entity = em.find(entityClass, id);
            return entity != null;

        } catch (PersistenceException e) {
            ErrorHandler.handleDatabaseError(e, "verificar existencia de " + entityClass.getSimpleName());
            return false;

        } catch (Exception e) {
            ErrorHandler.handleDatabaseError(e, "comprobar " + entityClass.getSimpleName());
            return false;

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Verifica si la conexión a la base de datos está activa
     */
    public static boolean isDatabaseAvailable() {
        EntityManager em = null;
        try {
            em = getEmf().createEntityManager();
            em.createNativeQuery("SELECT 1").getSingleResult();
            return true;
        } catch (Exception e) {
            ErrorHandler.logWarning("⚠️ Base de datos no disponible: " + e.getMessage());
            return false;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Cierra el EntityManagerFactory (llamar al cerrar la aplicación)
     */
    public static void closeFactory() {
        try {
            if (emf != null && emf.isOpen()) {
                emf.close();
                System.out.println("✅ EntityManagerFactory cerrado correctamente");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al cerrar EntityManagerFactory: " + e.getMessage());
        }
    }
}