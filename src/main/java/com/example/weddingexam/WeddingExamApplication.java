package com.example.weddingexam;

import com.example.weddingexam.account.AccountDto;
import com.example.weddingexam.account.AccountService;
import com.example.weddingexam.service.WeddingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;

@SpringBootApplication
public class WeddingExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeddingExamApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(WeddingService weddingService, AccountService accountService) {
        return args -> {
            // 청첩장 샘플 데이터
            weddingService.save(weddingService.getDefaultDto());

            // 계좌 샘플 데이터
            AccountDto a1 = new AccountDto();
            a1.setSide("groom"); a1.setOwner("박지훈 (신랑)");
            a1.setBank("신한은행"); a1.setAccountNumber("110-123-456789");
            a1.setKakaoPayUrl(""); a1.setSortOrder(0);

            AccountDto a2 = new AccountDto();
            a2.setSide("groom"); a2.setOwner("박철수 (신랑 아버지)");
            a2.setBank("국민은행"); a2.setAccountNumber("123-45-678901");
            a2.setKakaoPayUrl(""); a2.setSortOrder(1);

            AccountDto a3 = new AccountDto();
            a3.setSide("bride"); a3.setOwner("이수아 (신부)");
            a3.setBank("카카오뱅크"); a3.setAccountNumber("3333-01-9876543");
            a3.setKakaoPayUrl(""); a3.setSortOrder(2);

            AccountDto a4 = new AccountDto();
            a4.setSide("bride"); a4.setOwner("이민수 (신부 아버지)");
            a4.setBank("하나은행"); a4.setAccountNumber("200-910-111222");
            a4.setKakaoPayUrl(""); a4.setSortOrder(3);

            accountService.saveAll(List.of(a1, a2, a3, a4));

            System.out.println("========================================");
            System.out.println(" 청첩장: http://localhost:8080/");
            System.out.println(" 편집:   http://localhost:8080/admin/edit");
            System.out.println(" H2:     http://localhost:8080/h2-console");
            System.out.println("========================================");
        };
    }
}
