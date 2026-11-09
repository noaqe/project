package kg.alatoo.library.service;

public final class StudentLendingPolicy implements LendingPolicy {
    @Override public int maxActiveLoans() { return 5; }
    @Override public int loanDays() { return 30; }
}
