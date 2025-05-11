package asia.canopy.tree.service;

import asia.canopy.tree.domain.Avatar;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.UserProfileResponse;
import asia.canopy.tree.exception.ResourceNotFoundException;
import asia.canopy.tree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        return UserProfileResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .emailVerified(user.isEmailVerified())
                .profileComplete(user.isProfileComplete())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, Avatar avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        user.setNickname(nickname);
        user.setAvatar(avatar);

        User updatedUser = userRepository.save(user);

        return UserProfileResponse.builder()
                .id(updatedUser.getUserId())
                .email(updatedUser.getEmail())
                .nickname(updatedUser.getNickname())
                .avatar(updatedUser.getAvatar())
                .emailVerified(updatedUser.isEmailVerified())
                .profileComplete(updatedUser.isProfileComplete())
                .build();
    }
}