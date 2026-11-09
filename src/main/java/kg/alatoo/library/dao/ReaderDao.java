package kg.alatoo.library.dao;

import kg.alatoo.library.model.Reader;
import java.util.List;

public interface ReaderDao extends CrudDao<Reader> {
    List<Reader> search(String query);
}
