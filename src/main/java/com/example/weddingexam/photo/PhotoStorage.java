package com.example.weddingexam.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 업로드 사진을 서버 디스크에 파일로 저장한다.
 *
 * 파일 이름은 내용의 SHA-256 해시 — 같은 사진은 한 번만 저장되고(중복 제거),
 * 내용이 바뀌면 이름(URL)도 바뀌므로 브라우저가 1년 동안 캐시해도 안전하다.
 * 저장 위치는 app.upload.dir (기본 ./uploads — 배포 때 jar 만 바뀌고 이 폴더는 그대로 남는다).
 */
@Component
public class PhotoStorage {

    /** 하객 화면에서 쓰는 URL 접두어 — UploadResourceConfig 가 이 경로를 디스크 폴더에 연결한다 */
    public static final String URL_PREFIX = "/uploads/";

    private final Path dir;

    public PhotoStorage(@Value("${app.upload.dir:./uploads}") String dir) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
    }

    public Path dir() {
        return dir;
    }

    /** 저장하고 URL(/uploads/{hash}.{ext}) 을 돌려준다 — 이미 있으면 다시 쓰지 않는다 */
    public String store(byte[] bytes, String ext) {
        String name = sha256(bytes) + "." + ext;
        Path target = dir.resolve(name);
        try {
            if (!Files.exists(target)) {
                Files.createDirectories(dir);
                // 임시 파일에 다 쓴 뒤 옮긴다 — 쓰는 도중에 요청이 와서 반쯤 잘린 사진을 받는 일이 없게
                Path tmp = Files.createTempFile(dir, "up-", ".tmp");
                Files.write(tmp, bytes);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장하지 못했습니다", e);
        }
        return URL_PREFIX + name;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
