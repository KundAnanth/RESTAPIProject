package com.demo.products.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.demo.products.model.ColorSwatch;
import com.demo.products.model.Product;

/*
 * This class is implementation of the Product Service
 * 
 * @author Ananth Kundurthi
 * 
 */
@Service("productsService")
public class ProductsServiceImpl implements ProductsService {

	public static enum priceLabelType {
		SHOWWASNOW, SHOWWASTHENNOW, SHOWPERCDSCOUNT
	};
	
	
	/*
	 * Retrieves the Products which have price reduction in the Given Category
	 * 
	 * @param categoryId Category Id for which the products are requested
	 * @param labelType used for formatting the Price Label
	 * 
	 * @return List<Product> List of all products that have price reductions
	 * 
	 */
	@Override
	public List<Product> findDiscountProducts(long categoryId, String labelType) {

		List<Product> products = new LinkedList<Product>();
		SortedMap<Float, Product> productsMap = new TreeMap<Float, Product>(Collections.reverseOrder());

		JSONObject productsInfo = null;
		try {
			productsInfo = readJsonFromUrl(
					"https://jl-nonprod-syst.apigee.net/v1/categories/" + categoryId + "/products?key=2ALHCAAs6ikGRBoy6eTHA58RaG097Fma");
			// System.out.println(productsInfo);
		} catch (Exception exp) {
			// error occurred while accessing the URL return empty products list
			return products;
		}

		JSONArray productList = productsInfo.getJSONArray("products");
		for (int i = 0; i < productList.length(); i++) {
			JSONObject product = productList.getJSONObject(i);
			JSONObject price = product.getJSONObject("price");
			String wasPrice = price.getString("was");
			
			String nowPrice = "";
			
			//ASSUMPTION:
			//now price has from and to values sometimes and hence in this
			//case considering the to price which is the highest price
			try {
				nowPrice = price.getString("now");
			} catch (Exception exp) {
				//This is the case where the now price is from and to value
				//hence setting to the to value
				JSONObject nowPriceObj = price.getJSONObject("now");
				nowPrice = nowPriceObj.getString("to");
			}	
			
		
			
			if (!wasPrice.isEmpty()) {
				Product newProduct = new Product();
				newProduct.setNowPrice(nowPrice);
				newProduct.setProductId(product.getString("productId"));
				newProduct.setTitle(product.getString("title"));
				String priceLabel = "";				
				if (labelType != null) {
					if (priceLabelType.SHOWWASNOW.toString().equalsIgnoreCase(labelType)) {
						priceLabel = "was " + formatPrice(wasPrice, price.getString("currency")) 
						             + ", now " + formatPrice(nowPrice, price.getString("currency"));
					} else if (priceLabelType.SHOWWASTHENNOW.toString().equalsIgnoreCase(labelType)) {
						String then = "";
						if(!price.getString("then2").isEmpty()) {
							then = ", then "  + price.getString("then2");
						} else if(!price.getString("then1").isEmpty()) {
							then = ", then "  + price.getString("then1");
						} 
						priceLabel = "was " + formatPrice(wasPrice, price.getString("currency")) 
			             + then + ", now " + formatPrice(nowPrice, price.getString("currency"));
					} else if (priceLabelType.SHOWPERCDSCOUNT.toString().equalsIgnoreCase(labelType)) {
						String disc = getDiscountPerc(wasPrice, nowPrice);
						priceLabel = disc + "% off -"
			             + " now " + formatPrice(nowPrice, price.getString("currency"));
					}
				} else {
					priceLabel = "was " + formatPrice(wasPrice, price.getString("currency")) 
		             + ", now " + formatPrice(nowPrice, price.getString("currency"));
				}
				
				newProduct.setPriceLabel(priceLabel);
				
				JSONArray productColorSwtaches = product.getJSONArray("colorSwatches");
				List<ColorSwatch> colorSwatches = new ArrayList<ColorSwatch>();
				for (int j = 0; j < productColorSwtaches.length(); j++) {
					ColorSwatch colorSwatch = new ColorSwatch();
					colorSwatch.setColor(productColorSwtaches.getJSONObject(j).getString("color"));
					colorSwatch.setSkuid(productColorSwtaches.getJSONObject(j).getString("skuId"));
					colorSwatch.setRgbColor(colorToRGB(productColorSwtaches.getJSONObject(j).getString("basicColor")));
					colorSwatches.add(colorSwatch);
				}
				newProduct.setColorSwatches(colorSwatches);
				
				productsMap.put((Float.valueOf(wasPrice) - Float.valueOf(nowPrice)), newProduct);
				
			}			
		}
		
		products = new LinkedList<Product>(productsMap.values());
		
		return products;
	}
	
	
	/*
	 * Reads JSON from given URL and return JSONObject for the same
	 * 
	 * @param URL URL to get the JSON from
	 *  
	 * @return JSONObject JSON Object representation of the JSON returned from URL
	 * 
	 */
	public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}
	
	/*
	 * Read all content from the Reader and return as String
	 * 
	 * @param reader Reader object for reading the content
	 * 
	 * @return String all content read from the reader in String format
	 * 
	 */
	private String readAll(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = reader.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
	
	/*
	 * Format the price based on Currency and also the type of price
	 * If Integer value then some changes needs to be made 
	 * 
	 * @param price Unformatted price value
	 * @param currency Currency for the price
	 * 
	 * @return String Formatted Price with Currency Symbol prefixed
	 * 
	 */
	private String formatPrice(String price, String currency) {
		String formattedPrice = "";
		if (isInteger(price)) {
			if (Integer.parseInt(price) > 10) {
				formattedPrice = getCurrencyChar(currency) + price;
			} else {
				formattedPrice = getCurrencyChar(currency) + price + ".00";
			}
		} else {
			formattedPrice = getCurrencyChar(currency) + price;
		}
		return formattedPrice;
	}
	
	/*
	 * Method to check whether the given rpice is Integer value
	 * 
	 * @param price Unformatted price value
	 * 
	 * @return boolean Determines if price is integer or not
	 * 
	 */
	private boolean isInteger(String price) {
		Pattern p = Pattern.compile("[0-9]+");
		Matcher m = p.matcher(price);
		boolean b = m.matches();
		return b;
	}
	
	/*
	 * Method to get currency symbol from the currency value
	 * 
	 * @param currency Currency value in string format
	 * 
	 * @return String Currency symbol
	 * 
	 */
	private String getCurrencyChar(String currency) {
		if (currency.equals("GBP")) {
			return "£";
		}
		return "";
	}
	
	/*
	 * Get Discount value given old and new price
	 * 
	 * @param oldPrice price before reduction
	 * @param newPrice price after reduction
	 * 
	 * @return String Discount Percentage
	 * 
	 */
	private String getDiscountPerc(String oldPrice, String newPrice) {
		return String.valueOf(Math.round((Float.valueOf(newPrice)/ Float.valueOf(oldPrice))*100));
	}
	
	
	/*
	 * Get RGB values for the Basic Colors
	 * 
	 * @param basicColor Basic Color
     *
	 * @return String RGB Value of the Basic Color
	 * 
	 */
	private String colorToRGB(String basicColor) {
	  String rgbValue;
	  if (basicColor == null) {
	      return "";
	  }
	  
	  if (basicColor.equalsIgnoreCase("Black"))
	    rgbValue = "000000";
	  else if(basicColor.equalsIgnoreCase("Silver"))
	    rgbValue = "C0C0C0";
	  else if(basicColor.equalsIgnoreCase("Grey"))
	    rgbValue = "808080";
	  else if(basicColor.equalsIgnoreCase("White"))
	    rgbValue = "FFFFFF";
	  else if(basicColor.equalsIgnoreCase("Maroon"))
	    rgbValue = "800000";
	  else if(basicColor.equalsIgnoreCase("Red"))
	    rgbValue = "FF0000";
	  else if(basicColor.equalsIgnoreCase("Purple"))
	    rgbValue = "800080";
	  else if(basicColor.equalsIgnoreCase("Fuchsia"))
	    rgbValue = "FF00FF";
	  else if(basicColor.equalsIgnoreCase("Green"))
	    rgbValue = "008000";
	  else if(basicColor.equalsIgnoreCase("Lime"))
	    rgbValue = "00FF00";
	  else if(basicColor.equalsIgnoreCase("Olive"))
	    rgbValue = "808000";
	  else if(basicColor.equalsIgnoreCase("Yellow"))
	    rgbValue = "FFFF00";
	  else if(basicColor.equalsIgnoreCase("Navy"))
	    rgbValue = "000080";
	  else if(basicColor.equalsIgnoreCase("Blue"))
	    rgbValue = "0000FF";
	  else if(basicColor.equalsIgnoreCase("Teal"))
	    rgbValue = "008080";
	  else if(basicColor.equalsIgnoreCase("Aqua"))
	    rgbValue = "00FFFF";
	  else if(basicColor.equalsIgnoreCase("Orange"))
	    rgbValue = "FF8000";
	  else
	      rgbValue = ""; // sometimes get specified without leading #
	  return rgbValue;
	}
}
