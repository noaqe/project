package kg.alatoo.library.service;

public final class GuestLendingPolicy implements LendingPolicy {
    @Override public int maxActiveLoans() { return 2; }
    @Override public int loanDays() { return 14; }
}
