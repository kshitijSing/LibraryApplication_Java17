package org.example.library.controller;

import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.example.library.dto.Book;
import org.example.library.service.UserService;
import org.example.library.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class LibraryController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        if (userService.isValidUser(user)) {

            return userService.generateToken(user.getUsername());
        }
        return "Invalid credentials";
    }

    @GetMapping("/home")
    public ResponseEntity<List<String>> getBooks(@RequestHeader("Authorization") String token) throws CsvValidationException, IOException {
        List<String> books = userService.getBooks(token);
        return ResponseEntity.ok(books);
    }

    @PostMapping("/addBook")
    public ResponseEntity<String> addBook(@Valid @RequestBody Book book, @RequestHeader("Authorization") String token) throws IOException {

        if (book.getPublicationYear() < 0 || book.getPublicationYear() > 2024) {
            return ResponseEntity.badRequest().body("Invalid publication year: Must be between 0 and 2024");
        }

        if (book.getBookName() == null || book.getBookName().isEmpty()) {
            return ResponseEntity.badRequest().body("Book name must not be blank");
        }

        if (book.getAuthor() == null || book.getAuthor().isEmpty()) {
            return ResponseEntity.badRequest().body("Author must not be blank");
        }

        String userType = userService.addBook(token, book);
        if (!userType.equals("admin"))
            return  ResponseEntity.ok("User does not have access to add file");
        return ResponseEntity.ok("Book added successfully");
    }

    @DeleteMapping("/deleteBook")
    public ResponseEntity<String> deleteBook(@RequestHeader("Authorization") String token, @RequestParam String bookName) throws IOException, CsvException {
        if (bookName == null || bookName.isEmpty()) {
            return ResponseEntity.badRequest().body("BookName must not be blank");
        }
        Map<String, Boolean> userTypeMap = userService.deleteBook(token, bookName);
        if (!userTypeMap.containsKey("admin"))
            return  ResponseEntity.ok("User does not have access to delete file");
        else if (userTypeMap.get("admin") == false)
            return  ResponseEntity.ok("Book name does not exists");
        return ResponseEntity.ok("Book deleted successfully");
    }
}
