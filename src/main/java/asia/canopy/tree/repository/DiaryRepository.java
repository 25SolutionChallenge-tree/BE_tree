package asia.canopy.tree.repository;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.QType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 사용자 ID로 모든 일기 조회
    List<Diary> findByUserUserId(Long userId);

    // 사용자 ID와 일기 타입으로 조회
    List<Diary> findByUserUserIdAndQtype(Long userId, QType qtype);

    // 사용자 ID와 일기 ID로 특정 일기 조회
    Optional<Diary> findByUserUserIdAndDiaryId(Long userId, Long diaryId);

    // 생성 날짜 기간으로 일기 조회
    List<Diary> findByUserUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // 특정 연도와 월의 일기 조회
    @Query("SELECT d FROM Diary d WHERE d.user.userId = :userId AND YEAR(d.createdAt) = :year AND MONTH(d.createdAt) = :month ORDER BY d.createdAt")
    List<Diary> findByUserIdAndYearAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    // 특정 날짜의 일기 조회 (새벽 0시부터 밤 12시까지)
    @Query("SELECT d FROM Diary d WHERE d.user.userId = :userId AND DATE(d.createdAt) = DATE(:date) ORDER BY d.qtype")
    List<Diary> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    // 특정 연도와 월에 사용자가 일기를 작성한 날짜 목록 조회
    @Query("SELECT DISTINCT DATE(d.createdAt) FROM Diary d WHERE d.user.userId = :userId AND YEAR(d.createdAt) = :year AND MONTH(d.createdAt) = :month ORDER BY DATE(d.createdAt)")
    List<java.sql.Date> findDistinctDatesByUserIdAndYearAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
}