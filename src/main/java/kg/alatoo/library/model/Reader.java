package kg.alatoo.library.model;

import java.util.Objects;

public final class Reader extends Person {
    private final MembershipType membershipType;

    public Reader(long id, String fullName, String email, MembershipType membershipType) {
        super(id, fullName, email);
        this.membershipType = Objects.requireNonNull(membershipType, "Тип читателя обязателен.");
    }

    public MembershipType getMembershipType() { return membershipType; }

    @Override public String getRole() { return "Читатель (" + membershipType + ")"; }
}
