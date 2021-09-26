package hello.noddy.querydsl;

import static hello.noddy.querydsl.entity.QMember.*;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.noddy.querydsl.dto.MemberDto;
import hello.noddy.querydsl.dto.UserDto;
import hello.noddy.querydsl.entity.Member;
import hello.noddy.querydsl.entity.QMember;
import hello.noddy.querydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslTest {

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
  void simpleProjection() {
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println(s);
    }
  }

  /**
   * tuple은 querydsl에서 지원해주는 타입. 즉, repository 계층을 넘어서서 서비스 계층 등에 가면 좋지 않다. querydsl에 의존적이게 된다.
   */
  @Test
  void tupleProjection() {
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);

      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  @Test
  void findDtoByJpql() {
    // jpql new operation을 통해 조회하는 방법
    List<MemberDto> result = entityManager.createQuery(
        "select new hello.noddy.querydsl.dto.MemberDto(m.username, m.age) from Member m",
        MemberDto.class)
        .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByQuerydslSetter() {
    List<MemberDto> result = queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByQuerydslField() {
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByConstructor() {
    List<MemberDto> result = queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findUserDtoByField() {
    // 별칭이 다를때
    // member는 username이지만, 받는 userDto는 name이기 때문에

    List<UserDto> result = queryFactory
        .select(Projections.fields(UserDto.class,
            member.username.as("name"),
            member.age))
        .from(member)
        .fetch();
    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }
}
