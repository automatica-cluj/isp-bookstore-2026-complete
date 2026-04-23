package com.bookstore.controller;

import com.bookstore.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final BookService bookService;

    public StatisticsController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Count  books!!!!!
     * @return
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("bookCount", bookService.countAll());
        return ResponseEntity.ok(stats);
    }

}
