package sakhno.springframework.ms.KafkaMicroservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sakhno.springframework.ms.KafkaMicroservice.service.ProductService;
import sakhno.springframework.ms.KafkaMicroservice.dto.CreateProductDto;
import sakhno.springframework.ms.KafkaMicroservice.dto.ErrorMessage;

import java.util.Date;

@RestController
@RequestMapping("/product")
public class ProductController {
    private ProductService productService;
    private final static Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Object> createProduct(@RequestBody CreateProductDto createProductDto){
        String productId = null;
        try {
            productId = productService.createProduct(createProductDto);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage(new Date(), e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }
}
