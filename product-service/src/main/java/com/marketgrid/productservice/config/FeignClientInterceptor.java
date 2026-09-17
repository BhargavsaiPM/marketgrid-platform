package com.marketgrid.productservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the incoming {@code Authorization} header to outgoing Feign
 * requests so that authenticated vendor-service endpoints (e.g.
 * {@code GET /api/vendors/me}) receive the same JWT.
 *
 * <p>If no current HTTP request is in scope (e.g. async context), the
 * header is silently omitted — public vendor-service endpoints will
 * still work without it.</p>
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                template.header("Authorization", authHeader);
            }
        }
    }
}
