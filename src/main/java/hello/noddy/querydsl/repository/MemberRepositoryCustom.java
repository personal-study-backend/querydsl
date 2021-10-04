package hello.noddy.querydsl.repository;

import hello.noddy.querydsl.dto.MemberSearchCondition;
import hello.noddy.querydsl.dto.MemberTeamDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {

  List<MemberTeamDto> search(MemberSearchCondition condition);

  Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

  Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
