package com.example.weddingexam.rsvp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RsvpService {

    private final RsvpRepository repo;

    public RsvpService(RsvpRepository repo) { this.repo = repo; }

    @Transactional
    public RsvpDto save(RsvpDto dto) {
        RsvpEntity entity = new RsvpEntity();
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setAttendance(dto.getAttendance());
        entity.setMealCount(dto.getMealCount() != null ? dto.getMealCount() : 1);
        entity.setMessage(dto.getMessage());
        return RsvpDto.from(repo.save(entity));
    }

    @Transactional(readOnly = true)
    public List<RsvpDto> findAll() {
        return repo.findAllByOrderByCreatedAtDesc()
                .stream().map(RsvpDto::from).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    /** 어드민 대시보드용 참석 응답 집계 */
    @Transactional(readOnly = true)
    public RsvpSummary summary() {
        List<RsvpEntity> all = repo.findAll();
        long attend  = all.stream().filter(r -> Boolean.TRUE.equals(r.getAttendance())).count();
        long decline = all.size() - attend;
        long meals   = all.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAttendance()))
                .mapToLong(r -> r.getMealCount() != null ? r.getMealCount() : 1)
                .sum();
        return new RsvpSummary(all.size(), attend, decline, meals);
    }

    /** 참석 집계 결과 */
    public record RsvpSummary(long total, long attend, long decline, long totalMeals) {}
}
