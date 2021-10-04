package hello.noddy.querydsl.repository;

import hello.noddy.querydsl.dto.MemberSearchCondition;
import hello.noddy.querydsl.dto.MemberTeamDto;
import java.util.List;

public interface MemberRepositoryCustom {

  List<MemberTeamDto> search(MemberSearchCondition condition);
}
