package com.rich.app.utils.ConvertTokenStreamToFluxUtils;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * AI 客服 TokenStream 转 Flux 转换器
 *
 * @author DuRuiChi
 * @create 2026/4/9
 */
@Slf4j
@Component
public class ConvertCustomerServiceTokenStreamToFluxUtils {

    public Flux<String> convertTokenStreamToFlux(TokenStream tokenStream) {
        return Flux.create(sink -> tokenStream
                .onPartialResponse(sink::next)
                .onCompleteResponse((ChatResponse response) -> sink.complete())
                .onError(error -> {
                    String errorMsg = error.getMessage() != null ? error.getMessage() : "";
                    if (errorMsg.contains("JsonEOFException") || errorMsg.contains("Unexpected end-of-input")) {
                        log.warn("AI 客服 TokenStream JSON 截断，优雅结束: {}", errorMsg);
                        sink.complete();
                    } else {
                        sink.error(error);
                    }
                })
                .start());
    }
}
