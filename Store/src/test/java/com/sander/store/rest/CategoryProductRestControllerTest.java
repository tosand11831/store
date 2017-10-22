package com.sander.store.rest;

import com.sander.store.StoreApplication;
import com.sander.store.currency.CurrencyConversionProvider;
import com.sander.store.pojo.Category;
import com.sander.store.pojo.Product;
import com.sander.store.repository.CategoryRepository;
import com.sander.store.repository.ProductRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StoreApplication.class)
@WebAppConfiguration
public class CategoryProductRestControllerTest {

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void shouldCreateReadUpdateDeleteSomeProducts() throws Exception {

        clear();

        // should be empty at beginning
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // add some products
        Product product1 = new Product();
        product1.setName("Product1");
        product1.setValue(99.99);

        mockMvc.perform(put("/products").content(this.json(product1)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("name", is("Product1")))
                .andExpect(jsonPath("value", is(99.99)))
                .andExpect(jsonPath("currencyIso", is(CurrencyConversionProvider.EUR)));

        Product product2 = new Product();
        product2.setName("Product2");
        product2.setValue(99.99);
        product2.setCurrencyIso("USD");

        mockMvc.perform(put("/products").content(this.json(product2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("name", is("Product2")))
                .andExpect(jsonPath("value", is(99.99)))
                .andExpect(jsonPath("currencyIso", is("USD")));

        // should not add products without name or value
        Product product3 = new Product();
        mockMvc.perform(put("/products").content(this.json(product3)))
                .andExpect(status().isBadRequest());

        product3.setName("test");
        mockMvc.perform(put("/products").content(this.json(product3)))
                .andExpect(status().isBadRequest());

        // should get all products
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", iterableWithSize(2)));

        // should get all products in distinct currency
        mockMvc.perform(get("/products?currencyIso=BGN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$[0].currencyIso", is("BGN")))
                .andExpect(jsonPath("$[1].currencyIso", is("BGN")));

        List<Product> products = productRepository.findAll();

        // should return a single product by id or name
        mockMvc.perform(get("/products?id=" + products.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$[0].id", is(products.get(0).getId())));

        mockMvc.perform(get("/products?name=" + products.get(1).getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$[0].name", is(products.get(1).getName())));

        Product testProduct;

        if(products.get(0).getCurrencyIso().equals("EUR"))
            testProduct = products.get(0);
        else
            testProduct = products.get(1);

        // get single product with adapted currency
        mockMvc.perform(get("/products?currencyIso=USD&id=" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$[0].id", is(testProduct.getId())))
                .andExpect(jsonPath("$[0].currencyIso", is("USD")));

        // should update project using id, update name or value but updating currency is currently not allowed
        mockMvc.perform(post("/products?id=" + testProduct.getId())
                    .content("{\"name\":\"Product1Update\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("Product1Update")));

        // should update value
        mockMvc.perform(post("/products?id=" + testProduct.getId())
                    .content("{\"value\":10000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("value", is(10000.0)));

        // should not update because invalid value
        mockMvc.perform(post("/products?id=" + testProduct.getId())
                    .content("{\"value\":-121212}"))
                .andExpect(status().isBadRequest());

        // should not update because not found
        mockMvc.perform(post("/products?id=hhndhdhdndhdhdnhdbs")
                        .content("{\"name\":\"NewProduct\"}"))
                .andExpect(status().isNotFound());

        // should delete project
        mockMvc.perform(delete("/products?id=" + testProduct.getId()))
                .andExpect(status().isOk());

        // now there should only be one entry inside product repository
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", iterableWithSize(1)));
    }

    @Test
    public void createReadUpdateDeleteCategories() throws Exception {

        clear();

        // add some products first
        // add some products
        Product product1 = new Product();
        product1.setName("Product1");
        product1.setValue(99.99);

        mockMvc.perform(put("/products").content(this.json(product1)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("name", is("Product1")))
                .andExpect(jsonPath("value", is(99.99)))
                .andExpect(jsonPath("currencyIso", is(CurrencyConversionProvider.EUR)));

        Product product2 = new Product();
        product2.setName("Product2");
        product2.setValue(99.99);
        product2.setCurrencyIso("USD");

        mockMvc.perform(put("/products").content(this.json(product2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("name", is("Product2")))
                .andExpect(jsonPath("value", is(99.99)))
                .andExpect(jsonPath("currencyIso", is("USD")));

        Product product3 = new Product();
        product3.setName("Product3");
        product3.setValue(1000.99);
        product3.setCurrencyIso("EUR");

        mockMvc.perform(put("/products").content(this.json(product3)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("name", is("Product3")))
                .andExpect(jsonPath("value", is(1000.99)))
                .andExpect(jsonPath("currencyIso", is("EUR")));

        List<Product> products = productRepository.findAll();

        // should create a category
        mockMvc.perform(put("/categories/category1").content(addProductId(products.get(0).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("category1")))
                .andExpect(jsonPath("categoryPath", is("category1")));

        // should create a category with a subcategory
        mockMvc.perform(put("/categories/category1_subCategory").content(addProductId(products.get(1).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("subCategory")))
                .andExpect(jsonPath("categoryPath", is("category1_subCategory")));

        // should create a category without products
        mockMvc.perform(put("/categories/category2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("category2")))
                .andExpect(jsonPath("categoryPath", is("category2")));

        // should return all categories
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", iterableWithSize(3)));

        // should return category1 and subcategory
        mockMvc.perform(get("/categories/category1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", iterableWithSize(2)));

        // should return Product1 associated with Category1
        mockMvc.perform(get("/categories/category1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Product1")));

        // should return Product2 associated with SubCategory
        mockMvc.perform(get("/categories/category1_subCategory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Product2")));

        // should return 404 because no product is associated with Category2
        mockMvc.perform(get("/categories/category2/products"))
                .andExpect(status().isNotFound());

        // should add (update) a subcategory2 to a existing category1
        mockMvc.perform(put("/categories/category1_subCategory2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("categoryPath", is("category1_subCategory2")));

        // category1 should now have 2 subcategories, return category1 and both subcategories
        mockMvc.perform(get("/categories/category1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", iterableWithSize(3)));

        // change name of category1 to category1Changed, all subcategories should now be adapted as well
        mockMvc.perform(post("/categories/category1").content("{\"name\":\"category1Changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("category1Changed")));

        mockMvc.perform(get("/categories/category1Changed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", iterableWithSize(3)));

        // should add Product3 to subCategory2
        mockMvc.perform(post("/categories/category1Changed_subCategory2/products/add").content(addProductId(products.get(2).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("products",  is(Arrays.asList(products.get(2).getId()))));

        // should remove Product1 from category1Changed
        mockMvc.perform(post("/categories/category1Changed/products/remove").content(addProductId(products.get(0).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("products", is(Collections.emptyList())));


        // should delete category
        mockMvc.perform(delete("/categories/category1Changed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("category1Changed")));
    }

    private void clear() {
        categoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    private String addProductId(String id) {
        return "{\"products\":[\"" + id + "\"]}";
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
