package com.tw.darkhorse.config;

import com.tw.darkhorse.repository.BaseJpaRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
//@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"com.tw.darkhorse.repository"}, repositoryBaseClass = BaseJpaRepositoryImpl.class)
public class JpaConfig {
}
