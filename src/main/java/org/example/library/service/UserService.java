package org.example.library.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.collections.map.HashedMap;
import org.example.library.dto.Book;
import org.example.library.dto.User;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final String secretKey = "root";
    private final String adminCsvFilePath = "Books/adminUser.csv";
    private static final String regularUserInputPath = "Books/Input/regularUser.csv";
    private static final String regularUserOutputPath = "Books/Output/regularUser.csv";

    public boolean isValidUser(User user) {

        return (user.getUsername().equals("admin") && user.getPassword().equals("admin")) ||
                (user.getUsername().equals("user") && user.getPassword().equals("user"));
    }

    public String generateToken(String username) {
        String token = Jwts.builder()
                .setSubject(username)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
        return token;
    }

    private Claims decodeToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();

        return claims;
    }

    public List<String> getBooks(String token) throws CsvValidationException, IOException {
        Claims userType = decodeToken(token);
        String user = (String) userType.get("sub");
        List<String> bookNames = new ArrayList<>();
        if (user.equals("user")) {
            moveFile(regularUserInputPath, regularUserOutputPath);
            bookNames = fileReader(regularUserOutputPath);
        } else if (user.equals("admin")){
            moveFile(regularUserInputPath, regularUserOutputPath);
            bookNames = fileReader(regularUserOutputPath);
            bookNames.addAll(fileReader(adminCsvFilePath));
        }
        return bookNames;
    }

    private static List<String> fileReader(String filePath) throws CsvValidationException {

        List<String> bookNames = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext();
            while ((line = reader.readNext()) != null) {
                String bookName = line[0];
                bookNames.add(bookName);
            }
            return bookNames;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    public String addBook(String token, Book book) throws IOException {
        Claims userType = decodeToken(token);
        String user = (String) userType.get("sub");
        if (user.equals("admin")) {
            writeToCSV(book);
        }
        return user;
    }

    public Map<String, Boolean> deleteBook(String token, String bookName) throws IOException, CsvException {
        Claims userType = decodeToken(token);
        boolean existsInFile = false;
        String user = (String) userType.get("sub");
        if (user.equals("admin")) {
            existsInFile = deleteFromCSV(bookName);
        }
        Map<String, Boolean> userTypeMap = new HashedMap();
        userTypeMap.put(user, existsInFile);
        return userTypeMap;
    }

    public static boolean deleteFromCSV(String bookName) throws IOException, CsvException {
        moveFile(regularUserOutputPath, regularUserInputPath);
        CSVReader reader = new CSVReader(new FileReader(regularUserInputPath));
        List<String[]> lines = reader.readAll();
        reader.close();

        boolean found = false;
        List<String[]> updatedLines = new ArrayList<>();

        for (String[] line : lines) {
            if (line.length > 0 && line[0].equalsIgnoreCase(bookName)) {
                found = true;
            } else {
                updatedLines.add(line);
            }
        }
        if (!found) {
            return false;
        }

        CSVWriter writer = new CSVWriter(new FileWriter(regularUserInputPath));
        writer.writeAll(updatedLines);
        writer.close();

        return true;
    }

    public static void writeToCSV(Book book) throws IOException {
        moveFile(regularUserOutputPath, regularUserInputPath);
        try (Writer writer = Files.newBufferedWriter(Paths.get(regularUserInputPath), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            CSVWriter csvWriter = new CSVWriter(writer);
            csvWriter.writeNext(new String[]{book.getBookName(), book.getAuthor(), String.valueOf(book.getPublicationYear())});
            csvWriter.close();
        } catch (IOException e) {
            throw new IOException("Error writing to CSV file", e);
        }
    }

    private static void moveFile(String sourceDir, String destinationDir) throws IOException {
        Path source = Paths.get(sourceDir);
        Path destination = Paths.get(destinationDir);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}

