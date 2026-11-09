SET search_path TO library;
INSERT INTO authors(name) VALUES
    ('Robert C. Martin'), ('Joshua Bloch'), ('George Orwell'), ('Fyodor Dostoevsky')
ON CONFLICT(name) DO NOTHING;
INSERT INTO categories(name) VALUES ('Programming'), ('Fiction'), ('Classics')
ON CONFLICT(name) DO NOTHING;
INSERT INTO books(isbn, title, publication_year, category_id, total_copies) VALUES
    ('9780132350884', 'Clean Code', 2008, (SELECT id FROM categories WHERE name = 'Programming'), 3),
    ('9780134685991', 'Effective Java', 2018, (SELECT id FROM categories WHERE name = 'Programming'), 2),
    ('9780451524935', '1984', 1950, (SELECT id FROM categories WHERE name = 'Fiction'), 2),
    ('9780140449136', 'The Brothers Karamazov', 2003, (SELECT id FROM categories WHERE name = 'Classics'), 1)
ON CONFLICT(isbn) DO NOTHING;
INSERT INTO book_authors(book_id, author_id)
SELECT b.id, a.id FROM books b JOIN authors a ON
    (b.isbn = '9780132350884' AND a.name = 'Robert C. Martin') OR
    (b.isbn = '9780134685991' AND a.name = 'Joshua Bloch') OR
    (b.isbn = '9780451524935' AND a.name = 'George Orwell') OR
    (b.isbn = '9780140449136' AND a.name = 'Fyodor Dostoevsky')
ON CONFLICT DO NOTHING;
INSERT INTO readers(full_name, email, membership_type) VALUES
    ('Aida Asanova', 'aida@example.com', 'STUDENT'),
    ('Bek Sadykov', 'bek@example.com', 'GUEST')
ON CONFLICT(email) DO NOTHING;
-- Add sample loans once, only to an unused demo database.
INSERT INTO loans(book_id, reader_id, borrowed_on, due_on, returned_on)
SELECT b.id, r.id, CURRENT_DATE - 20, CURRENT_DATE - 6, NULL::date
FROM books b CROSS JOIN readers r
WHERE b.isbn = '9780451524935' AND r.email = 'bek@example.com'
  AND NOT EXISTS (SELECT 1 FROM loans l WHERE l.book_id = b.id AND l.reader_id = r.id);
INSERT INTO loans(book_id, reader_id, borrowed_on, due_on, returned_on)
SELECT b.id, r.id, CURRENT_DATE - 3, CURRENT_DATE + 27, NULL::date
FROM books b CROSS JOIN readers r
WHERE b.isbn = '9780132350884' AND r.email = 'aida@example.com'
  AND NOT EXISTS (SELECT 1 FROM loans l WHERE l.book_id = b.id AND l.reader_id = r.id);
INSERT INTO loans(book_id, reader_id, borrowed_on, due_on, returned_on)
SELECT b.id, r.id, CURRENT_DATE - 40, CURRENT_DATE - 10, CURRENT_DATE - 15
FROM books b CROSS JOIN readers r
WHERE b.isbn = '9780134685991' AND r.email = 'aida@example.com'
  AND NOT EXISTS (SELECT 1 FROM loans l WHERE l.book_id = b.id AND l.reader_id = r.id);
