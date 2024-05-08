package org.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class Book {
    @NotBlank(message = "Book name must not be blank")
    private String bookName;

    @NotBlank(message = "Author must not be blank")
    private String author;

    @NotNull(message = "Publication year must not be null")
    @Pattern(regexp = "^\\d{4}$", message = "Invalid publication year format (YYYY required)")//not working, need to check
    private Integer publicationYear;

}
