package com.sander.store.rest;

import com.sander.store.currency.CurrencyConversionProvider;
import com.sander.store.pojo.Category;
import com.sander.store.pojo.Product;
import com.sander.store.repository.CategoryRepository;
import com.sander.store.repository.CurrencyRepository;
import com.sander.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class CategoryProductRestController {

    private static final String NAME = "name", CATEGORY = "category", PRODUCTS = "products",
        ADD = "add", REMOVE = "remove";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CurrencyRepository currencyRepository;
    private final CurrencyConversionProvider currencyConversionProvider;
    private final JacksonJsonParser jsonParser = new JacksonJsonParser();

    @Autowired
    public CategoryProductRestController(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CurrencyRepository currencyRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.currencyRepository = currencyRepository;
        currencyConversionProvider = new CurrencyConversionProvider(currencyRepository);
        currencyConversionProvider.updateCurrencies();
    }

    // categories

    @RequestMapping(value = "/categories/{categories}", method = RequestMethod.PUT)
    public ResponseEntity<Category> createCategory(
            @PathVariable(value = "categories") String categories,
            @RequestBody(required = false) String data
    ) {

        if(categoryRepository.findByCategoryPath(categories) != null)
            return new ResponseEntity<>(HttpStatus.ALREADY_REPORTED);

        // would be nicer to have a category hierarchy like C1/C2/C3/.../CN but but is not provided by Spring?
        // so: use C1_C2_C3_..._CN
        String[] pathParts = categories.substring(0, categories.length()).split("_");

        Category category = new Category();
        category.setName(pathParts[pathParts.length - 1]);
        category.setCategoryPath(categories);

        if(data != null) {

            Map<String, Object> dataMap = jsonParser.parseMap(data);

            if (dataMap.containsKey("products")) {

                List<String> products = (List<String>) dataMap.get(PRODUCTS);
                if (products != null)
                    category.setProducts(new HashSet<>((List<String>) dataMap.get(PRODUCTS)));
            }
        }

        return new ResponseEntity<>(categoryRepository.save(category), HttpStatus.OK);
    }

    /**
     * Return all categories
     * @return
     */
    @RequestMapping(value = "/categories", method = RequestMethod.GET)
    public ResponseEntity<Collection<Category>> readCategories() {
        return new ResponseEntity<>(categoryRepository.findAll(), HttpStatus.OK);
    }

    /**
     *
     * @param categories
     * @return
     */
    @RequestMapping(value = "/categories/{categories}", method = RequestMethod.GET)
    public ResponseEntity<Collection<Category>> readCategoriesAndSubCategories(
        @PathVariable() String categories
    ) {
        List<Category> response = categoryRepository.findByCategoryPathRegex("^" + categories);
        if(response == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        else
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/categories/{categories}/products", method = RequestMethod.GET)
    public ResponseEntity<Collection<Product>> readAssociatedProducts(@PathVariable String categories) {

        Category category = categoryRepository.findByCategoryPath(categories);

        if(category == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Iterable<Product> all = productRepository.findAll(category.getProducts());
        if(!all.iterator().hasNext())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        List<Product> target = new ArrayList<>();
        all.forEach(target::add);
        return new ResponseEntity<>(target, HttpStatus.OK);
    }

    /**
     * Update name of category, or description (if available), etc.
     * TODO also by id
     * @param categories
     * @param data
     * @return
     */
    @RequestMapping(value = "/categories/{categories}", method = RequestMethod.POST)
    public ResponseEntity<Category> updateCategoryCategoryPath(
            @PathVariable(value = "categories") String categories,
            @RequestBody() String data
    ) {
        Category category = categoryRepository.findByCategoryPath(categories);

        if(category == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Map<String, Object> dataMap = jsonParser.parseMap(data);

        if(dataMap.containsKey(NAME)) {
            String newName = (String) dataMap.get(NAME);
            if(newName != null) {
                String oldName = category.getName();
                category.setName(newName);
                categoryRepository.save(category);
                List<Category> subCategories = categoryRepository.findByCategoryPathRegex("^" + categories);
                subCategories.stream().forEach(sub -> sub.updateCategoryPath(oldName, newName));
                categoryRepository.save(subCategories);
            }
        }

        return new ResponseEntity<>(categoryRepository.findOne(category.getId()), HttpStatus.OK);
    }

    /**
     * Update products of distinct category using product path
     * TODO also by id
     * @param categories
     * @param data
     * @return
     */
    @RequestMapping(value = "/categories/{categories}/products/{action}", method = RequestMethod.POST)
    public ResponseEntity<Category> updateCategoryProducts(
            @PathVariable(value = "categories") String categories,
            @PathVariable(value = "action") String action,
            @RequestBody() String data
    ) {
        Category category = categoryRepository.findByCategoryPath(categories);

        if(categories == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Map<String, Object> dataMap = jsonParser.parseMap(data);
        List<String> products = null;

        if(!dataMap.containsKey(PRODUCTS)) {
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            products = (List<String>) dataMap.get(PRODUCTS);
            if(products == null)
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(action.equals(ADD)) {
            category.addProducts(products);
        } else if(action.equals(REMOVE)) {
            category.removeProducts(products);
        } else
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(categoryRepository.save(category), HttpStatus.OK);
    }

    /**
     * Delete distinct category by using category path
     * TODO delete also by id
     * @return
     */
    @RequestMapping(value = "/categories/{categories}", method = RequestMethod.DELETE)
    public ResponseEntity<Category> deleteCategory(@PathVariable(value = "categories") String categories) {

        // remove category and all subcategories, maybe extend that sub categories are moved under next parent category?
        Category category = categoryRepository.findByCategoryPath(categories);

        if(category == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        // delete sub categories
        List<Category> subCategories = categoryRepository.findByCategoryPathRegex("^" + category.getCategoryPath());
        subCategories.stream().forEach(cat -> categoryRepository.delete(cat.getId()));

        // delete category
        categoryRepository.delete(category);
        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    // products

    /**
     * Create a distinct product
     * @param data
     * @return
     */
    @RequestMapping(value = "/products", method = RequestMethod.PUT)
    public ResponseEntity<Product> createProduct(@RequestBody() String data) {

        Map<String, Object> dataMap = jsonParser.parseMap(data);

        String name, currencyIso = null;
        double value;

        if(!dataMap.containsKey(NAME))
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        else {
            name = (String) dataMap.get(NAME);
            if(name == null)
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(!dataMap.containsKey(CurrencyConversionProvider.VALUE))
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        else {
            value = (double) dataMap.get(CurrencyConversionProvider.VALUE);
            if(value <= 0)
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(!dataMap.containsKey(CurrencyConversionProvider.CURRENCY_ISO)) {
           currencyIso = CurrencyConversionProvider.EUR;
        } else {
            currencyIso = ((String) dataMap.get(CurrencyConversionProvider.CURRENCY_ISO));
            if(currencyIso == null)
                currencyIso = CurrencyConversionProvider.EUR;
            else
                if(!currencyIso.equals(CurrencyConversionProvider.EUR) && !currencyRepository.exists(currencyIso))
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Product product = productRepository.save(Product.newProduct(name, value, currencyIso));

        if(dataMap.containsKey(CATEGORY)) {
            String categoryId = (String) dataMap.get(CATEGORY);
            if(categoryId != null) {
                Category category = categoryRepository.findOne(categoryId);
                if(category != null) {
                    category.addProduct(product.getId());
                    categoryRepository.save(category);
                }
            }
        }

        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    /**
     * Get projects using id or name in combination with and without currencyIso
     * @param id
     * @param name
     * @param currencyIso
     * @return
     */
    @RequestMapping(value = "/products", method = RequestMethod.GET)
    public ResponseEntity<Collection<Product>> readProducts(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "currencyIso", required = false) String currencyIso
    ) {
        if(id == null && name == null) {
            List<Product> products = productRepository.findAll();

            if(currencyIso != null)
                products.stream().forEach(p -> currencyConversionProvider.adaptCurrency(currencyIso, p));

            return new ResponseEntity<>(products, HttpStatus.OK);
        }
        else if(id != null) {
            Product product = productRepository.findOne(id);

            if(currencyIso != null)
                currencyConversionProvider.adaptCurrency(currencyIso, product);

            return product == null ?
                    new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                    new ResponseEntity<>(Arrays.asList(product), HttpStatus.OK);
        } else {

            Collection<Product> products = productRepository.findByName(name);

            if(currencyIso != null)
                products.stream().forEach(p -> currencyConversionProvider.adaptCurrency(currencyIso, p));

            return products == null || products.isEmpty() ?
                    new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                    new ResponseEntity<>(products, HttpStatus.OK);
        }
    }

    /**
     * Update a distinct product, updating name and value is allowed, updating currencyIso is currently
     * not supported
     * @param data
     * @return
     */
    @RequestMapping(value = "/products", method = RequestMethod.POST)
    public ResponseEntity<Product> updateProduct(
            @RequestParam(value = "id") String id,
            @RequestBody() String data) {

        if(id == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Product product = productRepository.findOne(id);

        if(product == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

       try {
           product.update(jsonParser.parseMap(data));
       } catch(Exception ex) {
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
       }

       productRepository.save(product);
       return new ResponseEntity<>(product, HttpStatus.OK);
    }

    /**
     * Delete a distinct product
     * @param id
     * @return
     */
    @RequestMapping(value = "/products", method = RequestMethod.DELETE)
    public ResponseEntity<Product> deleteProduct(@RequestParam(value = "id") String id) {
        if(productRepository.exists(id))
            productRepository.delete(id);
        else
            return new ResponseEntity<Product>(HttpStatus.NOT_FOUND);

        Collection<Category> associatedCategories = categoryRepository.products(id);
        if(associatedCategories != null)
            associatedCategories.stream().forEach(cat -> cat.removeProduct(id));

        return new ResponseEntity<Product>(HttpStatus.OK);
    }
 }
