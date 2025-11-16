package dao;

import entities.Auth;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class AuthDAO extends genericDAO<Auth> {
    public AuthDAO() {
        super(Auth.class);
    }

    public Auth login(String username, String passwordHash) {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT a FROM Auth a WHERE a.username = :u AND a.passwordHash = :p",
                            Auth.class
                    )
                    .setParameter("u", username)
                    .setParameter("p", passwordHash)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null; // usuario o pass incorrectos
        } finally {
            em.close();
        }
    }

    public boolean existsUser(String username, String email) {
        EntityManager em = emf.createEntityManager();

        try {
            Long count = em.createQuery(
                            "SELECT COUNT(a) FROM Auth a WHERE a.username = :u OR a.email = :e",
                            Long.class
                    )
                    .setParameter("u", username)
                    .setParameter("e", email)
                    .getSingleResult();

            return count > 0;

        } finally {
            em.close();
        }
    }
    public boolean resetPassword(String email, String newPasswordHash) {
        EntityManager em = emf.createEntityManager();

        try {
            Auth user = em.createQuery(
                            "SELECT a FROM Auth a WHERE a.email = :e",
                            Auth.class
                    )
                    .setParameter("e", email)
                    .getSingleResult();

            em.getTransaction().begin();
            user.setPasswordHash(newPasswordHash);
            em.getTransaction().commit();
            return true;

        } catch (NoResultException e) {
            return false;
        } finally {
            em.close();
        }
    }

    public boolean resetPasswordByUserOrEmail(String userOrEmail, String newPasswordHash) {
        EntityManager em = emf.createEntityManager();

        try {
            Auth auth = em.createQuery(
                            "SELECT a FROM Auth a WHERE a.username = :ue OR a.email = :ue",
                            Auth.class
                    )
                    .setParameter("ue", userOrEmail)
                    .getSingleResult();

            em.getTransaction().begin();
            auth.setPasswordHash(newPasswordHash);
            em.getTransaction().commit();

            return true;

        } catch (NoResultException e) {
            return false;
        } finally {
            em.close();
        }
    }

}
