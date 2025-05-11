package asia.canopy.tree.controller;

import asia.canopy.tree.config.UserPrincipal;
import asia.canopy.tree.domain.QType;
import asia.canopy.tree.dto.DiaryDto;
import asia.canopy.tree.dto.DiaryListResponse;
import asia.canopy.tree.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "일기", description = "일기 관리 API")
@SecurityRequirement(name = "JWT")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "사용자의 모든 일기 조회", description = "현재 로그인한 사용자의 모든 일기를 조회합니다. 조회된 일기 수를 함께 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DiaryListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<DiaryListResponse> getAllDiaries(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        DiaryListResponse response = diaryService.getDiariesByUserId(userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 타입별 조회", description = "일기 타입(morning, lunch, evening)에 따라 일기를 조회합니다. 조회된 일기 수를 함께 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DiaryListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/type/{qtype}")
    public ResponseEntity<DiaryListResponse> getDiariesByType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable QType qtype) {
        DiaryListResponse response = diaryService.getDiariesByUserIdAndType(userPrincipal.getId(), qtype);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 일기 조회", description = "특정 일기 ID의 일기를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "일기를 찾을 수 없음")
    })
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryDto> getDiaryById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long diaryId) {
        DiaryDto diary = diaryService.getDiaryById(userPrincipal.getId(), diaryId);
        return ResponseEntity.ok(diary);
    }

    @Operation(summary = "기간별 일기 조회", description = "시작 날짜와 종료 날짜 사이에 작성된 일기를 조회합니다. 조회된 일기 수를 함께 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DiaryListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/period")
    public ResponseEntity<DiaryListResponse> getDiariesByPeriod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        DiaryListResponse response = diaryService.getDiariesByPeriod(userPrincipal.getId(), start, end);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 생성", description = "새로운 일기를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = DiaryDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping
    public ResponseEntity<DiaryDto> createDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DiaryDto.CreateRequest request) {
        DiaryDto createdDiary = diaryService.createDiary(userPrincipal.getId(), request);
        return ResponseEntity.ok(createdDiary);
    }

    @Operation(summary = "일기 수정", description = "기존 일기를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = DiaryDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "일기를 찾을 수 없음")
    })
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryDto> updateDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryDto.UpdateRequest request) {
        DiaryDto updatedDiary = diaryService.updateDiary(userPrincipal.getId(), diaryId, request);
        return ResponseEntity.ok(updatedDiary);
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "일기를 찾을 수 없음")
    })
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long diaryId) {
        diaryService.deleteDiary(userPrincipal.getId(), diaryId);
        return ResponseEntity.noContent().build();
    }
}