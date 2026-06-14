package net.causw.app.main.domain.asset.locker.api.v2.controller.admin.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "만료 사물함 일괄 회수 결과 응답")
public record LockerExpiredReleaseResponse(

	@Schema(description = "회수된 사물함 수", example = "3") int releasedCount,

	@Schema(description = "회수된 사물함 목록") List<ReleasedLockerItem> released) {

	@Schema(description = "회수된 사물함 항목")
	public record ReleasedLockerItem(

		@Schema(description = "사물함 ID", example = "locker-uuid-1234") String lockerId,

		@Schema(description = "사물함 번호", example = "1") Long lockerNumber,

		@Schema(description = "위치 설명", example = "2층") String location) {
	}
}
