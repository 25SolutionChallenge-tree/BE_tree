package asia.canopy.tree.repository;

import asia.canopy.tree.domain.Diary;
import asia.canopy.tree.domain.QType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 사용자 ID로 모든 일기 조회
    List<Diary> findByUserId(Long userId);

    // 사용자 ID와 일기 타입으로 조회
    List<Diary> findByUserIdAndQtype(Long userId, QType qtype);

    // 사용자 ID와 일기 ID로 특정 일기 조회
    Optional<Diary> findByUserIdAndDiaryId(Long userId, Long diaryId);

    // 생성 날짜 기간으로 일기 조회
    List<Diary> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}