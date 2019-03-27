package com.demo.products.service;


import java.util.List;
import com.demo.products.model.Product;

/*
 * This interface defines the methods the Product Service exposes
 * 
 * @author Ananth Kundurthi
 * 
 */

public interface ProductsService {
	
	/*
	 * Retrieves the Products which have price reduction in the Given Category
	 * 
	 * @param categoryId Category Id for which the products are requested
	 * @param labelType used for formatting the Price Label
	 * 
	 * @return List<Product> List of all products that have price reductions
	 * 
	 */
	List<Product> findDiscountProducts(long categoryId, String labelType);
	
}