package hello.noddy.querydsl;

import static hello.noddy.querydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.*;

import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.noddy.querydsl.entity.Member;
import hello.noddy.querydsl.entity.QMember;
import hello.noddy.querydsl.entity.Team;
import javax.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager entityManager;

  private JPAQueryFactory queryFactory;

  @BeforeEach
  void before() {
    queryFactory = new JPAQueryFactory(entityManager);
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
  }

  @Test
  void startJpql() {
    // member1 을 찾자.
    String qlString = ""
        + "select m from Member m "
        + "where m.username = :username";
    Member findMember = entityManager
        .createQuery(qlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQueryDsl() {
    // QMember m = new QMember("m"); // 별칭 직접 지정
    // QMember m = QMember.member; // 기본 인스턴스 생성
    // but static import 사용하자

    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void searchTest() {
    // username이 member1이고 age가 10인 member 가져와라
    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");

    // where 파라미터로 검색 조건을 추가할 수 있다. (and 조건으로)
    // 아래와 같은 방식으로 작성하면 null일 경우 무시되서 동적 쿼리 작성 시 유리해짐
    Member findMember2 = queryFactory
        .selectFrom(member)
        .where(
            member.username.eq("member1"),
            member.age.eq(10)
        )
        .fetchOne();

    assertThat(findMember2.getUsername()).isEqualTo("member1");
  }
}
