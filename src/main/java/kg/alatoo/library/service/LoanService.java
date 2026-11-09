package kg.alatoo.library.service;

import kg.alatoo.library.dao.LoanDao;
import kg.alatoo.library.dao.ReaderDao;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.MembershipType;
import kg.alatoo.library.model.Reader;
import kg.alatoo.library.util.Validation;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

/** Lending rules belong here; transactional checks and SQL remain in the DAO layer. */
public final class LoanService {
    private final ReaderDao readers;
    private final LoanDao loans;
    private final Map<MembershipType, LendingPolicy> policies;
    private final Clock clock;

    public LoanService(ReaderDao readers, LoanDao loans, Map<MembershipType, LendingPolicy> policies, Clock clock) {
        this.readers = readers;
        this.loans = loans;
        this.policies = Map.copyOf(policies);
        this.clock = clock;
    }

    public long issue(long bookId, long readerId) {
        Validation.positiveId(bookId); Validation.positiveId(readerId);
        Reader reader = readers.findById(readerId).orElseThrow(() -> new LibraryException("Читатель не найден."));
        LendingPolicy policy = policies.get(reader.getMembershipType());
        if (policy == null) throw new LibraryException("Не настроены правила для этого типа читателя.");
        return loans.issue(bookId, readerId, reader.getMembershipType(), policy.maxActiveLoans(), policy.loanDays(), LocalDate.now(clock));
    }

    public void returnBook(long loanId) {
        Validation.positiveId(loanId);
        loans.returnBook(loanId, LocalDate.now(clock));
    }
}
