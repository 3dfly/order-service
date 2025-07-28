package com.threedfly.orderservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        logger.warn("⚠️ Error endpoint called for path: {}", request.getRequestURI());
        
        try {
            Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                    new ServletWebRequest(request),
                    ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
            );

            String path = (String) attributes.get("path");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found");
            errorResponse.put("path", path != null ? path : "unknown");
            
            logger.info("🔍 Returning error response for path: {}", path != null ? path : "unknown");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("❌ Error in error handler", e);
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Internal Server Error");
            fallbackResponse.put("path", "unknown");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallbackResponse);
        }
    }
}
