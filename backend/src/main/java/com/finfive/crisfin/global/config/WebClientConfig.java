package com.finfive.crisfin.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final int TIMEOUT_SECONDS = 30;

    /**
     * Shared {@link ObjectMapper} with Java-time support and ISO-8601 date serialisation.
     * Declared here so it can be reused by WebClient codecs and any other bean that
     * needs JSON serialisation.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings, not epoch numbers
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Pre-configured {@link WebClient.Builder} with a 30-second connect / read / write
     * timeout and the shared Jackson codec.
     *
     * <p>Downstream beans should inject {@code WebClient.Builder} and call
     * {@code .baseUrl(...).build()} rather than injecting a fully-built
     * {@code WebClient}, so each service can set its own base URL.</p>
     */
    @Bean
    public WebClient.Builder webClientBuilder(ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis())
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(TIMEOUT_SECONDS, TimeUnit.SECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                    // Allow up to 16 MB response bodies (LLM streaming responses can be large)
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
                })
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }
}
