package kg.alatoo.library.dao;

import kg.alatoo.library.model.Book;
import java.util.List;

public interface BookDao extends CrudDao<Book> {
    List<Book> search(String query);
    int availableCopies(long bookId);
}
