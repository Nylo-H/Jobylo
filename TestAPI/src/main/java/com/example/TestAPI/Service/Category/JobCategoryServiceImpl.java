package com.example.TestAPI.Service.Category;

import com.example.TestAPI.DTO.Category.CategoryResponse;
import com.example.TestAPI.DTO.Category.CategoryTreeResponse;
import com.example.TestAPI.Model.JobCategory;
import com.example.TestAPI.Repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobCategoryServiceImpl implements JobCategoryService {

    private final JobCategoryRepository categoryRepository;

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrderAsc()
                .stream()
                .map(this::toTreeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getSubcategories(UUID parentId) {
        return categoryRepository.findByParentIdOrderByDisplayOrderAsc(parentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        JobCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        return toResponse(category);
    }

    private CategoryTreeResponse toTreeResponse(JobCategory category) {
        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getDisplayOrder(),
                category.getSubcategories() != null
                        ? category.getSubcategories().stream()
                                .map(this::toTreeResponse)
                                .collect(Collectors.toList())
                        : List.of()
        );
    }

    private CategoryResponse toResponse(JobCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getDisplayOrder()
        );
    }
}
