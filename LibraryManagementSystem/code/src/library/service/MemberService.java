package library.service;

import library.exception.DuplicateMemberException;
import library.exception.MemberNotFoundException;
import library.model.Member;
import library.model.MemberStatus;
import library.model.MemberTier;
import library.repository.MemberRepository;

import java.util.List;

/**
 * Single authority for member lifecycle.
 * All methods identify members by email (== member.getId()).
 */
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /** Register a new member. Rejects duplicate email. */
    public synchronized Member register(Member member) {
        memberRepository.findById(member.getEmail()).ifPresent(existing -> {
            throw new DuplicateMemberException(member.getEmail());
        });
        return memberRepository.save(member);
    }

    public Member getMember(String email) {
        return memberRepository.findById(email)
                .orElseThrow(() -> new MemberNotFoundException(email));
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /** Suspend — member can no longer borrow. */
    public void suspend(String email) {
        Member member = getMember(email);
        member.setStatus(MemberStatus.SUSPENDED);
        memberRepository.save(member);
    }

    /** Reactivate a suspended member. Blacklisted members cannot be reactivated. */
    public void reactivate(String email) {
        Member member = getMember(email);
        if (member.getStatus() == MemberStatus.BLACKLISTED) {
            throw new IllegalStateException("Blacklisted member cannot be reactivated: " + email);
        }
        member.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);
    }

    /** Blacklist is permanent — no reactivation path. */
    public void blacklist(String email) {
        Member member = getMember(email);
        member.setStatus(MemberStatus.BLACKLISTED);
        memberRepository.save(member);
    }

    /** Upgrade or change tier, e.g. REGULAR → PREMIUM or → LIBRARIAN. */
    public void upgradeTier(String email, MemberTier tier) {
        Member member = getMember(email);
        member.setTier(tier);
        memberRepository.save(member);
    }

    public void deregister(String email) {
        getMember(email);   // throws MemberNotFoundException if absent
        memberRepository.delete(email);
    }
}
