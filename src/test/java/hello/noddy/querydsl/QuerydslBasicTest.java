package hello.noddy.querydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static hello.noddy.querydsl.entity.QMember.*;
import static hello.noddy.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.noddy.querydsl.entity.Member;
import hello.noddy.querydsl.entity.QMember;
import hello.noddy.querydsl.entity.QTeam;
import hello.noddy.querydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
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

  @Test
  void resultFetchTest() {
    List<Member> fetch = queryFactory
        .selectFrom(member)
        .fetch();

    Member fetchOne = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    Member fetchFirst = queryFactory
        .selectFrom(member)
        .fetchFirst();

    // 쿼리 2번 나간다 -> total 가져오기 위해서 count 쿼리
    QueryResults<Member> results = queryFactory
        .selectFrom(member)
        .fetchResults();

    long total = results.getTotal();
    long offset = results.getOffset();
    List<Member> content = results.getResults();

    long count = queryFactory
        .selectFrom(member)
        .fetchCount();
  }

  /**
   * 1. 회원 나이 내림차순(desc)
   * 2. 회원 이름 오름차순(asc)
   * 3. 2에서 회원 이름 없으면 마지막에 출력(nulls last)
   */
  @Test
  void sortTest() {
    entityManager.persist(new Member(null, 100));
    entityManager.persist(new Member("member5", 100));
    entityManager.persist(new Member("member6", 100));

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  // offset, limit 2가지로 페이징 지원
  @Test
  void pagingTest() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
    // 전체 조회수 필요하면, fetchResults
  }

  @Test
  void aggregationTest() {
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min()
        )
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println(tuple);
    }
  }

  // 팀의 이름과 각 팀의 평균 연령
  @Test
  void groupByTest() {
    List<Tuple> result = queryFactory
        .select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  /**
   * 팀 A에 소속된 모든 회원
   */
  @Test
  void joinTest() {
    List<Member> results = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();

    for (Member result : results) {
      System.out.println(result);
    }
  }

  // 연관관계 없이 조인 (theta_join)
  // 모든 멤버 테이블, 팀테이블 다 조인하고, 조건확인해서 가져오는 것
  // 멤버 이름과 팀 이름이 같은 회원 조회
  @Test
  void thetaJoinTest() {
    entityManager.persist(new Member("teamA"));
    entityManager.persist(new Member("teamB"));

    List<Member> results = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(results)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  // 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
  // jpql : select m, t from Member m left join m.team t on t.name = 'teamA'
  @Test
  void joinOnFiltering() {
    List<Tuple> results = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))
        .fetch();

    for (Tuple result : results) {
      System.out.println("result = " + result);
    }
  }

  /**
   * 연관관계 없는 엔티티 외부 조인
   * 회원의 이름이 팀 이름과 같은 대상 외부 조인
   */
  @Test
  void joinOnNoRelationTest() {
    entityManager.persist(new Member("teamA"));
    entityManager.persist(new Member("teamB"));

    List<Tuple> results = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team)
        .on(member.username.eq(team.name))
        .fetch();

    for (Tuple result : results) {
      System.out.println(result);
    }
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  void noFetchJoinTest() {
    entityManager.flush();
    entityManager.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();
    // 멤버의 team은 lazy로 설정해두었기 때문에, 이렇게만 조회하면 member만 조회되고, team은 조회 안된다.

    // emf에서 얻은 persistenceUnitUtil의 isLoaded메소드로 이미 로딩된 엔티티인지 아닌지 알려주는 기능
    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isFalse();
  }

  @Test
  void useFetchJoinTest() {
    entityManager.flush();
    entityManager.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isTrue();
  }

  /**
   * 1. 나이가 가장 많은 회원 조회
   * 2. 나이가 평균 이상인 회원
   */
  @Test
  void subQueryTest() {

    // 서브쿼리는 바깥에 있는 member와 alias가 겹치면 안된다.
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(40);

    List<Member> secondResult = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(secondResult).extracting("age")
        .containsExactly(30, 40);

    List<Member> thirdResult = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();

    assertThat(thirdResult).extracting("age")
        .containsExactly(20, 30, 40);

    // select 절 sub query
    List<Tuple> fetch = queryFactory
        .select(member.username,
            select(memberSub.age.avg())
                .from(memberSub))
        .from(member)
        .fetch();

    for (Tuple tuple : fetch) {
      System.out.println(tuple);
    }
  }

  @Test
  void basicCaseTest() {
    List<String> result = queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("스무살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  void complexCaseTest() {
    List<String> result = queryFactory
        .select(new CaseBuilder()
            .when(member.age.between(0, 20)).then("0~20살")
            .when(member.age.between(21, 30)).then("21~30살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  void constantTest() {
    List<Tuple> result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println(tuple);
    }

    // username_나이
    // .select(member.username.concat("_").concat(member.age)) 이건 타입이 달라서 안된다.
    List<String> concatResult = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();

    for (String s : concatResult) {
      System.out.println(s);
    }
  }
}
