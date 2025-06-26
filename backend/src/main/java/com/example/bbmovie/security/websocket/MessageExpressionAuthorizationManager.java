package com.example.bbmovie.security.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.expression.EvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.ExpressionAuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;

import org.springframework.expression.Expression;

import java.util.function.Supplier;

@Log4j2
public final class MessageExpressionAuthorizationManager implements AuthorizationManager<MessageAuthorizationContext<?>> {

	@SuppressWarnings({"unchecked", "rawtypes"})
	private final SecurityExpressionHandler<Message<?>> expressionHandler = new DefaultMessageSecurityExpressionHandler();
	private final Expression expression;

	public MessageExpressionAuthorizationManager(String expressionString) {
		if (expressionString == null || expressionString.trim().isEmpty()) {
			throw new IllegalArgumentException("expressionString cannot be empty");
		}
		this.expression = this.expressionHandler.getExpressionParser().parseExpression(expressionString);
	}

	@Override
	public AuthorizationDecision check(Supplier<Authentication> authentication, MessageAuthorizationContext<?> context) {
		try {
			EvaluationContext ctx = this.expressionHandler.createEvaluationContext(authentication, context.getMessage());
			boolean granted = ExpressionUtils.evaluateAsBoolean(this.expression, ctx);
			return new ExpressionAuthorizationDecision(granted, this.expression);
		} catch (Exception e) {
			log.error("Error when granting access SpEL: {}", e.getMessage(), e);
			return new ExpressionAuthorizationDecision(false, this.expression);
		}
	}
}