package com.bbmovie.payment.service.impl;

import java.util.Locale;

import com.bbmovie.payment.service.I18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class I18nServiceImpl implements I18nService {
     
    private final MessageSource messageSource;

    @Autowired
    public I18nServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }
}