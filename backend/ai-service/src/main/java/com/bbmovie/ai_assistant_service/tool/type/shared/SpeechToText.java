package com.bbmovie.ai_assistant_service.tool.type.shared;

import com.bbmovie.ai_assistant_service.service.WhisperService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
@Qualifier("commonTools")
@RequiredArgsConstructor
public class SpeechToText implements CommonTools {

    private final WhisperService whisperService;

//    @Tool
//    public void transcribeAudio() {
//
//    }
}