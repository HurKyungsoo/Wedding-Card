package com.example.weddingexam.guestbook;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GuestbookService {

    /** 한 청첩장에 쌓일 수 있는 최대 글 수 — 무제한이면 목록 응답이 계속 커진다 */
    public static final int MAX_ENTRIES_PER_WEDDING = 500;
    private static final int MAX_NAME_LEN = 20;
    private static final int MAX_MESSAGE_LEN = 500;

    private final GuestbookRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public GuestbookService(GuestbookRepository repo) { this.repo = repo; }

    @Transactional
    public GuestbookDto save(GuestbookDto dto) {
        String name = trimToNull(dto.getName());
        String message = trimToNull(dto.getMessage());
        String password = trimToNull(dto.getPassword());

        if (name == null)    throw new IllegalArgumentException("이름을 입력해 주세요.");
        if (message == null) throw new IllegalArgumentException("축하 메시지를 입력해 주세요.");
        if (password == null || !password.matches("[0-9]{4}"))
            throw new IllegalArgumentException("비밀번호는 숫자 4자리로 입력해 주세요.");
        if (name.length() > MAX_NAME_LEN)
            throw new IllegalArgumentException("이름은 " + MAX_NAME_LEN + "자까지 입력할 수 있습니다.");
        if (message.length() > MAX_MESSAGE_LEN)
            throw new IllegalArgumentException("메시지는 " + MAX_MESSAGE_LEN + "자까지 입력할 수 있습니다.");
        if (repo.countByWeddingId(dto.getWeddingId()) >= MAX_ENTRIES_PER_WEDDING)
            throw new IllegalArgumentException("방명록이 가득 찼습니다.");

        GuestbookEntity e = new GuestbookEntity();
        e.setWeddingId(dto.getWeddingId());
        e.setName(name);
        e.setMessage(message);
        e.setPasswordHash(encoder.encode(password));
        return GuestbookDto.from(repo.save(e));
    }

    @Transactional(readOnly = true)
    public List<GuestbookDto> findByWeddingId(Long weddingId) {
        return repo.findByWeddingIdOrderByCreatedAtDesc(weddingId)
                .stream().map(GuestbookDto::from).collect(Collectors.toList());
    }

    /**
     * 작성자 본인 삭제 — PIN이 맞을 때만.
     * @return 삭제됐으면 true, PIN이 틀리거나 대상이 없으면 false
     */
    @Transactional
    public boolean deleteWithPassword(Long id, Long weddingId, String password) {
        if (password == null) return false;
        return repo.findById(id)
                .filter(e -> e.getWeddingId() != null && e.getWeddingId().equals(weddingId))
                .filter(e -> e.getPasswordHash() != null && encoder.matches(password, e.getPasswordHash()))
                .map(e -> { repo.delete(e); return true; })
                .orElse(false);
    }

    /** 청첩장 주인의 삭제 — PIN 없이. 소유 확인은 컨트롤러가 한다 */
    @Transactional
    public void deleteForWedding(Long id, Long weddingId) {
        repo.findById(id).ifPresent(e -> {
            if (weddingId.equals(e.getWeddingId())) repo.delete(e);
        });
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
