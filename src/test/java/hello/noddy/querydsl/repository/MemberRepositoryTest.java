package hello.noddy.querydsl.repository;


import static org.assertj.core.api.Assertions.assertThat;

import hello.noddy.querydsl.dto.MemberSearchCondition;
import hello.noddy.querydsl.dto.MemberTeamDto;
import hello.noddy.querydsl.entity.Member;
import hello.noddy.querydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberRepositoryTest {


  @Autowired
  EntityManager entityManager;

  @Autowired
  MemberRepository memberRepository;

  @Test
  void basicTest() {
    Member member = new Member("member1", 10);
    memberRepository.save(member);

    Member findMember = memberRepository.findById(member.getId())
        .orElseThrow(RuntimeException::new);

    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberRepository.findAll();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberRepository.findByUsername("member1");
    assertThat(result2).containsExactly(member);
  }

  @Test
  void searchTest() {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    entityManager.persist(teamA);
    entityManager.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    entityManager.persist(member1);
    entityManager.persist(member2);
    entityManager.persist(member3);
    entityManager.persist(member4);

    MemberSearchCondition condition = new MemberSearchCondition();
    condition.setAgeGoe(35);
    condition.setAgeLoe(40);
    condition.setTeamName("teamB");

    List<MemberTeamDto> result2 = memberRepository.search(condition);

    assertThat(result2).extracting("username").containsExactly("member4");
  }

  @Test
  void searchPageTest() {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    entityManager.persist(teamA);
    entityManager.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    entityManager.persist(member1);
    entityManager.persist(member2);
    entityManager.persist(member3);
    entityManager.persist(member4);

    MemberSearchCondition condition = new MemberSearchCondition();
    PageRequest pageRequest = PageRequest.of(0, 3);

    Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

    assertThat(result.getSize()).isEqualTo(3);
    assertThat(result.getContent()).extracting("username")
        .containsExactly("member1", "member2", "member3");
  }
}