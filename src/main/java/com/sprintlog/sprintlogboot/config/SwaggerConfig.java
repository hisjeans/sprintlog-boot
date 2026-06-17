package com.sprintlog.sprintlogboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig { // 전체적인 설정 잡는 클래스

    @Bean // 외부 라이브러리에서 제공되는 콘텐츠를 등록하기 위해 bean 등록
    public OpenAPI customOpenApi(){
        List<Server> servers = List.of(
                new Server().url("http://localhost:8080").description("로컬 개발 서버"),
                new Server().url("https://wwww.example.com").description("운영 서버(예정)")
        );
        return new OpenAPI()
                .servers(servers)
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Sprint Log 활동 저장 시스템 API")
                .description("여거 강의, 학습, 읽기 활동들을 저장하고 생성하는 등의 기능을 제공하는 API입니다.")
                .version("1.0.0")
                .contact(new Contact() // 새로운 contact 객체 생성해 작성해야 한다
                        .name("Codeit 13th Development Team")
                        .email("dev@codeit.com"))
                .license(new License()
                        .name("MIT License") // 오픈소스, 무료 배포 대부분 사용
                        .url("https://opensource.org/license/MIT"));

    }
}
