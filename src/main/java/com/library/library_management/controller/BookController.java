package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.model.Book;
import com.library.library_management.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@RequestBody Book book) {
        Book created = bookService.createBook(book);
        return new ResponseEntity<>(
                ApiResponse.success(201, "Book created successfully", created),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Book>>> getAllBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {

        List<Book> books = bookService.searchBooks(title, author);
        if (books.isEmpty()) {
            String msg = "No books found";
            if (title != null && author != null) {
                msg = "No books found with title '" + title + "' and author '" + author + "'";
            } else if (title != null) {
                msg = "No books found with title '" + title + "'";
            } else if (author != null) {
                msg = "No books found with author '" + author + "'";
            }
            return ResponseEntity.ok(
                    ApiResponse.success(200, msg, books)
            );
        }
        return ResponseEntity.ok(
                ApiResponse.success(200, "Books fetched successfully", books)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Book fetched successfully", book)
        );
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<ApiResponse<Book>> getBookByIsbn(@PathVariable String isbn) {
        Book book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Book fetched successfully", book)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(
            @PathVariable Long id,
            @RequestBody Book bookDetails) {
        Book updated = bookService.updateBook(id, bookDetails);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Book updated successfully", updated)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Book deleted successfully", null)
        );
    }
}