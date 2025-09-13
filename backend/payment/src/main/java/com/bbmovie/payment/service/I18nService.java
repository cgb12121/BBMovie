package com.bbmovie.payment.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class I18nService {
     
    private final MessageSource messageSource;

    @Autowired
    public I18nService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }
}