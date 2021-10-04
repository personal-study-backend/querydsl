package hello.noddy.querydsl.repository;

import hello.noddy.querydsl.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

  // select m from Member m where m.username = ?
  List<Member> findByUsername(String username);
}
