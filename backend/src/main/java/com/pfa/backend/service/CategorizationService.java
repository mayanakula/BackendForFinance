package com.pfa.backend.service;

import com.pfa.backend.domain.Category;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CategorizationService {

	private final Map<String, Category> keywordRules = new LinkedHashMap<>();

	public CategorizationService() {
		// Order matters: first match wins
		add(Category.FOOD_AND_DINING, "restaurant", "cafe", "coffee", "uber eats", "doordash", "grubhub", "pizza", "mcdonald", "starbucks");
		add(Category.TRANSPORTATION, "uber", "lyft", "gas", "fuel", "petrol", "metro", "train", "bus", "parking", "toll");
		add(Category.SHOPPING, "amazon", "walmart", "target", "mall", "store", "shopping", "electronics", "clothing");
		add(Category.BILLS_AND_UTILITIES, "rent", "mortgage", "electric", "water", "internet", "wifi", "utility", "phone", "insurance");
		add(Category.ENTERTAINMENT, "netflix", "spotify", "movie", "cinema", "concert", "game", "steam");
		add(Category.HEALTHCARE, "pharmacy", "hospital", "clinic", "doctor", "dental", "medical");
		add(Category.INVESTMENTS, "brokerage", "etf", "stock", "crypto", "coinbase", "robinhood", "vanguard");
	}

	private void add(Category category, String... keywords) {
		for (String k : keywords) keywordRules.put(k, category);
	}

	public Category categorize(String merchant, String description) {
		String haystack = ((merchant == null ? "" : merchant) + " " + (description == null ? "" : description)).toLowerCase();
		for (Map.Entry<String, Category> e : keywordRules.entrySet()) {
			if (haystack.contains(e.getKey())) return e.getValue();
		}
		return Category.OTHERS;
	}
}

