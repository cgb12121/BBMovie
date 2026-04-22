package com.bbmovie.ai_assistant_service.config.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties("embedding")
public abstract class IgnoreEmbeddingMixin {}
