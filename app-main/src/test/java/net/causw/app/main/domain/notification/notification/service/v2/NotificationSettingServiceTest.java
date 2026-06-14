package net.causw.app.main.domain.notification.notification.service.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.causw.app.main.domain.community.board.entity.Board;
import net.causw.app.main.domain.community.board.service.implementation.BoardReader;
import net.causw.app.main.domain.notification.notification.enums.UserNotificationSettingKey;
import net.causw.app.main.domain.notification.notification.service.NotificationSettingService;
import net.causw.app.main.domain.notification.notification.service.dto.NotificationSettingResult;
import net.causw.app.main.domain.notification.notification.service.dto.UpdateNotificationSettingCommand;
import net.causw.app.main.domain.notification.notification.service.dto.UserNotificationSettingMap;
import net.causw.app.main.domain.notification.notification.service.implementation.NotificationSettingReader;
import net.causw.app.main.domain.notification.notification.service.implementation.NotificationSettingWriter;
import net.causw.app.main.domain.notification.notification.service.implementation.UserBoardSubscribeReader;
import net.causw.app.main.domain.user.academic.enums.userAcademicRecord.AcademicStatus;
import net.causw.app.main.domain.user.account.entity.user.User;
import net.causw.app.main.domain.user.account.service.implementation.UserReader;
import net.causw.app.main.domain.user.account.service.implementation.UserValidator;
import net.causw.app.main.shared.exception.BaseRunTimeV2Exception;
import net.causw.app.main.shared.exception.errorcode.AuthErrorCode;
import net.causw.app.main.shared.exception.errorcode.BoardConfigErrorCode;
import net.causw.app.main.shared.exception.errorcode.UserErrorCode;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

	@InjectMocks
	private NotificationSettingService notificationSettingService;

	@Mock
	private NotificationSettingReader notificationSettingReader;
	@Mock
	private NotificationSettingWriter notificationSettingWriter;
	@Mock
	private BoardReader boardReader;
	@Mock
	private UserBoardSubscribeReader userBoardSubscribeReader;
	@Mock
	private UserReader userReader;
	@Mock
	private UserValidator userValidator;

	private UserNotificationSettingMap emptySettingMap() {
		Map<UserNotificationSettingKey, Boolean> empty = new EnumMap<>(UserNotificationSettingKey.class);
		return UserNotificationSettingMap.ofFull(empty);
	}

	@Nested
	@DisplayName("전체 알림 설정 조회 (getAllSettings)")
	class GetAllSettingsTest {

		private final String userId = "user-001";

		@Test
		@DisplayName("성공: 접근 가능한 게시판이 없으면 officialBoards가 빈 리스트로 반환된다")
		void givenNoBoardsAccessible_whenGetAllSettings_thenReturnEmptyOfficialBoards() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			given(notificationSettingReader.findSettingMap(userId)).willReturn(emptySettingMap());
			given(mockUser.getAcademicStatus()).willReturn(AcademicStatus.ENROLLED);
			given(boardReader.findAccessibleNoticeBoards(AcademicStatus.ENROLLED)).willReturn(List.of());

			// when
			NotificationSettingResult result = notificationSettingService.getAllSettings(userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.officialBoards()).isEmpty();
		}

		@Test
		@DisplayName("성공: 게시판이 있으면 구독 여부가 포함된 officialBoards가 반환된다")
		void givenAccessibleBoards_whenGetAllSettings_thenReturnOfficialBoardsWithSubscribeStatus() {
			// given
			User mockUser = mock(User.class);
			Board mockBoard = mock(Board.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			given(notificationSettingReader.findSettingMap(userId)).willReturn(emptySettingMap());
			given(mockUser.getAcademicStatus()).willReturn(AcademicStatus.ENROLLED);
			given(mockBoard.getId()).willReturn("board-001");
			given(mockBoard.getName()).willReturn("공지사항");
			given(boardReader.findAccessibleNoticeBoards(AcademicStatus.ENROLLED)).willReturn(List.of(mockBoard));
			given(userBoardSubscribeReader.findSubscribedBoardIds(mockUser, List.of(mockBoard)))
				.willReturn(Set.of("board-001"));

			// when
			NotificationSettingResult result = notificationSettingService.getAllSettings(userId);

			// then
			assertThat(result.officialBoards()).hasSize(1);
			assertThat(result.officialBoards().get(0).boardId()).isEqualTo("board-001");
			assertThat(result.officialBoards().get(0).name()).isEqualTo("공지사항");
			assertThat(result.officialBoards().get(0).subscribed()).isTrue();
		}

		@Test
		@DisplayName("성공: DB에 설정이 없으면 enum의 defaultEnabled가 반영된다")
		void givenNoStoredSettings_whenGetAllSettings_thenUseEnumDefaultValues() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			given(notificationSettingReader.findSettingMap(userId)).willReturn(emptySettingMap());
			given(mockUser.getAcademicStatus()).willReturn(AcademicStatus.ENROLLED);
			given(boardReader.findAccessibleNoticeBoards(AcademicStatus.ENROLLED)).willReturn(List.of());

			// when
			NotificationSettingResult result = notificationSettingService.getAllSettings(userId);

			// then
			assertThat(result.community().likeOnMyPost()).isTrue();
			assertThat(result.community().commentOnMyPost()).isTrue();
			assertThat(result.community().replyOnMyComment()).isTrue();
			assertThat(result.ceremony().enabled()).isTrue();
			assertThat(result.service().noticeEnabled()).isTrue();
		}

		@Test
		@DisplayName("실패: 존재하지 않는 userId이면 예외 발생")
		void givenInvalidUserId_whenGetAllSettings_thenThrowUserNotFoundException() {
			// given
			String invalidUserId = "non-existent";
			given(userReader.findUserByIdNotDeleted(invalidUserId))
				.willThrow(UserErrorCode.USER_NOT_FOUND.toBaseException());

			// when & then
			assertThatThrownBy(() -> notificationSettingService.getAllSettings(invalidUserId))
				.isInstanceOf(BaseRunTimeV2Exception.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
		}

		@Test
		@DisplayName("실패: 추방된 유저이면 예외 발생")
		void givenBlockedUser_whenGetAllSettings_thenThrowBlockedUserException() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			doThrow(AuthErrorCode.DROPPED_USER.toBaseException())
				.when(userValidator).validateUser(any(User.class));

			// when & then
			assertThatThrownBy(() -> notificationSettingService.getAllSettings(userId))
				.isInstanceOf(BaseRunTimeV2Exception.class)
				.hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DROPPED_USER);
		}
	}

	@Nested
	@DisplayName("개인 알림 설정 업데이트 (updateUserSettings)")
	class UpdateUserSettingsTest {

		private final String userId = "user-001";

		@Test
		@DisplayName("성공: null이 아닌 필드가 있으면 upsert가 호출된다")
		void givenNonNullFields_whenUpdateUserSettings_thenUpsertIsCalled() {
			// given
			UpdateNotificationSettingCommand command = new UpdateNotificationSettingCommand(
				true, null, false, null, null);

			// when
			notificationSettingService.updateUserSettings(userId, command);

			// then
			verify(notificationSettingWriter).upsertSettings(any(), any(UserNotificationSettingMap.class));
		}

		@Test
		@DisplayName("성공: 모든 필드가 null이면 upsert가 호출되지 않는다")
		void givenAllNullFields_whenUpdateUserSettings_thenUpsertIsNotCalled() {
			// given
			UpdateNotificationSettingCommand command = new UpdateNotificationSettingCommand(
				null, null, null, null, null);

			// when
			notificationSettingService.updateUserSettings(userId, command);

			// then
			verify(notificationSettingWriter, never()).upsertSettings(any(), any());
		}
	}

	@Nested
	@DisplayName("공식 게시판 알림 설정 (updateOfficialBoardSubscribe)")
	class UpdateOfficialBoardSubscribeTest {

		private final String userId = "user-001";
		private final String boardId = "board-001";

		@Test
		@DisplayName("성공: 유효한 공지 게시판이면 구독 상태 upsert가 호출된다")
		void givenValidNoticeBoard_whenUpdateOfficialBoardSubscribe_thenUpsertIsCalled() {
			// given
			User mockUser = mock(User.class);
			Board mockBoard = mock(Board.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			given(boardReader.getNoticeBoardOrThrow(boardId)).willReturn(mockBoard);

			// when
			notificationSettingService.updateOfficialBoardSubscribe(userId, boardId, true);

			// then
			verify(notificationSettingWriter).upsertBoardSubscribe(mockUser, mockBoard, true);
		}

		@Test
		@DisplayName("실패: 공지사항 게시판이 아니면 예외 발생")
		void givenNonNoticeBoard_whenUpdateOfficialBoardSubscribe_thenThrowException() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);
			given(boardReader.getNoticeBoardOrThrow(boardId))
				.willThrow(BoardConfigErrorCode.BOARD_NOT_NOTICE.toBaseException());

			// when & then
			assertThatThrownBy(
				() -> notificationSettingService.updateOfficialBoardSubscribe(userId, boardId, true))
				.isInstanceOf(BaseRunTimeV2Exception.class)
				.hasFieldOrPropertyWithValue("errorCode", BoardConfigErrorCode.BOARD_NOT_NOTICE);

			verify(notificationSettingWriter, never()).upsertBoardSubscribe(any(), any(), anyBoolean());
		}
	}

	@Nested
	@DisplayName("개인 알림 설정 기본값 복원 (resetUserSettingsToDefault)")
	class ResetUserSettingsTest {

		private final String userId = "user-001";

		@Test
		@DisplayName("성공: 저장된 커스텀 설정과 무관하게 모든 키가 기본값으로 upsert된다 (커스텀 설정 덮어쓰기)")
		void givenStoredCustomSettings_whenReset_thenUpsertAllKeysWithDefaults() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);

			// when
			notificationSettingService.resetUserSettingsToDefault(userId);

			// then: 모든 enum 키가 defaultEnabled 값으로 채워진 맵으로 upsert가 호출된다
			ArgumentCaptor<UserNotificationSettingMap> captor = ArgumentCaptor
				.forClass(UserNotificationSettingMap.class);
			verify(notificationSettingWriter).upsertSettings(eq(userId), captor.capture());

			UserNotificationSettingMap captured = captor.getValue();
			for (UserNotificationSettingKey key : UserNotificationSettingKey.values()) {
				assertThat(captured.get(key)).isEqualTo(key.isDefaultEnabled());
			}
		}

		@Test
		@DisplayName("성공: 공식계정 게시판 구독 상태는 조회/변경되지 않는다")
		void givenResetRequest_whenReset_thenOfficialBoardSubscriptionUntouched() {
			// given
			User mockUser = mock(User.class);
			given(userReader.findUserByIdNotDeleted(userId)).willReturn(mockUser);

			// when
			notificationSettingService.resetUserSettingsToDefault(userId);

			// then: 게시판 구독 관련 reader/writer는 전혀 호출되지 않는다
			verify(notificationSettingWriter, never()).upsertBoardSubscribe(any(), any(), anyBoolean());
			verifyNoInteractions(boardReader);
			verifyNoInteractions(userBoardSubscribeReader);
		}

		@Test
		@DisplayName("실패: 존재하지 않는 userId이면 원 비즈니스 예외가 전파되고 upsert가 호출되지 않는다")
		void givenInvalidUserId_whenReset_thenThrowUserNotFoundExceptionAndNoUpsert() {
			// given
			String invalidUserId = "non-existent";
			given(userReader.findUserByIdNotDeleted(invalidUserId))
				.willThrow(UserErrorCode.USER_NOT_FOUND.toBaseException());

			// when & then
			assertThatThrownBy(() -> notificationSettingService.resetUserSettingsToDefault(invalidUserId))
				.isInstanceOf(BaseRunTimeV2Exception.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

			verify(notificationSettingWriter, never()).upsertSettings(any(), any());
		}
	}
}
