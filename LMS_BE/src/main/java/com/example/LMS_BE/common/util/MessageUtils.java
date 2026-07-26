package com.example.LMS_BE.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtils {
  private final MessageSource source;

  public String get(String code, Object... args) {
    return source.getMessage(code, args, code, LocaleContextHolder.getLocale());
  }
}
