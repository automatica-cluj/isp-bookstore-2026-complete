-- No hardcoded users — the first registered user automatically becomes ADMIN

-- Sample authors
INSERT INTO authors (first_name, last_name) VALUES ('Robert', 'Martin');
INSERT INTO authors (first_name, last_name) VALUES ('Martin', 'Fowler');
INSERT INTO authors (first_name, last_name) VALUES ('Joshua', 'Bloch');

-- Sample books
INSERT INTO books (title, author_id, isbn, price) VALUES ('Clean Code', 1, '9780132350884', 29.99);
INSERT INTO books (title, author_id, isbn, price) VALUES ('Refactoring', 2, '9780134757599', 39.99);
INSERT INTO books (title, author_id, isbn, price) VALUES ('Effective Java', 3, '9780134685991', 34.99);

-- Book tags (demonstrates Set<String> stored via @ElementCollection)
INSERT INTO book_tags (book_id, tag) VALUES (1, 'programming');
INSERT INTO book_tags (book_id, tag) VALUES (1, 'best-practices');
INSERT INTO book_tags (book_id, tag) VALUES (1, 'bestseller');
INSERT INTO book_tags (book_id, tag) VALUES (2, 'programming');
INSERT INTO book_tags (book_id, tag) VALUES (2, 'refactoring');
INSERT INTO book_tags (book_id, tag) VALUES (3, 'programming');
INSERT INTO book_tags (book_id, tag) VALUES (3, 'java');
INSERT INTO book_tags (book_id, tag) VALUES (3, 'bestseller');
