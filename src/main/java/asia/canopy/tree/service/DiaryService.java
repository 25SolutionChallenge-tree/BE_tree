package asia.canopy.tree.service;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.QType;
import asia.canopy.tree.dto.DiaryDto;
import asia.canopy.tree.dto.DiaryListResponse;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.DiaryRepository;
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

    // 사용자의 모든 일기 조회 (카운트 포함)
    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByUserId(Long userId) {
        List<DiaryDto> diaries = diaryRepository.findByUserId(userId)
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
        List<DiaryDto> diaries = diaryRepository.findByUserIdAndQtype(userId, qtype)
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
        Diary diary = diaryRepository.findByUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new ResourceNotFoundException("일기를 찾을 수 없습니다."));
        return convertToDto(diary);
    }

    // 기간별 일기 조회 (카운트 포함)
    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        List<DiaryDto> diaries = diaryRepository.findByUserIdAndCreatedAtBetween(userId, start, end)
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
        Diary diary = Diary.builder()
                .userId(userId)
                .qtype(request.getQtype())
                .diary(request.getDiary())
                .build();

        Diary savedDiary = diaryRepository.save(diary);
        return convertToDto(savedDiary);
    }

    // Entity to DTO 변환
    private DiaryDto convertToDto(Diary diary) {
        return DiaryDto.builder()
                .diaryId(diary.getDiaryId())
                .userId(diary.getUserId())
                .qtype(diary.getQtype())
                .diary(diary.getDiary())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}