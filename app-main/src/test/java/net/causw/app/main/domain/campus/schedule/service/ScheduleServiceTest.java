package net.causw.app.main.domain.campus.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.causw.app.main.domain.campus.schedule.entity.Schedule;
import net.causw.app.main.domain.campus.schedule.entity.enums.ScheduleType;
import net.causw.app.main.domain.campus.schedule.service.dto.ScheduleDto;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleMaskingResolver;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleReader;
import net.causw.app.main.domain.campus.schedule.service.implementation.ScheduleWriter;
import net.causw.app.main.domain.campus.schedule.util.ScheduleMapper;
import net.causw.app.main.domain.user.account.entity.user.User;
import net.causw.app.main.shared.exception.BaseRunTimeV2Exception;
import net.causw.app.main.shared.exception.errorcode.ScheduleErrorCode;
import net.causw.app.main.util.ObjectFixtures;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

	@InjectMocks
	private ScheduleService scheduleService;

	@Mock
	private ScheduleWriter scheduleWriter;

	@Mock
	private ScheduleReader scheduleReader;

	@Mock
	private ScheduleMaskingResolver scheduleMaskingResolver;

	private User mockUser;
	private Schedule mockSchedule;
	private ScheduleDto mockScheduleDto;

	@BeforeEach
	void setUp() {
		mockUser = ObjectFixtures.getCertifiedUser();
		mockSchedule = ObjectFixtures.getSchedule(mockUser);
		mockScheduleDto = ObjectFixtures.getScheduleDto(mockUser);
	}

	@Nested
	@DisplayName("일정 생성 테스트")
	class CreateScheduleTest {

		@Test
		@DisplayName("일정 생성 성공")
		void createScheduleSuccess() {
			// given
			given(scheduleWriter.create(any(ScheduleDto.class))).willReturn(mockSchedule);

			// when
			ScheduleDto result = scheduleService.save(mockScheduleDto);

			// then
			assertThat(result).isNotNull();
			assertThat(result)
				.usingRecursiveComparison()
				.comparingOnlyFields("title", "start", "end", "type", "creator")
				.isEqualTo(mockScheduleDto);

			verify(scheduleWriter).create(any(ScheduleDto.class));
		}
	}

	@Nested
	@DisplayName("일정 수정 테스트")
	class UpdateScheduleTest {

		@Test
		@DisplayName("일정 수정 성공")
		void updateScheduleSuccess() {
			// given
			String scheduleId = "schedule-id";
			User updatedUser = ObjectFixtures.getCertifiedUserWithId("updated-user");
			ScheduleDto updateDto = ScheduleDto.builder()
				.title("수정된 일정")
				.type(ScheduleType.DEPARTMENT)
				.start(LocalDateTime.of(2026, 6, 15, 0, 0))
				.end(LocalDateTime.of(2026, 6, 21, 23, 59))
				.creator(updatedUser)
				.build();
			Schedule updatedSchedule = ScheduleMapper.from(updateDto);

			given(scheduleReader.findById(scheduleId)).willReturn(mockSchedule);
			given(scheduleWriter.update(mockSchedule, updateDto)).willReturn(updatedSchedule);

			// when
			ScheduleDto result = scheduleService.update(scheduleId, updateDto);

			// then
			assertThat(result).isNotNull();
			assertThat(result)
				.usingRecursiveComparison()
				.comparingOnlyFields("title", "start", "end", "type", "creator")
				.isEqualTo(updateDto);

			verify(scheduleReader).findById(scheduleId);
			verify(scheduleWriter).update(mockSchedule, updateDto);
		}

		@Test
		@DisplayName("존재하지 않는 일정 수정 시도")
		void updateNonExistentSchedule() {
			// given
			String scheduleId = "non-existent-id";
			ScheduleDto updateDto = ScheduleDto.builder()
				.title("수정할 일정")
				.type(ScheduleType.ACADEMIC)
				.start(LocalDateTime.of(2026, 6, 15, 0, 0))
				.end(LocalDateTime.of(2026, 6, 21, 23, 59))
				.build();

			given(scheduleReader.findById(scheduleId))
				.willThrow(ScheduleErrorCode.SCHEDULE_NOT_FOUND.toBaseException());

			// when & then
			assertThatThrownBy(() -> scheduleService.update(scheduleId, updateDto))
				.isInstanceOf(BaseRunTimeV2Exception.class);

			verify(scheduleReader).findById(scheduleId);
			verify(scheduleWriter, never()).update(any(Schedule.class), any(ScheduleDto.class));
		}
	}

	@Nested
	@DisplayName("일정 삭제 테스트")
	class DeleteScheduleTest {

		@Test
		@DisplayName("일정 삭제 성공")
		void deleteScheduleSuccess() {
			// given
			String scheduleId = "schedule-id";

			// when
			scheduleService.delete(scheduleId);

			// then
			verify(scheduleWriter).deleteById(scheduleId);
		}
	}

	@Nested
	@DisplayName("일정 조회 테스트")
	class FindScheduleTest {

		@Test
		@DisplayName("ID로 일정 조회 성공")
		void findByIdSuccess() {
			// given
			String scheduleId = "schedule-id";
			given(scheduleReader.findById(scheduleId)).willReturn(mockSchedule);

			// when
			ScheduleDto result = scheduleService.findById(scheduleId);

			// then
			assertThat(result).isNotNull();
			assertThat(result)
				.usingRecursiveComparison()
				.comparingOnlyFields("title", "start", "end", "type", "creator")
				.isEqualTo(mockSchedule);

			verify(scheduleReader).findById(scheduleId);
		}

		@Test
		@DisplayName("존재하지 않는 ID로 조회 시도")
		void findByIdNotFound() {
			// given
			String scheduleId = "non-existent-id";
			given(scheduleReader.findById(scheduleId))
				.willThrow(ScheduleErrorCode.SCHEDULE_NOT_FOUND.toBaseException());

			// when & then
			assertThatThrownBy(() -> scheduleService.findById(scheduleId))
				.isInstanceOf(BaseRunTimeV2Exception.class);

			verify(scheduleReader).findById(scheduleId);
		}

		@Test
		@DisplayName("조건에 따른 일정 목록 조회 성공")
		void findByConditionSuccess() {
			// given
			LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
			LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);
			List<ScheduleType> types = List.of(ScheduleType.ACADEMIC, ScheduleType.DEPARTMENT);

			Schedule schedule1 = Schedule.of(
				"중간고사",
				ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 21, 23, 59),
				mockUser,
				null);

			Schedule schedule2 = Schedule.of(
				"학술제",
				ScheduleType.DEPARTMENT,
				LocalDateTime.of(2026, 4, 25, 0, 0),
				LocalDateTime.of(2026, 4, 27, 23, 59),
				mockUser,
				null);

			List<Schedule> mockSchedules = List.of(schedule1, schedule2);
			given(scheduleReader.findByCondition(from, to, types)).willReturn(mockSchedules);

			// when
			List<ScheduleDto> result = scheduleService.findByCondition(from, to, types);

			// then
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result)
				.allSatisfy(dto -> {
					assertThat(dto.start()).isAfterOrEqualTo(from);
					assertThat(dto.end()).isBeforeOrEqualTo(to);
					assertThat(types).contains(dto.type());
				});

			verify(scheduleReader).findByCondition(from, to, types);
		}

		@Test
		@DisplayName("조건에 맞는 일정이 없을 때 빈 리스트 반환")
		void findByConditionEmpty() {
			// given
			LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
			LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59);
			List<ScheduleType> types = List.of(ScheduleType.ACADEMIC);

			given(scheduleReader.findByCondition(from, to, types)).willReturn(List.of());

			// when
			List<ScheduleDto> result = scheduleService.findByCondition(from, to, types);

			// then
			assertThat(result).isNotNull();
			assertThat(result).isEmpty();

			verify(scheduleReader).findByCondition(from, to, types);
		}

		@Test
		@DisplayName("타입 필터 없이 전체 일정 조회")
		void findByConditionWithoutTypeFilter() {
			// given
			LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
			LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

			Schedule schedule1 = Schedule.of(
				"중간고사",
				ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 21, 23, 59),
				mockUser,
				null);

			Schedule schedule2 = Schedule.of(
				"동아리 활동",
				ScheduleType.CCSSAA,
				LocalDateTime.of(2026, 4, 25, 0, 0),
				LocalDateTime.of(2026, 4, 27, 23, 59),
				mockUser,
				null);

			List<Schedule> mockSchedules = List.of(schedule1, schedule2);
			given(scheduleReader.findByCondition(from, to, null)).willReturn(mockSchedules);

			// when
			List<ScheduleDto> result = scheduleService.findByCondition(from, to, null);

			// then
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result)
				.allSatisfy(dto -> {
					assertThat(dto.start()).isAfterOrEqualTo(from);
					assertThat(dto.end()).isBeforeOrEqualTo(to);
				});

			verify(scheduleReader).findByCondition(from, to, null);
		}
	}

	@Nested
	@DisplayName("마스킹 조회 위임 테스트")
	class MaskingDelegationTest {

		@Test
		@DisplayName("findByConditionWithMasking은 ScheduleMaskingResolver에 권한 검증을 위임한다")
		void findByConditionWithMaskingDelegatesToResolver() {
			// given
			LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
			LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);
			List<ScheduleType> types = List.of(ScheduleType.ACADEMIC);
			User viewer = ObjectFixtures.getCertifiedUserWithId("viewer-id");

			Schedule schedule = Schedule.of(
				"일정", ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 21, 23, 59),
				mockUser, "post-1");
			given(scheduleReader.findByCondition(from, to, types)).willReturn(List.of(schedule));
			given(scheduleMaskingResolver.resolveReadablePostIds(anyList(), eq(viewer))).willReturn(Set.of("post-1"));
			given(scheduleMaskingResolver.maskIfUnreadable(any(ScheduleDto.class), eq(Set.of("post-1"))))
				.willAnswer(inv -> inv.getArgument(0));

			// when
			List<ScheduleDto> result = scheduleService.findByConditionWithMasking(from, to, types, viewer);

			// then
			assertThat(result).hasSize(1);
			verify(scheduleMaskingResolver).resolveReadablePostIds(anyList(), eq(viewer));
			verify(scheduleMaskingResolver).maskIfUnreadable(any(ScheduleDto.class), any());
		}

		@Test
		@DisplayName("findByIdWithMasking은 ScheduleMaskingResolver에 권한 검증을 위임한다")
		void findByIdWithMaskingDelegatesToResolver() {
			// given
			String scheduleId = "schedule-id";
			User viewer = ObjectFixtures.getCertifiedUserWithId("viewer-id");

			given(scheduleReader.findById(scheduleId)).willReturn(mockSchedule);
			given(scheduleMaskingResolver.resolveReadablePostIds(anyList(), eq(viewer))).willReturn(Set.of());
			given(scheduleMaskingResolver.maskIfUnreadable(any(ScheduleDto.class), any()))
				.willAnswer(inv -> inv.getArgument(0));

			// when
			scheduleService.findByIdWithMasking(scheduleId, viewer);

			// then
			verify(scheduleReader).findById(scheduleId);
			verify(scheduleMaskingResolver).resolveReadablePostIds(anyList(), eq(viewer));
			verify(scheduleMaskingResolver).maskIfUnreadable(any(ScheduleDto.class), any());
		}
	}

	@Nested
	@DisplayName("readableOnly 필터 테스트")
	class ReadableOnlyFilterTest {

		private LocalDateTime from;
		private LocalDateTime to;

		@BeforeEach
		void setUp() {
			from = LocalDateTime.of(2026, 4, 1, 0, 0);
			to = LocalDateTime.of(2026, 4, 30, 23, 59);
		}

		/**
		 * maskIfUnreadable mock: readablePostIds에 포함되면 원본 반환, 아니면 targetPostId=null로 마스킹.
		 */
		private void setupMaskingMock(Set<String> readablePostIds) {
			given(scheduleMaskingResolver.resolveReadablePostIds(anyList(), any())).willReturn(readablePostIds);
			given(scheduleMaskingResolver.maskIfUnreadable(any(ScheduleDto.class), any()))
				.willAnswer(inv -> {
					ScheduleDto dto = inv.getArgument(0);
					if (dto.targetPostId() == null || readablePostIds.contains(dto.targetPostId())) {
						return dto;
					}
					return ScheduleMapper.toWithoutTargetPost(dto);
				});
		}

		@Test
		@DisplayName("readableOnly=true + 익명 사용자: 연결 게시물이 없는 일정만 반환된다")
		void anonymousUser_keepsOnlySchedulesWithoutLinkedPost() {
			// given: 일정 1은 연결 게시물 있음, 일정 2는 없음
			Schedule withPost = Schedule.of(
				"공지", ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 10, 0, 0),
				LocalDateTime.of(2026, 4, 12, 23, 59),
				mockUser, "post-1");
			Schedule withoutPost = Schedule.of(
				"일반일정", ScheduleType.DEPARTMENT,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 17, 23, 59),
				mockUser, null);
			given(scheduleReader.findByCondition(from, to, null)).willReturn(List.of(withPost, withoutPost));

			// viewer=null → readablePostIds=빈 집합 → post-1은 마스킹됨
			setupMaskingMock(Set.of());

			// when
			List<ScheduleDto> result = scheduleService.findByConditionWithMasking(from, to, null, null, true);

			// then: 연결 게시물이 없던 일정만 남음
			assertThat(result).hasSize(1);
			assertThat(result.get(0).title()).isEqualTo("일반일정");
			assertThat(result.get(0).targetPostId()).isNull();
		}

		@Test
		@DisplayName("readableOnly=true + 부분 권한: 읽기 가능한 게시물 일정 + 연결 없는 일정만 반환된다")
		void partialPermissions_keepsReadableAndNoPost() {
			// given: 일정 1은 읽기 가능, 일정 2는 읽기 불가, 일정 3은 연결 없음
			User viewer = ObjectFixtures.getCertifiedUserWithId("viewer-id");
			Schedule readablePostSchedule = Schedule.of(
				"readable공지", ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 10, 0, 0),
				LocalDateTime.of(2026, 4, 12, 23, 59),
				mockUser, "post-readable");
			Schedule unreadablePostSchedule = Schedule.of(
				"제한공지", ScheduleType.STUDENT_COUNCIL,
				LocalDateTime.of(2026, 4, 13, 0, 0),
				LocalDateTime.of(2026, 4, 14, 23, 59),
				mockUser, "post-unreadable");
			Schedule noPostSchedule = Schedule.of(
				"일반일정", ScheduleType.DEPARTMENT,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 17, 23, 59),
				mockUser, null);
			given(scheduleReader.findByCondition(from, to, null))
				.willReturn(List.of(readablePostSchedule, unreadablePostSchedule, noPostSchedule));

			setupMaskingMock(Set.of("post-readable"));

			// when
			List<ScheduleDto> result = scheduleService.findByConditionWithMasking(from, to, null, viewer, true);

			// then: 읽기 가능한 일정 + 연결 없는 일정 = 2개
			assertThat(result).hasSize(2);
			assertThat(result).extracting(ScheduleDto::title)
				.containsExactly("readable공지", "일반일정");
			// 읽기 가능한 일정은 targetPostId가 유지됨
			assertThat(result.stream().filter(d -> "readable공지".equals(d.title())).findFirst().get().targetPostId())
				.isEqualTo("post-readable");
			// 연결 없는 일정은 targetPostId가 원래 null
			assertThat(result.stream().filter(d -> "일반일정".equals(d.title())).findFirst().get().targetPostId())
				.isNull();
		}

		@Test
		@DisplayName("readableOnly=false: 모든 일정이 반환되고 읽기 불가 게시물의 targetPostId만 마스킹된다")
		void readableOnlyFalse_keepsAllSchedulesWithMasking() {
			// given: 일정 1은 읽기 불가 게시물, 일정 2는 연결 없음
			User viewer = ObjectFixtures.getCertifiedUserWithId("viewer-id");
			Schedule unreadableSchedule = Schedule.of(
				"제한공지", ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 10, 0, 0),
				LocalDateTime.of(2026, 4, 12, 23, 59),
				mockUser, "post-unreadable");
			Schedule noPostSchedule = Schedule.of(
				"일반일정", ScheduleType.DEPARTMENT,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 17, 23, 59),
				mockUser, null);
			given(scheduleReader.findByCondition(from, to, null))
				.willReturn(List.of(unreadableSchedule, noPostSchedule));

			setupMaskingMock(Set.of());

			// when
			List<ScheduleDto> result = scheduleService.findByConditionWithMasking(from, to, null, viewer, false);

			// then: 모든 일정이 반환됨 (기존 동작과 동일)
			assertThat(result).hasSize(2);
			// 읽기 불가 게시물의 targetPostId는 마스킹됨
			assertThat(result.stream().filter(d -> "제한공지".equals(d.title())).findFirst().get().targetPostId())
				.isNull();
			// 연결 없는 일정은 그대로
			assertThat(result.stream().filter(d -> "일반일정".equals(d.title())).findFirst().get().targetPostId())
				.isNull();
		}

		@Test
		@DisplayName("4인자 메서드는 readableOnly=false와 동일한 동작을 한다 (기본값 호환)")
		void fourArgMethod_delegatesWithReadableOnlyFalse() {
			// given
			Schedule schedule1 = Schedule.of(
				"일정1", ScheduleType.ACADEMIC,
				LocalDateTime.of(2026, 4, 10, 0, 0),
				LocalDateTime.of(2026, 4, 12, 23, 59),
				mockUser, "post-1");
			Schedule schedule2 = Schedule.of(
				"일정2", ScheduleType.DEPARTMENT,
				LocalDateTime.of(2026, 4, 15, 0, 0),
				LocalDateTime.of(2026, 4, 17, 23, 59),
				mockUser, null);
			given(scheduleReader.findByCondition(from, to, null)).willReturn(List.of(schedule1, schedule2));
			setupMaskingMock(Set.of());

			// when: 4인자 메서드 호출 (readableOnly 파라미터 없음)
			List<ScheduleDto> result = scheduleService.findByConditionWithMasking(from, to, null, null);

			// then: readableOnly=false와 동일 — 모든 일정 반환
			assertThat(result).hasSize(2);
			verify(scheduleMaskingResolver).resolveReadablePostIds(anyList(), any());
		}
	}
}
