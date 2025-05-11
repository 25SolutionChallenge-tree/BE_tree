package asia.canopy.tree.service;

import asia.canopy.tree.config.UserPrincipal;
import asia.canopy.tree.config.jwt.JwtTokenProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.domain.VerificationToken;
import asia.canopy.tree.dto.EmailVerifiedSignUpRequest;
import asia.canopy.tree.dto.JwtResponse;
import asia.canopy.tree.dto.LoginRequest;
import asia.canopy.tree.dto.SignUpRequest;
import asia.canopy.tree.exception.BadRequestException;
import asia.canopy.tree.exception.EmailAlreadyExistsException;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.UserRepository;
import asia.canopy.tree.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void registerUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .provider("local")
                .emailVerified(false)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void registerVerifiedUser(EmailVerifiedSignUpRequest signUpRequest) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        // 인증 코드 검증
        verifyEmailForSignUp(signUpRequest.getEmail(), signUpRequest.getCode());

        // 인증 토큰 삭제
        verificationTokenRepository.deleteByEmail(signUpRequest.getEmail());

        // 인증된 사용자 생성
        User user = User.builder()
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .provider("local")
                .emailVerified(true) // 이메일 인증 완료
                .build();

        userRepository.save(user);
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String jwt = jwtTokenProvider.generateToken(userPrincipal);

        return new JwtResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                userPrincipal.getUser().isProfileComplete()
        );
    }

    @Transactional
    public boolean verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        VerificationToken token = verificationTokenRepository.findByEmailAndToken(email, code)
                .orElseThrow(() -> new BadRequestException("유효하지 않은 인증 코드입니다."));

        if (token.isExpired()) {
            verificationTokenRepository.delete(token);
            throw new BadRequestException("만료된 인증 코드입니다. 인증 코드를 재발급 받으세요.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(token);

        return true;
    }

    @Transactional
    public boolean verifyEmailForSignUp(String email, String code) {
        VerificationToken token = verificationTokenRepository.findByEmailAndToken(email, code)
                .orElseThrow(() -> new BadRequestException("유효하지 않은 인증 코드입니다."));

        if (token.isExpired()) {
            verificationTokenRepository.delete(token);
            throw new BadRequestException("만료된 인증 코드입니다. 인증 코드를 재발급 받으세요.");
        }

        return true;
    }
}