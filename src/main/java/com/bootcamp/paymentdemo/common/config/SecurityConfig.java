package com.bootcamp.paymentdemo.common.config;

import com.bootcamp.paymentdemo.security.CustomAuthenticationEntryPoint;
import com.bootcamp.paymentdemo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;


import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toStaticResources;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    // 환경 설정 불러오기 위한 Bean 주입
    private final Environment env;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 현재 실행 중인 프로파일이 'prod'인지 확인하는 변수
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");

        http
                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // Session 사용 안 함 (Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // 운영 환경(prod)이 아닐 때만 H2 프레임 옵션 허용
        if (!isProd) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }

        // 요청 권한 설정
        http.authorizeHttpRequests(authorize -> {

                    // H2 Console 허용도 운영 환경(prod)이 아닐 때만!
                    // 운영(MySQL) 환경에서 이 코드가 실행되면 에러가 나기 때문에 감싸준 거예요.
                    if (!isProd) {
                        authorize.requestMatchers("/h2-console/**").permitAll();
                    }

                    authorize
                            // 정적 리소스 (css, js, images 등) 허용
                            .requestMatchers(toStaticResources().atCommonLocations()).permitAll()

                            // 템플릿 페이지 렌더링 허용 (html 파일)
                            .requestMatchers(HttpMethod.GET, "/").permitAll()
                            .requestMatchers(HttpMethod.GET, "/pages/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()

                            // 예상치 못한 오류 등이 발생했는데 LOGIN_ERROR 가 뜨지 않도록 함
                            .requestMatchers(HttpMethod.GET, "/error").permitAll()

                            // Public API 엔드포인트 허용
                            .requestMatchers("/api/public/**").permitAll()
                            .requestMatchers("/api/webhooks/portone").permitAll()

                            // 나머지 전부 인증 필요
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint));

        return http.build();
    }

    /**
     * PasswordEncoder Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
