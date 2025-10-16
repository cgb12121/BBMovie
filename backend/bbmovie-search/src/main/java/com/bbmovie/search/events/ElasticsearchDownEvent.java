package com.bbmovie.search.events;

import org.springframework.context.ApplicationEvent;

public class ElasticsearchDownEvent extends ApplicationEvent {
    public ElasticsearchDownEvent(Object source) {
        super(source);
    }
}
