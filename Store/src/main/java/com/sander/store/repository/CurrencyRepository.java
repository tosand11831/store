package com.sander.store.repository;

import com.sander.store.currency.CurrencyRate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CurrencyRepository extends MongoRepository<CurrencyRate, String> {}
