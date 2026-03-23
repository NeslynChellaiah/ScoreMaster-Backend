package com.example.scoremaster.app.config;

import com.example.scoremaster.app.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.example.scoremaster.app.controller")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // If it's already an ApiResponse (e.g., from an exception handler), skip wrapping
        if (body instanceof ApiResponse) {
            return body;
        }

        int statusCode = HttpStatus.OK.value();
        if (response instanceof ServletServerHttpResponse) {
            statusCode = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
        }

        boolean isSuccess = statusCode >= 200 && statusCode < 300;
        
        String message = isSuccess ? "Operation successful" : "An error occurred";
        Object finalData = body;
        
        // If the controller returned a String, intelligently use it as the message instead of data payload
        if (body instanceof String) {
            message = (String) body;
            finalData = null;
        }

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(isSuccess)
                .message(message)
                .statusCode(statusCode)
                .data(finalData)
                .build();

        // Support controllers returning String directly or via ResponseEntity<String>
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(apiResponse);
        }

        return apiResponse;
    }
}
