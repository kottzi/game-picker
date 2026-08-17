package io.github.kottzi.gamepicker.catalog.web;

import io.github.kottzi.gamepicker.catalog.domain.model.Genre;
import io.github.kottzi.gamepicker.catalog.domain.repository.GenreRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreRepository genreRepository;

    public GenreController(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @GetMapping
    public List<Genre> list() {
        List<Genre> genres = new ArrayList<>();
        genreRepository.findAll().forEach(genres::add);
        genres.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        return genres;
    }
}
