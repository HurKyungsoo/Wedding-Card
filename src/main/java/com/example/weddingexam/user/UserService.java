package com.example.weddingexam.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<UserEntity> findAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findById(Long id) { return repo.findById(id); }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) { return repo.findByEmail(email); }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByKakaoId(String kakaoId) { return repo.findByKakaoId(kakaoId); }

    @Transactional
    public UserEntity save(UserEntity user) { return repo.save(user); }

    @Transactional(readOnly = true)
    public long countAll() { return repo.count(); }

    @Transactional(readOnly = true)
    public long countPro() { return repo.countByPlan("PRO"); }
}
