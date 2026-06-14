package net.causw.app.main.domain.asset.locker.service.v2.dto.result;

import java.util.List;

import lombok.Builder;

/**
 * 만료된 사물함 일괄 회수 처리 결과 DTO.
 *
 * @param releasedCount  회수된 사물함 수
 * @param releasedLockers 회수된 사물함 상세 정보 목록
 */
@Builder
public record ExpiredLockerReleaseResult(
	int releasedCount,
	List<ReleasedLockerInfo> releasedLockers) {

	/**
	 * 회수된 단일 사물함 정보.
	 *
	 * @param lockerId     사물함 ID
	 * @param lockerNumber 사물함 번호
	 * @param locationName 사물함 위치 이름
	 */
	@Builder
	public record ReleasedLockerInfo(
		String lockerId,
		Long lockerNumber,
		String locationName) {
	}

	/**
	 * 만료된 사물함이 없을 때 빈 결과를 반환한다.
	 *
	 * @return 빈 결과 (회수 수 0, 목록 비어 있음)
	 */
	public static ExpiredLockerReleaseResult empty() {
		return new ExpiredLockerReleaseResult(0, List.of());
	}

	/**
	 * 회수된 사물함 정보 목록으로 결과를 생성한다.
	 *
	 * @param releasedLockers 회수된 사물함 정보 목록
	 * @return 처리 결과
	 */
	public static ExpiredLockerReleaseResult of(List<ReleasedLockerInfo> releasedLockers) {
		return new ExpiredLockerReleaseResult(releasedLockers.size(), releasedLockers);
	}
}
