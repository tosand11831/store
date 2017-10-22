package com.sander.store.repository;

import com.sander.store.pojo.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface ProductRepository extends MongoRepository<Product, String>{
    public Collection<Product> findByName(String name);
}
