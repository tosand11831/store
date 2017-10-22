package com.sander.store.pojo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes a simple Category containing a unique id,
 * a name, a distinct url (modelling category path) and a
 * set of associated products
 */
public class Category {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String categoryPath;

    @Indexed
    private Set<String> products = new HashSet<>();

    @Version
    private Long version;

    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", categoryPath='" + categoryPath + '\'' +
                ", products=" + products +
                ", version=" + version +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public Set<String> getProducts() {
        return products;
    }

    public void setProducts(Set<String> products) {
        this.products = products;
    }

    public void removeProduct(String id) {
        products.remove(id);
    }

    public void addProduct(String id) {
        products.add(id);
    }

    public void addProducts(List<String> productIds) {
        products.addAll(productIds);
    }

    public void removeProducts(List<String> productIds) {
        products.removeAll(productIds);
    }

    public void updateCategoryPath(String oldPathPath, String pathPart) {
        this.categoryPath = categoryPath.replaceFirst(oldPathPath, pathPart);
    }
}
