package com.furkanbegen.creditmodule.config;

import static org.mockito.Mockito.mock;

import com.furkanbegen.creditmodule.mapper.InstallmentMapper;
import com.furkanbegen.creditmodule.mapper.LoanMapper;
import com.furkanbegen.creditmodule.repository.CustomerRepository;
import com.furkanbegen.creditmodule.repository.UserRepository;
import com.furkanbegen.creditmodule.security.CustomJwtAuthenticationConverter;
import com.furkanbegen.creditmodule.security.CustomJwtDecoder;
import com.furkanbegen.creditmodule.security.TestCustomerSecurityEvaluator;
import com.furkanbegen.creditmodule.security.UserDetailService;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityTestConfig {

  @MockBean private UserRepository userRepository;

  @MockBean private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

  @MockBean private CustomJwtDecoder customJwtDecoder;

  @MockBean private UserDetailService userDetailService;

  @Bean
  public CustomerRepository customerRepository() {
    return mock(CustomerRepository.class);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf().disable().authorizeHttpRequests().anyRequest().authenticated().and().httpBasic();

    // Disable JWT for testing
    http.oauth2ResourceServer().jwt().decoder(token -> null);

    return http.build();
  }

  @Bean
  public TestCustomerSecurityEvaluator customerSecurity(CustomerRepository customerRepository) {
    return new TestCustomerSecurityEvaluator(customerRepository);
  }

  @Bean
  public LoanMapper loanMapper() {
    return new LoanMapper(installmentMapper());
  }

  @Bean
  public InstallmentMapper installmentMapper() {
    return new InstallmentMapper();
  }

  @Bean
  public UserDetailService userDetailService() {
    return new UserDetailService(userRepository);
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor() {
    return beanFactory -> {
      if (beanFactory instanceof ConfigurableListableBeanFactory) {
        ((ConfigurableListableBeanFactory) beanFactory)
            .registerResolvableDependency(CustomerRepository.class, customerRepository());
      }
    };
  }
}
