package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // spring data jpa가 메소드 이름으로 자동으로 JPQL을 만드는 전략이 있음
    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
