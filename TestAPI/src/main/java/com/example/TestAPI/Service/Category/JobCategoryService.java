package com.example.TestAPI.Service.Category;

import com.example.TestAPI.DTO.Category.CategoryResponse;
import com.example.TestAPI.DTO.Category.CategoryTreeResponse;

import java.util.List;
import java.util.UUID;

public interface JobCategoryService {
    List<CategoryTreeResponse> getCategoryTree();
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getSubcategories(UUID parentId);
    CategoryResponse getCategoryById(UUID id);
}
