package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 카카오 공유 썸네일 — 세로 사진을 1:1 로 자르되 얼굴이 있는 위쪽을 남긴다.
 */
class ShareImageServiceTest {

    @TempDir Path dir;
    PhotoStorage storage;
    ShareImageService service;

    @BeforeEach
    void setUp() {
        storage = new PhotoStorage(dir.toString());
        service = new ShareImageService(storage);
    }

    /** 위쪽 절반은 빨강, 아래쪽 절반은 파랑인 세로 사진 */
    static byte[] portraitJpeg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);  g.fillRect(0, 0, w, h / 2);
        g.setColor(Color.BLUE); g.fillRect(0, h / 2, w, h - h / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private WeddingDto withMain(String main) {
        WeddingDto d = new WeddingDto();
        d.setMainPhotoBase64(main);
        return d;
    }

    @Test
    void portraitPhoto_becomesSquare800_keepingTheTop() throws Exception {
        String url = storage.store(portraitJpeg(1000, 1600), "jpg");

        String share = service.shareImagePath(withMain(url));

        assertThat(share).startsWith("/uploads/share/").endsWith(".jpg");
        BufferedImage out = ImageIO.read(dir.resolve(share.substring("/uploads/".length())).toFile());
        assertThat(out.getWidth()).isEqualTo(800);
        assertThat(out.getHeight()).isEqualTo(800);
        // 위쪽(얼굴 자리)을 더 남긴다 — 정가운데로 자르면 빨강·파랑이 반반, 위로 치우치면 빨강이 더 많다
        int redRows = 0;
        for (int y = 0; y < 800; y++) if (new Color(out.getRGB(400, y)).getRed() > 150) redRows++;
        assertThat(redRows).isGreaterThan(450);
    }

    @Test
    void legacyBase64_andStaticDemoPhoto_work() throws Exception {
        String b64 = java.util.Base64.getEncoder().encodeToString(portraitJpeg(600, 900));
        assertThat(service.shareImagePath(withMain(b64))).startsWith("/uploads/share/");
        assertThat(service.shareImagePath(withMain("/images/demo/forever.jpg"))).startsWith("/uploads/share/");
    }

    @Test
    void samePhoto_reusesTheSameFile() throws Exception {
        String url = storage.store(portraitJpeg(900, 1200), "jpg");
        assertThat(service.shareImagePath(withMain(url))).isEqualTo(service.shareImagePath(withMain(url)));
    }

    @Test
    void noPhoto_tooSmall_orUnreadable_fallsBack() throws Exception {
        assertThat(service.shareImagePath(withMain(null))).isEqualTo(ShareImageService.FALLBACK);
        assertThat(service.shareImagePath(withMain(""))).isEqualTo(ShareImageService.FALLBACK);
        assertThat(service.shareImagePath(withMain(storage.store(portraitJpeg(100, 150), "jpg"))))
                .isEqualTo(ShareImageService.FALLBACK);
        assertThat(service.shareImagePath(withMain("/uploads/" + "c".repeat(64) + ".jpg")))
                .isEqualTo(ShareImageService.FALLBACK);
        assertThat(service.shareImagePath(withMain("/uploads/../../etc/passwd")))
                .isEqualTo(ShareImageService.FALLBACK);
        assertThat(service.shareImagePath(withMain("https://evil.example/a.jpg")))
                .isEqualTo(ShareImageService.FALLBACK);
    }

    @Test
    void shareDescription_readsLikeAnInvitation() {
        WeddingDto d = new WeddingDto();
        d.setWeddingDate("2026-05-23");
        d.setWeddingTime("13:30");
        d.setWeddingPlace("그랜드웨딩홀");
        assertThat(d.getShareDescription()).isEqualTo("2026년 5월 23일 토요일 오후 1시 30분 · 그랜드웨딩홀");
        d.setWeddingTime("12:00");
        d.setWeddingPlace(null);
        assertThat(d.getShareDescription()).isEqualTo("2026년 5월 23일 토요일 오후 12시");
        assertThat(new WeddingDto().getShareDescription()).contains("초대합니다");
        assertThat(new WeddingDto().getShareTitle()).isEqualTo("모바일 청첩장");
    }
}
