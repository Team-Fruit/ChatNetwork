package net.teamfruit.chatnetwork.util;

import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.util.Base64;

public class Base64Utils {
	public static String encode(final long num) {
		return Base64.getEncoder().encodeToString(BigInteger.valueOf(num).toByteArray());
	}

	public static long decode(final String str) throws IllegalArgumentException {
		Validate.notEmpty(str);
		return new BigInteger(Base64.getDecoder().decode(str)).longValue();
	}
}
