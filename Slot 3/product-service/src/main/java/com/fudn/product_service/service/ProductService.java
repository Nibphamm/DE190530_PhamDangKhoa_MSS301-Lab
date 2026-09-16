package com.fudn.product_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.dto.ProductResponse;
import com.fudn.product_service.model.Product;
import com.fudn.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

	private final ProductRepository productRepository;

	public ProductResponse createProduct(ProductRequest productRequest) {
		Product product = Product.builder()
				.name(productRequest.name())
				.description(productRequest.description())
				.price(productRequest.price())
				.build();

		Product savedProduct = productRepository.save(product);
		log.info("Product {} is saved", savedProduct.getId());

		return mapToProductResponse(savedProduct);
	}

	public List<ProductResponse> getAllProducts() {
		return productRepository.findAll()
				.stream()
				.map(this::mapToProductResponse)
				.toList();
	}

	private ProductResponse mapToProductResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice());
	}
}
