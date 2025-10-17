package com.bbmovie.search.dto.event;

import org.springframework.context.ApplicationEvent;

public class ElasticsearchUpEvent extends ApplicationEvent {
    public ElasticsearchUpEvent(Object source) {
        super(source);
    }
}
