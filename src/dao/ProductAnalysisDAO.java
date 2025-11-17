package dao;

import entities.ProductAnalysis;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProductAnalysisDAO extends genericDAO<ProductAnalysis>{
    public ProductAnalysisDAO() {
        super(ProductAnalysis.class);
    }

    public int countAll() {
        EntityManager em = getEmf().createEntityManager();
        try {
            return ((Number) em.createQuery("SELECT COUNT(p) FROM ProductAnalysis p").getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    public double getPromedioVariacionGeneral() {
        EntityManager em = getEmf().createEntityManager();
        try {
            Double avg = (Double) em.createQuery(
                    "SELECT AVG(p.priceDifference) FROM ProductAnalysis p"
            ).getSingleResult();
            return avg != null ? avg : 0.0;
        } finally {
            em.close();
        }
    }

    public List<ProductAnalysis> getTopVariacionesPositivas(int limit) {
        EntityManager em = getEmf().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT pa FROM ProductAnalysis pa " +
                                    "JOIN FETCH pa.item " +               // <-- SOLUCIÓN
                                    "ORDER BY pa.priceDifference DESC",
                            ProductAnalysis.class
                    )
                    .setMaxResults(limit)
                    .getResultList();

        } finally {
            em.close();
        }
    }

    public List<ProductAnalysis> getTopVariacionesNegativas(int limit) {
        EntityManager em = getEmf().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT pa FROM ProductAnalysis pa " +
                                    "JOIN FETCH pa.item " +               // <-- SOLUCIÓN
                                    "ORDER BY pa.priceDifference ASC",
                            ProductAnalysis.class
                    )
                    .setMaxResults(limit)
                    .getResultList();

        } finally {
            em.close();
        }
    }

    public List<Object[]> getResumenDiarioConsultas() {
        EntityManager em = getEmf().createEntityManager();
        try {
            return em.createNativeQuery("""
            SELECT DATE(analysis_date), COUNT(*)
            FROM product_analysis
            GROUP BY DATE(analysis_date)
            ORDER BY DATE(analysis_date) DESC
        """).getResultList();
        } finally {
            em.close();
        }
    }

    public ProductAnalysis findLastAnalysisByItem(String itemId) {
        EntityManager em = getEmf().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT pa FROM ProductAnalysis pa WHERE pa.item.itemId = :id ORDER BY pa.analysisDate DESC",
                            ProductAnalysis.class
                    ).setParameter("id", itemId)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

        } finally {
            em.close();
        }
    }
}
