package com.example.weddingexam.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findAllByOrderBySortOrderAsc();
    List<AccountEntity> findBySideOrderBySortOrderAsc(String side);
    List<AccountEntity> findByWeddingIdOrderBySortOrderAsc(Long weddingId);
    List<AccountEntity> findByWeddingIdIsNullOrderBySortOrderAsc();
    void deleteByWeddingId(Long weddingId);
}
