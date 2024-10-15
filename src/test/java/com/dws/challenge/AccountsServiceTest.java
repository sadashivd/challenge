package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void testTransfer() {

    // Set up your mock account data
    Account accountFrom = new Account("ID-1",new BigDecimal(1000.0));
    Account accountTo = new Account("ID-2",new BigDecimal(500.0));

    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId(accountFrom.getAccountId());
    transferRequest.setAccountToId(accountTo.getAccountId());
    transferRequest.setAmount(new BigDecimal(100.0));

    accountsService.transfer(transferRequest);

    // Verify balances
    assertEquals(900, accountFrom.getBalance().doubleValue());
    assertEquals(600, accountTo.getBalance().doubleValue());
  }

  @Test
  public void testTransfer_insufficeintFunds() {

    // Set up your mock account data
    Account accountFrom = new Account("ID-4",new BigDecimal(1000.0));
    Account accountTo = new Account("ID-3",new BigDecimal(500.0));

    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId(accountFrom.getAccountId());
    transferRequest.setAccountToId(accountTo.getAccountId());
    transferRequest.setAmount(new BigDecimal(1100.0));

    try {
      this.accountsService.transfer(transferRequest);
      fail("Should fail for sufficient balance");
    } catch (InsufficientFundsException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient balance in account "+accountFrom.getAccountId());
    }
  }

  @Test
  public void testConcurrentTransfers() throws Exception {
    Account accountFrom = new Account("ID-5",new BigDecimal(1000.0));
    Account accountTo = new Account("ID-7",new BigDecimal(1500.0));
    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);
    // Simulate multiple transfers happening concurrently

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAccountFromId(accountFrom.getAccountId());
    transferRequest.setAccountToId(accountTo.getAccountId());
    transferRequest.setAmount(new BigDecimal(600.0));

    TransferRequest transferRequest2 = new TransferRequest();
    transferRequest2.setAccountFromId(accountTo.getAccountId());
    transferRequest2.setAccountToId(accountFrom.getAccountId());
    transferRequest2.setAmount(new BigDecimal(700.0));

    CompletableFuture<Void> transfer1 = CompletableFuture.runAsync(() -> {
      accountsService.transfer(transferRequest);
    });

    CompletableFuture<Void> transfer2 = CompletableFuture.runAsync(() -> {
      accountsService.transfer(transferRequest2);
    });

    CompletableFuture<Void> transfer3 = CompletableFuture.runAsync(() -> {
      accountsService.transfer(transferRequest2);
    });

    CompletableFuture.allOf(transfer1, transfer2, transfer3).join();

    assertEquals(1800.0, accountFrom.getBalance().doubleValue());
    assertEquals(700.0, accountTo.getBalance().doubleValue());
  }

}
