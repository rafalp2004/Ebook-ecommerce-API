package com.ebookeria.ecommerce.service.ebook;

import com.ebookeria.ecommerce.dto.ebook.*;
import com.ebookeria.ecommerce.entity.*;
import com.ebookeria.ecommerce.exception.ResourceNotFoundException;
import com.ebookeria.ecommerce.repository.AuthorRepository;
import com.ebookeria.ecommerce.repository.CategoryRepository;
import com.ebookeria.ecommerce.repository.EbookRepository;
import com.ebookeria.ecommerce.repository.UserRepository;
import com.ebookeria.ecommerce.service.user.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Log4j2
@Service
public class EbookServiceImpl implements EbookService {

    private final EbookRepository ebookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final UserService userService;

    public EbookServiceImpl(EbookRepository ebookRepository, CategoryRepository categoryRepository, AuthorRepository authorRepository, UserService userService) {
        this.ebookRepository = ebookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
        this.userService = userService;
    }

    @Override
    public EbookResponse findAll(int pageNo, int pageSize, String sortField, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())?Sort.by(sortField).ascending():
                Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize,sort);
        Page<Ebook> ebooks =  ebookRepository.findAll(pageable);
        List<EbookDTO> listOfEbooks = ebooks.getContent().stream().map(this::mapToEbookDTO).toList();

        return new EbookResponse(listOfEbooks, ebooks.getNumber(), ebooks.getSize(), ebooks.getNumberOfElements(), ebooks.getTotalPages(), ebooks.isLast() );
    }

    @Override
    public EbookDTO findById(int id) {
        Ebook ebook = ebookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ebook with id: " + id + " not found"));
        return mapToEbookDTO(ebook);
    }

    @Override
    public EbookUserPanelResponse findUsersBook(int pageNo, int pageSize, String sortField, String sortDirection) {
        User currentUser = userService.getCurrentUser();
        String ebookSortField = "ebook." + sortField;
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(ebookSortField).ascending()
                : Sort.by(ebookSortField).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Ebook> ebooks =  ebookRepository.findUsersBoughtEbooksByUserId(currentUser.getId(),pageable);

        ebooks.stream().forEach((e)->log.info(e.toString()));
        List<EbookUserPanelDTO> listOfEbooks = ebooks.getContent().stream().map(this::mapToEbookUserPanelDTO).toList();

        return new EbookUserPanelResponse(listOfEbooks, ebooks.getNumber(), ebooks.getSize(), ebooks.getNumberOfElements(), ebooks.getTotalPages(), ebooks.isLast() );
    }

    @Override
    public EbookDTO save(EbookCreationDTO ebookCreationDTO) {
        Ebook ebook = new Ebook();
        ebook.setTitle(ebookCreationDTO.title());
        ebook.setDescription(ebookCreationDTO.description());
        ebook.setPublishedYear(ebookCreationDTO.publishedYear());
        ebook.setPrice(ebookCreationDTO.price());
        ebook.setDownloadUrl(ebookCreationDTO.downloadUrl());

        Category category = categoryRepository.findById(ebookCreationDTO.categoryId()).orElseThrow(() -> new ResourceNotFoundException("Category with id: " + ebookCreationDTO.categoryId() + " not found. Please add category first to categories base"));

        List<Author> authors = ebookCreationDTO.authorsId()
                .stream()
                .map(id -> {
                    Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author with id: " + id + " not found. Please add author first to author's base"));
                    ebook.addAuthor(author);
                    return author;
                })
                .toList();
        User user = userService.getCurrentUser();

        List<Image> images = ebookCreationDTO.imageUrls().
                stream().
                map(url -> {
                    Image image = new Image();
                    image.setUrl(url);
                    image.setEbook(ebook);
                    return image;
                })
                .toList();


        ebook.setCategory(category);
        ebook.setAuthors(authors);
        ebook.setUser(user);
        ebook.setImages(images);

        ebookRepository.save(ebook);
        return mapToEbookDTO(ebook);
    }


    @Override
    public void update(EbookUpdateDTO ebookUpdateDTO) {
        Ebook ebook = ebookRepository.findById(ebookUpdateDTO.id())
                .orElseThrow(() -> new ResourceNotFoundException("Ebook with id: " + ebookUpdateDTO.id() + " not found"));


        // Update fields only if they are present in DTO and different
        if (ebookUpdateDTO.title() != null && !ebook.getTitle().equals(ebookUpdateDTO.title())) {
            ebook.setTitle(ebookUpdateDTO.title());
        }

        if (ebookUpdateDTO.description() != null && !ebook.getDescription().equals(ebookUpdateDTO.description())) {
            ebook.setDescription(ebookUpdateDTO.description());
        }

        if (ebookUpdateDTO.publishedYear() != null && !ebook.getPublishedYear().equals(ebookUpdateDTO.publishedYear())) {
            ebook.setPublishedYear(ebookUpdateDTO.publishedYear());
        }
        if (ebookUpdateDTO.price() != null && Double.compare(ebook.getPrice(), ebookUpdateDTO.price()) != 0) {
            ebook.setPrice(ebookUpdateDTO.price());
        }

        if (ebookUpdateDTO.downloadUrl() != null && !ebook.getDownloadUrl().equals(ebookUpdateDTO.downloadUrl())) {
            ebook.setDownloadUrl(ebookUpdateDTO.downloadUrl());
        }

        if (ebookUpdateDTO.categoryId() != null) {
            Category category = categoryRepository.findById(ebookUpdateDTO.categoryId()).orElseThrow(() -> new ResourceNotFoundException("Category with id: " + ebookUpdateDTO.categoryId() + " not found. Please add category first to categories base"));

            if (!ebook.getCategory().equals(category)) {
                ebook.setCategory(category);
            }
        }

        if (ebookUpdateDTO.authorsId() != null) {
            List<Integer> currentAuthorIds = ebook.getAuthors().stream().map(Author::getId).toList();
            List<Integer> newAuthorIds = ebookUpdateDTO.authorsId();

            newAuthorIds.stream()
                    .filter(id -> !currentAuthorIds.contains(id))
                    .forEach(id -> {
                        Author author = authorRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Author with id: " + id + " not found"));
                        ebook.addAuthor(author);
                    });

            ebook.getAuthors().removeIf(author -> {
                boolean toRemove = !newAuthorIds.contains(author.getId());
                if (toRemove) {
                    author.getEbooks().remove(ebook);
                }
                return toRemove;
            });
        }

        if (ebookUpdateDTO.imageUrls() != null) {
            List<String> newImageUrls = ebookUpdateDTO.imageUrls();
            ebook.getImages().removeIf(image -> !newImageUrls.contains(image.getUrl()));

            newImageUrls.stream()
                    .filter(url -> ebook.getImages().stream().noneMatch(image -> image.getUrl().equals(url)))
                    .forEach(url -> {
                        Image newImage = new Image();
                        newImage.setUrl(url);
                        ebook.addImage(newImage);
                    });
        }
        ebookRepository.save(ebook);

    }

    @Override
    public void deleteById(int id) {
        if (!ebookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ebook with id: " + id + " not found");
        }
        ebookRepository.deleteById(id);
    }

    private EbookUserPanelDTO mapToEbookUserPanelDTO(Ebook s){
        return new EbookUserPanelDTO(s.getId(),
                s.getTitle(),
                s.getImages().getFirst().getUrl(),
                s.getDownloadUrl()
        );
    }

    private EbookDTO mapToEbookDTO(Ebook s) {
        return new EbookDTO(
                s.getId(),
                s.getTitle(),
                s.getDescription(),
                s.getPublishedYear(),
                s.getCategory().getName(),
                s.getPrice(),
                s.getAuthors().stream().map(Author::getFullName).toList(),
                s.getImages().stream().map(Image::getUrl).toList()
        );
    }
}

