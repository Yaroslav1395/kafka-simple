package sakhno.springframework.ms.KafkaMicroservice.service;

import sakhno.springframework.ms.KafkaMicroservice.dto.CreateProductDto;

import java.util.concurrent.ExecutionException;

public interface ProductService {

    String createProduct(CreateProductDto createProductDto) throws ExecutionException, InterruptedException;
}
