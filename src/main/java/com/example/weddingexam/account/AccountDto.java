package com.example.weddingexam.account;

public class AccountDto {

    private Long id;
    private Long weddingId;
    private String side;
    private String owner;
    private String bank;
    private String accountNumber;
    private String kakaoPayUrl;
    private int sortOrder;

    public AccountDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWeddingId() { return weddingId; }
    public void setWeddingId(Long weddingId) { this.weddingId = weddingId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getKakaoPayUrl() { return kakaoPayUrl; }
    public void setKakaoPayUrl(String kakaoPayUrl) { this.kakaoPayUrl = kakaoPayUrl; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public static AccountDto from(AccountEntity e) {
        AccountDto dto = new AccountDto();
        dto.setId(e.getId());
        dto.setWeddingId(e.getWeddingId());
        dto.setSide(e.getSide());
        dto.setOwner(e.getOwner());
        dto.setBank(e.getBank());
        dto.setAccountNumber(e.getAccountNumber());
        dto.setKakaoPayUrl(e.getKakaoPayUrl());
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }

    public AccountEntity toEntity() {
        AccountEntity e = new AccountEntity();
        e.setId(this.id);
        e.setWeddingId(this.weddingId);
        e.setSide(this.side);
        e.setOwner(this.owner);
        e.setBank(this.bank);
        e.setAccountNumber(this.accountNumber);
        e.setKakaoPayUrl(this.kakaoPayUrl);
        e.setSortOrder(this.sortOrder);
        return e;
    }
}
