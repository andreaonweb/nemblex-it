package com.nemblex.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

class AsyncConfigTest {

    @Test
    void asyncConfig_shouldBeAnnotatedWithEnableAsync() {
        assertThat(AsyncConfig.class.isAnnotationPresent(EnableAsync.class)).isTrue();
    }

    @Test
    void taskExecutor_shouldProduceAUsableExecutor() {
        AsyncConfig asyncConfig = new AsyncConfig();

        Executor executor = asyncConfig.taskExecutor();

        assertThat(executor).isNotNull();
    }
}
