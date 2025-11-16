package dao;

public interface inter_CRUD<T> {
    public void create(T entitty);
    T read(Object id);
    public void update(T entitty);
    public void delete(Object id);

}
