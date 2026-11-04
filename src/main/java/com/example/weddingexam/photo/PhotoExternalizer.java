package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * 청첩장 데이터 안의 base64 사진을 파일로 빼고 URL 로 바꿔 넣는다.
 *
 * 예전에는 사진을 base64 문자열로 DB 에 넣고 청첩장 HTML 에 통째로 박았다 — 갤러리 30장이면
 * 하객이 열 때마다 몇 MB 짜리 HTML 을 받고, 캐시도 안 됐다. 저장 시점에 여기서 파일로 바꾼다.
 *
 * 들어올 수 있는 값
 *  - data:image/...;base64,...   (갤러리 — 편집기가 만든 data URL)
 *  - 접두어 없는 base64          (메인·엔딩 사진 — 예전 형식)
 *  - /uploads/..., /images/..., http(s)://...  (이미 URL — 그대로 둔다)
 * 이미지가 아니거나 너무 크면 바꾸지 않고 원래 값을 둔다(데이터를 잃지 않게).
 */
@Component
public class PhotoExternalizer {

    /** 디코딩 후 사진 한 장 최대 크기 — 편집기가 1600px 로 줄여 보내므로 넉넉하다 */
    static final int MAX_BYTES = 15 * 1024 * 1024;
    private static final String SEP = "|||";

    private final PhotoStorage storage;

    public PhotoExternalizer(PhotoStorage storage) {
        this.storage = storage;
    }

    /** 사진 필드 네 곳을 URL 로 바꾼다. 하나라도 바뀌었으면 true */
    public boolean externalize(WeddingDto d) {
        if (d == null) return false;
        String main = toUrl(d.getMainPhotoBase64());
        String endingOne = toUrl(d.getEndingPhotoBase64());
        String gallery = toUrlList(d.getGalleryImages());
        String endingAll = toUrlList(d.getEndingPhotos());
        boolean changed = !eq(main, d.getMainPhotoBase64()) || !eq(endingOne, d.getEndingPhotoBase64())
                || !eq(gallery, d.getGalleryImages()) || !eq(endingAll, d.getEndingPhotos());
        d.setMainPhotoBase64(main);
        d.setEndingPhotoBase64(endingOne);
        d.setGalleryImages(gallery);
        d.setEndingPhotos(endingAll);
        return changed;
    }

    /** '|||' 로 이은 여러 장 */
    public String toUrlList(String joined) {
        if (joined == null || joined.isBlank() || !mayContainInline(joined)) return joined;
        return Arrays.stream(joined.split("\\|\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::toUrl)
                .collect(Collectors.joining(SEP));
    }

    /** 한 장 — base64 면 파일로 저장하고 URL, 아니면 그대로 */
    public String toUrl(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty() || isUrl(v)) return value;

        String payload = v;
        if (v.startsWith("data:")) {
            int comma = v.indexOf(',');
            if (comma < 0 || !v.substring(0, comma).endsWith(";base64")) return value;
            payload = v.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            return value;
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) return value;
        String ext = imageExt(bytes);
        if (ext == null) return value;      // 이미지가 아니면 저장하지 않는다
        return storage.store(bytes, ext);
    }

    /**
     * 이미 URL 인가. 주의: JPEG 의 base64 는 항상 "/9j/" 로 시작한다 — '/' 로 시작한다고 경로로 보면 안 된다.
     * base64 에 없는 문자('.', '?', '-', '_', '%', ':')가 있어야 경로로 본다 (/uploads/x.jpg, /images/demo/a.jpg).
     */
    public static boolean isUrl(String v) {
        if (v.startsWith("http://") || v.startsWith("https://")) return true;
        if (!v.startsWith("/")) return false;
        for (int i = 0; i < v.length(); i++) {
            if (".?-_%:".indexOf(v.charAt(i)) >= 0) return true;
        }
        return false;
    }

    /** 이미 전부 URL 인 목록은 split 없이 빨리 넘긴다 */
    private static boolean mayContainInline(String joined) {
        for (String s : joined.split("\\|\\|\\|")) {
            String t = s.trim();
            if (!t.isEmpty() && !isUrl(t)) return true;
        }
        return false;
    }

    /** 파일 앞부분(매직 바이트)으로 실제 이미지 형식을 판별 — 확장자·라벨은 믿지 않는다 */
    static String imageExt(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        return null;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
