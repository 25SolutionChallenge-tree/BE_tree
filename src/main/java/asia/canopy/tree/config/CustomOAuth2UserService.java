package asia.canopy.tree.config;

import asia.canopy.tree.domain.AuthProvider;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email;
        String name;
        String id;

        if (registrationId.equals("google")) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            id = (String) attributes.get("sub");
        } else if (registrationId.equals("facebook")) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            id = (String) attributes.get("id");
        } else {
            throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported");
        }

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                throw new OAuth2AuthenticationException("You are signed up with " +
                        user.getProvider() + ". Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, name);
        } else {
            user = registerNewUser(registrationId, id, email, name);
        }

        return new CustomUserDetails(user, attributes);
    }

    private User registerNewUser(String registrationId, String id, String email, String name) {
        User user = User.builder()
                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(id)
                .email(email)
                .name(name)
                .emailVerified(true)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User user, String name) {
        user.setName(name);
        return userRepository.save(user);
    }
}
