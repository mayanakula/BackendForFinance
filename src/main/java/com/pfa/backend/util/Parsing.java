package com.pfa.backend.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class Parsing {
	private Parsing() {}

	private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
		DateTimeFormatter.ISO_LOCAL_DATE,
		DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US),
		DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US),
		DateTimeFormatter.ofPattern("d/M/uuuu", Locale.UK),
		DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.UK),
		DateTimeFormatter.ofPattern("uuuuMMdd")
	);

	public static LocalDate parseDateLenient(String raw) {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.isEmpty()) return null;
		for (DateTimeFormatter f : DATE_FORMATS) {
			try {
				return LocalDate.parse(s, f);
			} catch (DateTimeParseException ignored) {
			}
		}
		throw new IllegalArgumentException("Unsupported date format: " + raw);
	}

	public static BigDecimal parseAmountLenient(String raw) {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.isEmpty()) return null;
		// Remove currency symbols and thousands separators
		s = s.replaceAll("[,$₹€£\\s]", "");
		// Handle parentheses as negative
		boolean negative = s.startsWith("(") && s.endsWith(")");
		if (negative) s = s.substring(1, s.length() - 1);
		BigDecimal v = new BigDecimal(s);
		return negative ? v.negate() : v;
	}

	public static String normalizeDescription(String raw) {
		if (raw == null) return "";
		return raw.trim().replaceAll("\\s+", " ");
	}
}

