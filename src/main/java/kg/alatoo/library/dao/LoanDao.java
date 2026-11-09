package kg.alatoo.library.dao;

import kg.alatoo.library.model.Loan;
import kg.alatoo.library.model.MembershipType;
import java.time.LocalDate;
import java.util.List;

public interface LoanDao {
    long issue(long bookId, long readerId, MembershipType expectedType, int limit, int days, LocalDate today);
    void returnBook(long loanId, LocalDate today);
    List<Loan> findAll(boolean activeOnly);
}
