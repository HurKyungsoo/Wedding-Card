package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * base64 사진 → 파일 URL 변환.
 * 하객 화면 HTML 에 사진을 통째로 박던 것(갤러리 30장이면 수 MB)을 없애는 핵심이라
 * 형식별 입력, 중복 제거, 이미지가 아닌 값 거르기를 고정한다.
 */
class PhotoExternalizerTest {

    /** 가장 작은 JPEG 시그니처 + 몇 바이트 — 매직 바이트 판별만 통과하면 된다 */
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    static final byte[] PNG  = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 9, 9};
    static final String JPEG_B64 = Base64.getEncoder().encodeToString(JPEG);
    static final String PNG_B64  = Base64.getEncoder().encodeToString(PNG);

    @TempDir Path dir;
    PhotoExternalizer ex;

    @BeforeEach
    void setUp() {
        ex = new PhotoExternalizer(new PhotoStorage(dir.toString()));
    }

    @Test
    void rawBase64_isStoredAsFile() throws Exception {
        String url = ex.toUrl(JPEG_B64);
        assertThat(url).matches("/uploads/[0-9a-f]{64}\\.jpg");
        assertThat(Files.readAllBytes(dir.resolve(url.substring("/uploads/".length())))).isEqualTo(JPEG);
    }

    @Test
    void dataUrl_isStoredWithRealFormat() {
        // 라벨은 jpeg 라고 해도 실제 내용이 PNG 면 png 로 저장한다
        assertThat(ex.toUrl("data:image/jpeg;base64," + PNG_B64)).endsWith(".png");
    }

    @Test
    void samePhoto_isStoredOnce() throws Exception {
        String a = ex.toUrl(JPEG_B64);
        String b = ex.toUrl("data:image/jpeg;base64," + JPEG_B64);
        assertThat(a).isEqualTo(b);
        try (var files = Files.list(dir)) {
            assertThat(files.count()).isEqualTo(1);
        }
    }

    @Test
    void urlsAndNonImages_areLeftAsIs() throws Exception {
        assertThat(ex.toUrl("/uploads/abc.jpg")).isEqualTo("/uploads/abc.jpg");
        assertThat(ex.toUrl("/images/demo/basic.jpg")).isEqualTo("/images/demo/basic.jpg");
        assertThat(ex.toUrl("https://example.com/a.jpg")).isEqualTo("https://example.com/a.jpg");
        assertThat(ex.toUrl("Zm9v")).isEqualTo("Zm9v");                    // "foo" — 이미지 아님
        assertThat(ex.toUrl("data:text/html;base64,PHNjcmlwdD4=")).isEqualTo("data:text/html;base64,PHNjcmlwdD4=");
        assertThat(ex.toUrl("not base64 !!")).isEqualTo("not base64 !!");
        assertThat(ex.toUrl("")).isEqualTo("");
        assertThat(ex.toUrl(null)).isNull();
        try (var files = Files.list(dir)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void externalize_convertsAllPhotoFields() {
        WeddingDto d = new WeddingDto();
        d.setMainPhotoBase64(JPEG_B64);
        d.setEndingPhotoBase64(JPEG_B64);
        d.setEndingPhotos(JPEG_B64 + "|||" + PNG_B64);
        d.setGalleryImages("data:image/jpeg;base64," + JPEG_B64 + "|||/images/demo/basic.jpg|||data:image/png;base64," + PNG_B64);

        assertThat(ex.externalize(d)).isTrue();

        assertThat(d.getMainPhotoBase64()).startsWith("/uploads/");
        assertThat(d.getEndingPhotoBase64()).isEqualTo(d.getMainPhotoBase64());
        assertThat(d.getEndingPhotos().split("\\|\\|\\|")).hasSize(2).allMatch(s -> s.startsWith("/uploads/"));
        String[] gal = d.getGalleryImages().split("\\|\\|\\|");
        assertThat(gal).hasSize(3);
        assertThat(gal[0]).startsWith("/uploads/");
        assertThat(gal[1]).isEqualTo("/images/demo/basic.jpg");   // 순서 유지
        assertThat(gal[2]).endsWith(".png");

        // 두 번째부터는 바뀔 게 없다
        assertThat(ex.externalize(d)).isFalse();
    }

    @Test
    void photoSrc_handlesUrlsAndLegacyBase64() {
        assertThat(WeddingDto.photoSrc("/uploads/a.jpg")).isEqualTo("/uploads/a.jpg");
        assertThat(WeddingDto.photoSrc("Zm9v")).isEqualTo("data:image/jpeg;base64,Zm9v");
        assertThat(WeddingDto.photoSrc("data:image/png;base64,AA")).isEqualTo("data:image/png;base64,AA");
        assertThat(WeddingDto.photoSrc(null)).isEmpty();
    }
}
