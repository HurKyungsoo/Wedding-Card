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
}
