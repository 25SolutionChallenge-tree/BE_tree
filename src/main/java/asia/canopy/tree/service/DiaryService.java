package asia.canopy.tree.service;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.QType;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.DiaryDto;
import asia.canopy.tree.dto.DiaryListResponse;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.DiaryRepository;
import asia.canopy.tree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    // 사용자의 모든 일기 조회 (카운트 포함)
    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByUserId(Long userId) {
        List<DiaryDto> diaries = diaryRepository.findByUserUserId(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return DiaryListResponse.builder()
                .diaries(diaries)
                .count(diaries.size())
                .build();
    }

    // 사용자의 특정 타입 일기 조회 (카운트 포함)
    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByUserIdAndType(Long userId, QType qtype) {
        List<DiaryDto> diaries = diaryRepository.findByUserUserIdAndQtype(userId, qtype)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return DiaryListResponse.builder()
                .diaries(diaries)
                .count(diaries.size())
                .build();
    }

    // 특정 일기 조회
    @Transactional(readOnly = true)
    public DiaryDto getDiaryById(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByUserUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new ResourceNotFoundException("일기를 찾을 수 없습니다."));
        return convertToDto(diary);
    }

    // 기간별 일기 조회 (카운트 포함)
    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        List<DiaryDto> diaries = diaryRepository.findByUserUserIdAndCreatedAtBetween(userId, start, end)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return DiaryListResponse.builder()
                .diaries(diaries)
                .count(diaries.size())
                .build();
    }

    // 일기 생성
    @Transactional
    public DiaryDto createDiary(Long userId, DiaryDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Diary diary = Diary.builder()
                .user(user)
                .qtype(request.getQtype())
                .diary(request.getDiary())
                .build();

        diary = diaryRepository.save(diary);
        return convertToDto(diary);
    }

    /**
     * 일기를 수정합니다.
     *
     * @param userId 사용자 ID
     * @param diaryId 일기 ID
     * @param request 수정 요청 DTO
     * @return 수정된 일기 DTO
     */
    @Transactional
    public DiaryDto updateDiary(Long userId, Long diaryId, DiaryDto.UpdateRequest request) {
        Diary diary = diaryRepository.findByUserUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new ResourceNotFoundException("일기를 찾을 수 없습니다."));

        // 일기 타입이 있으면 업데이트
        if (request.getQtype() != null) {
            diary.setQtype(request.getQtype());
        }

        // 일기 내용이 있으면 업데이트
        if (request.getDiary() != null && !request.getDiary().isEmpty()) {
            diary.setDiary(request.getDiary());
        }

        diary = diaryRepository.save(diary);
        return convertToDto(diary);
    }

    /**
     * 일기를 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param diaryId 일기 ID
     */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByUserUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new ResourceNotFoundException("일기를 찾을 수 없습니다."));

        diaryRepository.delete(diary);
    }

    // Entity to DTO 변환
    private DiaryDto convertToDto(Diary diary) {
        Long userId = (diary.getUser() != null) ? diary.getUser().getUserId() : null;

        return DiaryDto.builder()
                .diaryId(diary.getDiaryId())
                .userId(userId)
                .qtype(diary.getQtype())
                .diary(diary.getDiary())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}