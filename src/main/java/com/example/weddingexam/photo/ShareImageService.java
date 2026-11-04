package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카카오톡 공유·링크 미리보기용 썸네일 — 공유용 사진(없으면 커플 메인 사진)을 정사각형(800×800)으로 잘라 만든다.
 *
 * 카카오는 1:1·4:3·2:1 이 아닌 이미지를 자동으로 잘라 버리는데, 청첩장 메인 사진은 대부분 세로라
 * 얼굴이 잘리기 쉽다. 그래서 서버에서 직접 1:1 로 자르고, 세로 사진은 얼굴이 있는 위쪽을 더 남긴다.
 * 만든 파일은 uploads/share/{원본 해시}.jpg — 같은 사진이면 다시 만들지 않는다.
 * (share/ 하위 폴더는 매일 도는 "안 쓰는 파일 정리" 대상이 아니다 — 원본에서 언제든 다시 만들 수 있다)
 */
@Service
public class ShareImageService {

    /** 사진이 없거나 못 읽을 때 쓰는 기본 썸네일 (800×400) */
    public static final String FALLBACK = "/images/share-thumbnail.jpg";
    static final int SIZE = 800;
    /** 카카오 공유 이미지 최소 크기 */
    static final int MIN_SOURCE = 200;
    /** 세로 사진에서 잘라낼 여유 중 위쪽에 둘 비율 — 0.5 면 가운데, 작을수록 위쪽(얼굴)을 남긴다 */
    static final double TOP_BIAS = 0.3;

    private final PhotoStorage storage;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ShareImageService(PhotoStorage storage) {
        this.storage = storage;
    }

    /** 공유 썸네일 경로 (사이트 기준 상대 경로). 만들 수 없으면 기본 썸네일 */
    public String shareImagePath(WeddingDto dto) {
        /* 공유용 사진을 따로 골랐으면 그것, 아니면 메인 사진 */
        String main = dto == null ? null
                : (dto.getSharePhoto() != null && !dto.getSharePhoto().isBlank()) ? dto.getSharePhoto()
                : dto.getMainPhotoBase64();
        if (main == null || main.isBlank()) return FALLBACK;
        String key = main.length() > 300 ? Integer.toHexString(main.hashCode()) + ":" + main.length() : main;
        String hit = cache.get(key);
        if (hit != null && (hit.equals(FALLBACK) || Files.exists(fileOf(hit)))) return hit;
        String made = make(main);
        cache.put(key, made);
        return made;
    }

    public boolean isSquare(String path) {
        return !FALLBACK.equals(path);
    }

    private String make(String main) {
        try {
            byte[] src = readSource(main.trim());
            if (src == null) return FALLBACK;
            String name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(src)) + ".jpg";
            Path out = storage.dir().resolve("share").resolve(name);
            String url = PhotoStorage.URL_PREFIX + "share/" + name;
            if (Files.exists(out)) return url;

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(src));
            if (img == null || Math.min(img.getWidth(), img.getHeight()) < MIN_SOURCE) return FALLBACK;
            byte[] jpg = toJpeg(squareCrop(img));

            Files.createDirectories(out.getParent());
            Path tmp = Files.createTempFile(out.getParent(), "sh-", ".tmp");
            Files.write(tmp, jpg);
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return url;
        } catch (Exception e) {
            return FALLBACK;   // 공유 썸네일 때문에 청첩장이 안 열리면 안 된다
        }
    }

    /** /uploads/..., /images/..., 예전 base64 — 셋 다 읽는다 */
    private byte[] readSource(String v) throws IOException {
        if (v.startsWith(PhotoStorage.URL_PREFIX)) {
            Path p = storage.dir().resolve(v.substring(PhotoStorage.URL_PREFIX.length())).normalize();
            if (!p.startsWith(storage.dir()) || !Files.isRegularFile(p)) return null;
            return Files.readAllBytes(p);
        }
        if (v.startsWith("/images/")) {
            ClassPathResource r = new ClassPathResource("static" + v);
            if (!r.exists()) return null;
            try (InputStream in = r.getInputStream()) { return in.readAllBytes(); }
        }
        if (PhotoExternalizer.isUrl(v)) return null;   // 외부 주소는 받아오지 않는다
        String payload = v.startsWith("data:") ? v.substring(v.indexOf(',') + 1) : v;
        try {
            return Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static BufferedImage squareCrop(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (int) Math.round((h - side) * (h > w ? TOP_BIAS : 0.5));
        // 항상 800 — og:image 크기·카카오 imageWidth/Height 로 800 을 알려 주므로 실제 크기도 맞춘다
        // (원본이 작으면 살짝 키운다. MIN_SOURCE 200px 미만은 위에서 이미 기본 썸네일로 돌렸다)
        int target = SIZE;
        BufferedImage out = new BufferedImage(target, target, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, target, target, x, y, x + side, y + side, java.awt.Color.WHITE, null);
        g.dispose();
        return out;
    }

    private static byte[] toJpeg(BufferedImage img) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.85f);
            writer.write(null, new IIOImage(img, null, null), p);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private Path fileOf(String url) {
        return storage.dir().resolve(url.substring(PhotoStorage.URL_PREFIX.length()));
    }
}
