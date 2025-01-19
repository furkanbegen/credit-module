package com.furkanbegen.creditmodule.config;

import com.furkanbegen.creditmodule.security.CustomAuthenticationFailureHandler;
import com.furkanbegen.creditmodule.security.CustomJwtAuthenticationConverter;
import com.furkanbegen.creditmodule.security.CustomJwtDecoder;
import com.furkanbegen.creditmodule.security.UserDetailService;
import com.furkanbegen.creditmodule.service.TokenRevocationService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
public class SecurityConfig {

  @Value("${jwt.key}")
  private String jwtKey;

  private final UserDetailService userDetailService;
  private final TokenRevocationService tokenRevocationService;

  private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

  public SecurityConfig(
      UserDetailService userDetailService,
      final TokenRevocationService tokenRevocationService,
      CustomJwtAuthenticationConverter customJwtAuthenticationConverter) {
    this.userDetailService = userDetailService;
    this.tokenRevocationService = tokenRevocationService;
    this.customJwtAuthenticationConverter = customJwtAuthenticationConverter;
  }

  @Bean
  public WebSecurityCustomizer ignoringCustomizer() {
    return web -> web.ignoring().requestMatchers("/status/**");
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .authorizeHttpRequests(
            authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/v1/login")
                    .permitAll()
                    .requestMatchers("/api/v1/logout")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2ResourceServer ->
                oauth2ResourceServer
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter))
                    .withObjectPostProcessor(
                        // When revoking a token in TokenRevocationService, Spring throws an
                        // AuthenticationServiceException which cannot be handled by
                        // ControllerAdvice.
                        // To return a 401 status code for token revocation failures, we override
                        // the
                        // default authentication failure handler with a custom one.
                        new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                          @Override
                          public <O extends BearerTokenAuthenticationFilter> O postProcess(
                              final O object) {
                            object.setAuthenticationFailureHandler(
                                new CustomAuthenticationFailureHandler());
                            return object;
                          }
                        }))
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .userDetailsService(userDetailService)
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(userDetailService);
    authenticationProvider.setPasswordEncoder(passwordEncoder());
    return authenticationProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey.getBytes()));
  }

  @Bean
  @Primary
  public JwtDecoder jwtDecoder() {
    var bytes = jwtKey.getBytes();
    var originalKey = new SecretKeySpec(bytes, 0, bytes.length, MacAlgorithm.HS512.getName());
    var nimbusJwtDecoder =
        NimbusJwtDecoder.withSecretKey(originalKey).macAlgorithm(MacAlgorithm.HS512).build();
    return new CustomJwtDecoder(nimbusJwtDecoder, tokenRevocationService);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName(
        "authorities"); // defaults to "scope" or "scp"

    grantedAuthoritiesConverter.setAuthorityPrefix(""); // defaults to "SCOPE_"
    final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }
}
