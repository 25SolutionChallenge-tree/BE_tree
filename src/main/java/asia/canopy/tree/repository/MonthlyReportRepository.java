package asia.canopy.tree.repository;

import asia.canopy.tree.domain.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    Optional<MonthlyReport> findByUserUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
}
