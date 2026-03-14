package com.pfa.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Hashing {
	private Hashing() {}

	public static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to compute hash", e);
		}
	}
}

