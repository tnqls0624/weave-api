package com.weave.domain.auth.jwt;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyLoaderUtils {

    /**
     * RSA 개인키 로드
     * - PEM 파일 경로가 비어 있으면 null 반환
     * - 파일이 있으면 PKCS8 포맷으로 로드
     */
    public static PrivateKey loadPrivateKey(String path) {
        if (path == null || path.isBlank()) return null;

        try {
            String key = new String(Files.readAllBytes(Paths.get(path)))
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("RSA PrivateKey 로드 실패: " + path, e);
        }
    }

    /**
     * RSA 공개키 로드
     * - PEM 파일 경로가 비어 있으면 null 반환
     * - 파일이 있으면 X.509 포맷으로 로드
     */
    public static PublicKey loadPublicKey(String path) {
        if (path == null || path.isBlank()) return null;

        try {
            String key = new String(Files.readAllBytes(Paths.get(path)))
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("RSA PublicKey 로드 실패: " + path, e);
        }
    }
}