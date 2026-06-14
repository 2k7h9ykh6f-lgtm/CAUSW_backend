package net.causw.app.main.domain.campus.schedule.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.causw.app.main.domain.campus.schedule.entity.Schedule;
import net.causw.app.main.domain.campus.schedule.entity.enums.ScheduleType;
import net.causw.app.main.domain.campus.schedule.service.dto.ScheduleDto;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleMaskingResolver;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleReader;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleWriter;
import net.causw.app.main.domain.campus.schedule.util.ScheduleMapper;
import net.causw.app.main.domain.user.account.entity.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {
	private final ScheduleWriter scheduleWriter;
	private final ScheduleReader scheduleReader;
	private final ScheduleMaskingResolver scheduleMaskingResolver;

	@Transactional
	public ScheduleDto save(ScheduleDto dto) {
		return ScheduleMapper.to(scheduleWriter.create(dto));
	}

	@Transactional
	public ScheduleDto update(String scheduleId, ScheduleDto dto) {
		Schedule schedule = scheduleReader.findById(scheduleId);
		return ScheduleMapper.to(scheduleWriter.update(schedule, dto));
	}

	@Transactional
	public void delete(String scheduleId) {
		scheduleWriter.deleteById(scheduleId);
	}

	@Transactional(readOnly = true)
	public List<ScheduleDto> findByCondition(LocalDateTime from, LocalDateTime to, Collection<ScheduleType> types) {
		return scheduleReader.findByCondition(from, to, types).stream().map(ScheduleMapper::to)
			.toList();
	}

	@Transactional(readOnly = true)
	public ScheduleDto findById(String scheduleId) {
		return ScheduleMapper.to(scheduleReader.findById(scheduleId));
	}

	/**
	 * 조건에 따라 일정을 조회하고, 연결된 게시물(targetPostId)에 대한 읽기 권한이 없는 경우 null로 마스킹합니다.
	 * 모든 일정을 반환하되 권한 없는 targetPostId만 마스킹하는 기존 기본 동작입니다.
	 *
	 * @param from   시작 일시
	 * @param to     종료 일시
	 * @param types  일정 유형 필터
	 * @param viewer 현재 요청 사용자 (null이면 모두 마스킹)
	 * @return targetPostId가 권한에 따라 마스킹된 일정 목록
	 */
	@Transactional(readOnly = true)
	public List<ScheduleDto> findByConditionWithMasking(LocalDateTime from, LocalDateTime to,
		Collection<ScheduleType> types, User viewer) {
		return findByConditionWithMasking(from, to, types, viewer, false);
	}

	/**
	 * 조건에 따라 일정을 조회하고, 연결된 게시물(targetPostId)에 대한 읽기 권한 정책을 적용합니다.
	 * <ul>
	 *     <li>{@code excludeUnreadablePosts == false} (기본): 모든 일정을 반환하되 권한 없는 targetPostId만 null로 마스킹합니다.</li>
	 *     <li>{@code excludeUnreadablePosts == true}: targetPostId가 없거나 viewer가 읽을 수 있는 일정만 반환하고,
	 *         targetPostId가 있으면서 권한이 없는 일정은 목록에서 제외합니다.</li>
	 * </ul>
	 *
	 * @param from                   시작 일시
	 * @param to                     종료 일시
	 * @param types                  일정 유형 필터
	 * @param viewer                 현재 요청 사용자 (null이면 모두 마스킹/제외)
	 * @param excludeUnreadablePosts true이면 권한 없는 연결 게시물을 가진 일정을 목록에서 제외
	 * @return 권한 정책이 적용된 일정 목록
	 */
	@Transactional(readOnly = true)
	public List<ScheduleDto> findByConditionWithMasking(LocalDateTime from, LocalDateTime to,
		Collection<ScheduleType> types, User viewer, boolean excludeUnreadablePosts) {
		List<ScheduleDto> scheduleDtos = scheduleReader.findByCondition(from, to, types).stream()
			.map(ScheduleMapper::to)
			.toList();
		Set<String> readablePostIds = scheduleMaskingResolver.resolveReadablePostIds(scheduleDtos, viewer);

		Stream<ScheduleDto> stream = scheduleDtos.stream();
		if (excludeUnreadablePosts) {
			stream = stream.filter(dto -> scheduleMaskingResolver.isReadableOrUnlinked(dto, readablePostIds));
		}
		return stream
			.map(dto -> scheduleMaskingResolver.maskIfUnreadable(dto, readablePostIds))
			.toList();
	}

	/**
	 * 특정 ID의 일정을 조회하고, 연결된 게시물(targetPostId)에 대한 읽기 권한이 없는 경우 null로 마스킹합니다.
	 *
	 * @param scheduleId 일정 ID
	 * @param viewer     현재 요청 사용자 (null이면 마스킹)
	 * @return targetPostId가 권한에 따라 마스킹된 일정
	 */
	@Transactional(readOnly = true)
	public ScheduleDto findByIdWithMasking(String scheduleId, User viewer) {
		ScheduleDto scheduleDto = ScheduleMapper.to(scheduleReader.findById(scheduleId));
		Set<String> readablePostIds = scheduleMaskingResolver.resolveReadablePostIds(List.of(scheduleDto), viewer);
		return scheduleMaskingResolver.maskIfUnreadable(scheduleDto, readablePostIds);
	}
}
