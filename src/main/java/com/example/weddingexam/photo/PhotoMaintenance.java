package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.dto.WeddingEntity;
import com.example.weddingexam.service.WeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 사진 파일 관리
 *  1) 서버가 뜰 때 — 예전 방식(base64)으로 DB 에 남아 있는 사진을 파일로 옮긴다 (게시본 + 임시저장본).
 *     이미 옮긴 건 건너뛰므로 매번 돌아도 안전하다.
 *  2) 하루 한 번 — 어떤 청첩장(게시본·임시저장본)에서도 안 쓰는 파일을 지운다.
 *     편집 중 크롭을 바꿀 때마다 새 파일이 생기기 때문. 막 올린 파일은 아직 저장 전일 수 있어 하루는 남겨 둔다.
 */
@Component
public class PhotoMaintenance implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoMaintenance.class);
    private static final Pattern FILE_REF = Pattern.compile("/uploads/([0-9a-f]{64}\\.(?:jpg|png|webp|gif))");
    static final Duration KEEP_NEW_FILES = Duration.ofDays(1);

    private final WeddingRepository repo;
    private final PhotoExternalizer externalizer;
    private final PhotoStorage storage;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    public PhotoMaintenance(WeddingRepository repo, PhotoExternalizer externalizer, PhotoStorage storage,
                            ObjectMapper objectMapper, TransactionTemplate tx) {
        this.repo = repo;
        this.externalizer = externalizer;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int moved = migrateInlinePhotos();
            if (moved > 0) log.info("base64 사진을 파일로 옮긴 청첩장: {}개", moved);
        } catch (Exception e) {
            // 옮기다 실패해도 서비스는 떠야 한다 — 예전 값은 그대로 남아 있어 화면은 계속 보인다
            log.warn("사진 파일 이전 실패 — 다음 재시작 때 다시 시도합니다", e);
        }
    }

    /** 예전 base64 사진을 파일로 옮긴다. 바뀐 청첩장 수를 돌려준다 */
    public int migrateInlinePhotos() {
        List<Long> ids = repo.findAll().stream().map(WeddingEntity::getId).toList();
        int changed = 0;
        for (Long id : ids) {
            Boolean c = tx.execute(s -> migrateOne(id));
            if (Boolean.TRUE.equals(c)) changed++;
        }
        return changed;
    }

    private boolean migrateOne(Long id) {
        WeddingEntity e = repo.findById(id).orElse(null);
        if (e == null) return false;
        boolean changed = false;

        WeddingDto published = new WeddingDto();
        published.setMainPhotoBase64(e.getMainPhotoBase64());
        published.setEndingPhotoBase64(e.getEndingPhotoBase64());
        published.setGalleryImages(e.getGalleryImages());
        published.setEndingPhotos(e.getEndingPhotos());
        published.setSharePhoto(e.getSharePhoto());
        if (externalizer.externalize(published)) {
            e.setMainPhotoBase64(published.getMainPhotoBase64());
            e.setEndingPhotoBase64(published.getEndingPhotoBase64());
            e.setGalleryImages(published.getGalleryImages());
            e.setEndingPhotos(published.getEndingPhotos());
            e.setSharePhoto(published.getSharePhoto());
            changed = true;
        }

        if (e.getDraftData() != null && !e.getDraftData().isBlank()) {
            try {
                WeddingDto draft = objectMapper.readValue(e.getDraftData(), WeddingDto.class);
                if (externalizer.externalize(draft)) {
                    e.setDraftData(objectMapper.writeValueAsString(draft));
                    changed = true;
                }
            } catch (IOException ex) {
                log.warn("임시저장본을 읽지 못해 사진 이전을 건너뜀 id={}", id);
            }
        }
        if (changed) repo.save(e);
        return changed;
    }

    /** 매일 새벽 4시 — 안 쓰는 사진 파일 정리 */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupScheduled() {
        try {
            int n = deleteUnreferencedFiles(Instant.now());
            if (n > 0) log.info("안 쓰는 사진 파일 {}개 삭제", n);
        } catch (Exception e) {
            log.warn("사진 파일 정리 실패", e);
        }
    }

    /** 어디에서도 참조하지 않고, KEEP_NEW_FILES 보다 오래된 파일을 지운다. 지운 수를 돌려준다 */
    public int deleteUnreferencedFiles(Instant now) throws IOException {
        Path dir = storage.dir();
        if (!Files.isDirectory(dir)) return 0;
        Set<String> used = tx.execute(s -> referencedFileNames());
        if (used == null) return 0;
        int deleted = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String name = p.getFileName().toString();
                if (used.contains(name) || !Files.isRegularFile(p)) continue;
                Instant modified = Files.getLastModifiedTime(p).toInstant();
                if (modified.plus(KEEP_NEW_FILES).isAfter(now)) continue;
                Files.deleteIfExists(p);
                deleted++;
            }
        }
        return deleted;
    }

    private Set<String> referencedFileNames() {
        Set<String> used = new HashSet<>();
        for (WeddingEntity e : repo.findAll()) {
            for (String text : new String[] { e.getMainPhotoBase64(), e.getEndingPhotoBase64(),
                    e.getGalleryImages(), e.getEndingPhotos(), e.getSharePhoto(), e.getDraftData() }) {
                if (text == null) continue;
                Matcher m = FILE_REF.matcher(text);
                while (m.find()) used.add(m.group(1));
            }
        }
        return used;
    }
}
