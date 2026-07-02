package com.example.weddingexam.account;

import jakarta.persistence.*;

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long weddingId;
    private String side;
    private String owner;
    private String bank;
    private String accountNumber;
    private String kakaoPayUrl;
    private int sortOrder;

    public AccountEntity() {}

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
}
