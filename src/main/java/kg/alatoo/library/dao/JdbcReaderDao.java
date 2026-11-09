package kg.alatoo.library.dao;

import kg.alatoo.library.db.Database;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.MembershipType;
import kg.alatoo.library.model.Reader;
import kg.alatoo.library.util.Validation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReaderDao implements ReaderDao {
    private final Database database;
    public JdbcReaderDao(Database database) { this.database = database; }

    @Override public Reader create(Reader reader) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO readers(full_name, email, membership_type) VALUES (?, ?, ?) RETURNING id")) {
                bind(ps, reader);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next(); return new Reader(rs.getLong(1), reader.getFullName(), reader.getEmail(), reader.getMembershipType());
                }
            }
        });
    }

    @Override public Optional<Reader> findById(long id) {
        Validation.positiveId(id);
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM readers WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
            }
        });
    }

    @Override public List<Reader> findAll() { return list("SELECT * FROM readers ORDER BY id", null); }

    @Override public List<Reader> search(String query) {
        return list("SELECT * FROM readers WHERE full_name ILIKE ? ESCAPE '!' OR email ILIKE ? ESCAPE '!' ORDER BY id",
                JdbcBookDao.literalPattern(Validation.text(query, "Поиск", 160)));
    }

    private List<Reader> list(String sql, String pattern) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (pattern != null) { ps.setString(1, pattern); ps.setString(2, pattern); }
                List<Reader> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
                return result;
            }
        });
    }

    @Override public void update(Reader reader) {
        Validation.positiveId(reader.getId());
        database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE readers SET full_name = ?, email = ?, membership_type = ? WHERE id = ?")) {
                bind(ps, reader); ps.setLong(4, reader.getId());
                if (ps.executeUpdate() == 0) throw new LibraryException("Читатель не найден.");
            }
            return null;
        });
    }

    @Override public void delete(long id) {
        Validation.positiveId(id);
        database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM readers WHERE id = ?")) {
                ps.setLong(1, id);
                if (ps.executeUpdate() == 0) throw new LibraryException("Читатель не найден.");
            }
            return null;
        });
    }

    private void bind(PreparedStatement ps, Reader reader) throws SQLException {
        ps.setString(1, reader.getFullName()); ps.setString(2, reader.getEmail()); ps.setString(3, reader.getMembershipType().name());
    }

    private Reader map(ResultSet rs) throws SQLException {
        return new Reader(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"), MembershipType.valueOf(rs.getString("membership_type")));
    }
}
