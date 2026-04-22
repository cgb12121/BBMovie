package com.bbmovie.ai_assistant_service.tool.type.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("commonTools")
@RequiredArgsConstructor
public class ProcessDocument implements CommonTools {
}
