package net.causw.app.main.domain.user.academic.util;

import org.springframework.stereotype.Component;

import net.causw.app.main.domain.user.academic.entity.userAcademicRecord.UserAcademicRecordApplication;
import net.causw.app.main.domain.user.academic.enums.userAcademicRecord.AcademicRecordRequestStatus;
import net.causw.app.main.shared.exception.errorcode.AcademicRecordApplicationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AcademicRecordApplicationValidator {

	/**
	 * Service-level validation: throws ALREADY_PROCESSED for duplicate processing attempts.
	 * Used by AcademicRecordAdminService before delegating to the writer.
	 */
	public void validateAwaiting(UserAcademicRecordApplication application) {
		if (application.getAcademicRecordRequestStatus() != AcademicRecordRequestStatus.AWAIT) {
			throw AcademicRecordApplicationErrorCode.ACADEMIC_RECORD_APPLICATION_ALREADY_PROCESSED.toBaseException();
		}
	}

	/**
	 * Writer-level defensive validation (kept for backward compatibility).
	 * @deprecated Service layer should validate via {@link #validateAwaiting(UserAcademicRecordApplication)} before calling the writer.
	 */
	@Deprecated
	public static void validateAwaitStatus(UserAcademicRecordApplication application) {
		if (application.getAcademicRecordRequestStatus() != AcademicRecordRequestStatus.AWAIT) {
			throw AcademicRecordApplicationErrorCode.ACADEMIC_RECORD_APPLICATION_NOT_AWAITING.toBaseException();
		}
	}
}
