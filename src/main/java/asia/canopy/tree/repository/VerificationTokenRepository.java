package asia.canopy.tree.repository;

import asia.canopy.tree.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByEmailAndToken(String email, String token);

    void deleteByEmail(String email);
}