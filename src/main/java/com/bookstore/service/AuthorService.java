package com.bookstore.service;

import com.bookstore.dto.AuthorRequest;
import com.bookstore.dto.AuthorResponse;
import com.bookstore.model.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorResponse> findAll() {
        return authorRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AuthorResponse findById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + id));
        return toResponse(author);
    }

    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        Author author = new Author(request.firstName(), request.lastName());
        Author saved = authorRepository.save(author);
        return toResponse(saved);
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getFirstName(),
                author.getLastName()
        );
    }
}
