package net.causw.app.main.domain.asset.locker.service.v2.dto.result;

import java.util.List;

import lombok.Builder;

/**
 * 만료 사물함 일괄 회수 처리 결과 DTO.
 *
 * @param releasedCount 회수된 사물함 수
 * @param released      회수된 사물함 목록
 */
@Builder
public record LockerExpiredReleaseResult(
	int releasedCount,
	List<ReleasedLockerResult> released) {

	/**
	 * 회수 대상이 없는 빈 결과를 반환한다.
	 *
	 * @return 회수 수 0, 빈 목록 결과
	 */
	public static LockerExpiredReleaseResult empty() {
		return new LockerExpiredReleaseResult(0, List.of());
	}

	/**
	 * 회수된 사물함 목록으로 결과를 생성한다. 회수 수는 목록 크기로 설정된다.
	 *
	 * @param released 회수된 사물함 목록
	 * @return 회수 결과
	 */
	public static LockerExpiredReleaseResult of(List<ReleasedLockerResult> released) {
		return new LockerExpiredReleaseResult(released.size(), List.copyOf(released));
	}

	/**
	 * 회수된 개별 사물함 정보를 담는 DTO.
	 *
	 * @param lockerId     사물함 ID
	 * @param lockerNumber 사물함 번호
	 * @param location     사물함 위치 설명
	 */
	@Builder
	public record ReleasedLockerResult(
		String lockerId,
		Long lockerNumber,
		String location) {

		/**
		 * 회수된 사물함 정보를 생성한다.
		 *
		 * @param lockerId     사물함 ID
		 * @param lockerNumber 사물함 번호
		 * @param location     사물함 위치 설명
		 * @return 회수된 사물함 정보
		 */
		public static ReleasedLockerResult of(String lockerId, Long lockerNumber, String location) {
			return new ReleasedLockerResult(lockerId, lockerNumber, location);
		}
	}
}
