package com.pfa.backend.service;

import org.springframework.stereotype.Service;

@Service
public class MerchantExtractionService {
	public String extractMerchant(String description) {
		if (description == null) return null;
		String s = description.trim();
		if (s.isEmpty()) return null;

		String[] splitters = new String[] {" - ", " * ", "  ", "|"};
		for (String sp : splitters) {
			int idx = s.indexOf(sp);
			if (idx > 0) return clean(s.substring(0, idx));
		}
		// Fall back to first 32 chars (merchant-ish)
		return clean(s.length() > 32 ? s.substring(0, 32) : s);
	}

	private String clean(String s) {
		String v = s.trim().replaceAll("\\s+", " ");
		return v.isEmpty() ? null : v;
	}
}

