package kg.alatoo.library.dao;

import java.util.List;
import java.util.Optional;

public interface CrudDao<T> {
    T create(T entity);
    Optional<T> findById(long id);
    List<T> findAll();
    void update(T entity);
    void delete(long id);
}
