package com.ebookeria.ecommerce.dto.ebook;

import com.ebookeria.ecommerce.entity.Author;

import java.time.LocalDate;
import java.util.List;

public record EbookDTO(
        int id,
        String title,
        String description,
        LocalDate publishedYear,
        String category,
        double price,
        List<String> author,
        List<String> imageUrl

) {
}