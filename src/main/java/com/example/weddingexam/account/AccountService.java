package com.example.weddingexam.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> findAll() {
        return accountRepository.findAllByOrderBySortOrderAsc()
                .stream().map(AccountDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountDto> findBySide(String side) {
        return accountRepository.findBySideOrderBySortOrderAsc(side)
                .stream().map(AccountDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountDto> findByWeddingId(Long weddingId) {
        return accountRepository.findByWeddingIdOrderBySortOrderAsc(weddingId)
                .stream().map(AccountDto::from).collect(Collectors.toList());
    }

    @Transactional
    public AccountDto save(AccountDto dto) {
        if (dto.getOwner() == null || dto.getOwner().isBlank())
            throw new IllegalArgumentException("예금주를 입력해 주세요.");
        if (dto.getBank() == null || dto.getBank().isBlank())
            throw new IllegalArgumentException("은행을 입력해 주세요.");
        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank())
            throw new IllegalArgumentException("계좌번호를 입력해 주세요.");
        if (dto.getId() == null) {
            long count = accountRepository.count();
            dto.setSortOrder((int) count);
        }
        return AccountDto.from(accountRepository.save(dto.toEntity()));
    }

    @Transactional
    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    @Transactional
    public void saveAll(List<AccountDto> dtos) {
        accountRepository.deleteAll();
        for (int i = 0; i < dtos.size(); i++) {
            dtos.get(i).setId(null);
            dtos.get(i).setSortOrder(i);
            accountRepository.save(dtos.get(i).toEntity());
        }
    }

    @Transactional
    public void saveAllForWedding(Long weddingId, List<AccountDto> dtos) {
        accountRepository.deleteByWeddingId(weddingId);
        for (int i = 0; i < dtos.size(); i++) {
            dtos.get(i).setId(null);
            dtos.get(i).setWeddingId(weddingId);
            dtos.get(i).setSortOrder(i);
            accountRepository.save(dtos.get(i).toEntity());
        }
    }
}
