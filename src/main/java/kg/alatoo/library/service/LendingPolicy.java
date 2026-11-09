package kg.alatoo.library.service;

/** Strategy: LoanService uses the interface, not a switch with lending rules. */
public interface LendingPolicy {
    int maxActiveLoans();
    int loanDays();
}
