package com.sander.store.repository;

import com.sander.store.pojo.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {
    public List<Category> products(String id);
    public Category findByCategoryPath(String categoryPath);
    public List<Category> findByCategoryPathRegex(String url);
    public Category findByName(String name);
}
