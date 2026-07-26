package com.example.LMS_BE.config;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Rejects ambiguous repeated scalar query/form parameters instead of silently using one value. */
@Component
public class DuplicateRequestParameterInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      if (entry.getValue() != null && entry.getValue().length > 1) {
        throw new BusinessException(ErrorCode.DUPLICATE_PARAMETER, entry.getKey());
      }
    }
    return true;
  }
}
