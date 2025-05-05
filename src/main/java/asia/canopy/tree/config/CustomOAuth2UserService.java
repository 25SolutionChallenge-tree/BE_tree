package asia.canopy.tree.config;

import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email;
        String id;

        if (registrationId.equals("google")) {
            email = (String) attributes.get("email");
            id = (String) attributes.get("sub");
        } else if (registrationId.equals("facebook")) {
            email = (String) attributes.get("email");
            id = (String) attributes.get("id");
        } else {
            throw new RuntimeException("Unsupported provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = registerNewSocialUser(registrationId, id, email);
        }

        return new CustomUserDetails(user, attributes);
    }

    private User registerNewSocialUser(String registrationId, String id, String email) {
        User user = User.builder()
                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(id)
                .email(email)
                .emailVerified(true)
                .profileCompleted(false)
                .build();

        return userRepository.save(user);
    }
}