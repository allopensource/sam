package com.sam.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;

public class Test {

	public static void main(String... strings) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		
	      SecureRandom random = new SecureRandom();
	      byte bytes[] = new byte[32];
	      random.nextBytes(bytes);
	      System.out.println(Base64.encodeBase64String(bytes));

		System.out.println(createEncodedDigest());
	}

	private static String getHMACKeyForSAGA() {
		System.out.println(RandomStringUtils.randomAlphanumeric(32));
		return RandomStringUtils.random(32);
	}

	private static String createEncodedDigest() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
		String payload = null;
		String sagaKey = getHMACKeyForSAGA();
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(sagaKey.getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		payload = "";
		return Base64.encodeBase64String(sha256_HMAC.doFinal(payload.getBytes()));
	}

}
