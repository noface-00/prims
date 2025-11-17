package dao;

public interface inter_CRUD<T> {
    void create(T entity);
    T read(Object id);
    void update(T entity);
    void delete(T entity);
}


