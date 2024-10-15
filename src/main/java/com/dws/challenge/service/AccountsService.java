package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.TransferRequest;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  @Autowired
  private NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  private final Lock lock = new ReentrantLock();  // Ensure thread-safety

  public void transfer(TransferRequest transferRequest) {
    lock.lock();  // Ensure only one transfer happens at a time
    try {
      Account accountFrom = accountsRepository.getAccount(transferRequest.getAccountFromId());
      Account accountTo = accountsRepository.getAccount(transferRequest.getAccountToId());

      BigDecimal amount = transferRequest.getAmount();

      // Ensure no negative balance
      if (amount.compareTo(accountFrom.getBalance())>0) {
        throw new InsufficientFundsException("Insufficient balance in account " + accountFrom.getAccountId());
      }

      // Perform the transfer
      accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
      accountTo.setBalance(accountTo.getBalance().add(amount));

      // Send notifications
      notificationService.notifyAboutTransfer(accountFrom, "Transferred " + amount + " to account " + accountTo.getAccountId());
      notificationService.notifyAboutTransfer(accountTo, "Received " + amount + " from account " + accountFrom.getAccountId());

    } finally {
      lock.unlock();  // Release the lock to avoid deadlock
    }
  }

}
