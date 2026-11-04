package com.example.weddingexam.photo;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.dto.WeddingEntity;
import com.example.weddingexam.service.WeddingRepository;
import com.example.weddingexam.service.WeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 저장 → 하객 화면 → 파일 제공 → 예전 데이터 이전 → 안 쓰는 파일 정리까지 한 줄로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:photo-file-flow;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret",
    "app.upload.dir=target/test-uploads/photo-file-flow"
})
class PhotoFileFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired WeddingService weddingService;
    @Autowired WeddingRepository repo;
    @Autowired PhotoMaintenance maintenance;
    @Autowired PhotoStorage storage;

    private Long weddingId() {
        WeddingDto dto = weddingService.findFirst();
        return dto.getId() != null ? dto.getId() : weddingService.save(dto).getId();
    }

    @Test
    void savedPhoto_isServedAsFile_andGuestHtmlHasNoBase64() throws Exception {
        Long id = weddingId();
        WeddingDto dto = weddingService.findById(id);
        dto.setMainPhotoBase64(PhotoExternalizerTest.JPEG_B64);
        weddingService.update(id, dto);

        String stored = weddingService.findById(id).getMainPhotoBase64();
        assertThat(stored).startsWith("/uploads/");

        String html = mockMvc.perform(get("/wedding/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("src=\"" + stored + "\"");
        assertThat(html).doesNotContain("base64," + PhotoExternalizerTest.JPEG_B64);

        mockMvc.perform(get(stored))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")));
    }

    @Test
    void draft_storesUrlsNotBase64() throws Exception {
        Long id = weddingId();
        WeddingDto dto = weddingService.findById(id);
        dto.setGalleryImages("data:image/jpeg;base64," + PhotoExternalizerTest.JPEG_B64);
        weddingService.saveDraft(id, dto);

        String draft = repo.findById(id).orElseThrow().getDraftData();
        assertThat(draft).doesNotContain(PhotoExternalizerTest.JPEG_B64).contains("/uploads/");
        assertThat(dto.getGalleryImages()).startsWith("/uploads/");   // 응답으로 편집기에 돌려줄 값
    }

    @Test
    void legacyBase64Rows_areMigratedOnStartupJob() throws Exception {
        Long id = weddingId();
        WeddingEntity e = repo.findById(id).orElseThrow();
        e.setEndingPhotos(PhotoExternalizerTest.JPEG_B64 + "|||" + PhotoExternalizerTest.PNG_B64);
        e.setDraftData("{\"mainPhotoBase64\":\"" + PhotoExternalizerTest.PNG_B64 + "\"}");
        repo.save(e);   // 예전 코드가 저장한 것처럼 — 서비스(변환)를 거치지 않고 바로

        assertThat(maintenance.migrateInlinePhotos()).isGreaterThanOrEqualTo(1);

        WeddingEntity after = repo.findById(id).orElseThrow();
        assertThat(after.getEndingPhotos()).doesNotContain(PhotoExternalizerTest.JPEG_B64).contains("/uploads/");
        assertThat(after.getDraftData()).doesNotContain(PhotoExternalizerTest.PNG_B64).contains("/uploads/");
        assertThat(maintenance.migrateInlinePhotos()).isZero();   // 다시 돌아도 할 일 없음
    }

    @Test
    void unreferencedOldFiles_areDeleted_butUsedAndFreshOnesStay() throws Exception {
        Long id = weddingId();
        WeddingDto dto = weddingService.findById(id);
        dto.setMainPhotoBase64(PhotoExternalizerTest.JPEG_B64);
        weddingService.update(id, dto);
        Path used = storage.dir().resolve(weddingService.findById(id).getMainPhotoBase64().substring("/uploads/".length()));

        Path orphanOld = storage.dir().resolve("a".repeat(64) + ".jpg");
        Path orphanNew = storage.dir().resolve("b".repeat(64) + ".jpg");
        Files.write(orphanOld, PhotoExternalizerTest.JPEG);
        Files.write(orphanNew, PhotoExternalizerTest.JPEG);
        FileTime old = FileTime.from(Instant.now().minus(Duration.ofDays(3)));
        Files.setLastModifiedTime(orphanOld, old);
        Files.setLastModifiedTime(used, old);

        maintenance.deleteUnreferencedFiles(Instant.now());

        assertThat(orphanOld).doesNotExist();
        assertThat(orphanNew).exists();   // 막 올린 파일은 아직 저장 전일 수 있다
        assertThat(used).exists();
    }
}
