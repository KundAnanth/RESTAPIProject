package com.demo.products.controller;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.products.model.Product;
import com.demo.products.service.ProductsService;
import com.demo.products.util.ErrorType;

/*
 * Product Controller class which deals with the Product related API
 * 
 * @author Ananth Kundurthi
 * 
 */

@RestController
@RequestMapping("/v1")
public class ProductsController {

	@Autowired
	ProductsService productsService;	
		
	/*
	 * Product Controller class which deals with the Product related API
	 * 
	 * @param categoryId Category Id for which the products are requested
	 * @param labelType uSed for formatting the Price Label
	 * 
	 * @return List<Product> List of all products that have price reductions
	 * 
	 */
	@RequestMapping(value = "/categories/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> findProducts(@PathVariable("id") long categoryId,@RequestParam(value= "labelType", required=false) String labelType) {
		List<Product> redProducts = productsService.findDiscountProducts(categoryId, labelType);
	
		JSONObject products = new JSONObject();
		products.put("products", new JSONArray(redProducts));
		
		if (products.length() == 0) {
			return new ResponseEntity<ErrorType>(new ErrorType("ERROR: Id =" + categoryId 
					+ " not found"), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<String>(products.toString(), HttpStatus.OK);
	}
}