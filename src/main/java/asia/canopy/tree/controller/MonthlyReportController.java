package asia.canopy.tree.controller;

import asia.canopy.tree.config.UserPrincipal;
import asia.canopy.tree.dto.MonthlyReportDto;
import asia.canopy.tree.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "리포트", description = "사용자 일기 분석 리포트 API")
@SecurityRequirement(name = "bearerAuth")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    @Operation(summary = "월간 리포트 조회", description = "특정 연도와 월의 일기 데이터를 분석한 월간 리포트를 제공합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리포트 조회 성공",
                    content = @Content(schema = @Schema(implementation = MonthlyReportDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "해당 월의 일기가 존재하지 않음")
    })
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        // 연도와 월 파라미터가 없으면 현재 연도와 월을 사용
        YearMonth currentYearMonth = YearMonth.now();
        int reportYear = (year != null) ? year : currentYearMonth.getYear();
        int reportMonth = (month != null) ? month : currentYearMonth.getMonthValue();

        MonthlyReportDto report = monthlyReportService.getOrCreateMonthlyReport(
                userPrincipal.getId(), reportYear, reportMonth);

        return ResponseEntity.ok(report);
    }

    @Operation(summary = "월간 리포트 생성", description = "특정 연도와 월의 일기 데이터를 분석하여 새로운 월간 리포트를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리포트 생성 성공",
                    content = @Content(schema = @Schema(implementation = MonthlyReportDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "해당 월의 일기가 존재하지 않음")
    })
    @PostMapping("/monthly")
    public ResponseEntity<MonthlyReportDto> createMonthlyReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String yearMonth) {

        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        MonthlyReportDto report = monthlyReportService.generateMonthlyReport(
                userPrincipal.getId(), year, month);

        return ResponseEntity.ok(report);
    }
}