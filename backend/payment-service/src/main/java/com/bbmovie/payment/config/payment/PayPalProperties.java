package com.bbmovie.payment.config.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.paypal")
public class PayPalProperties {
    private String clientId;
    private String clientSecret;
    private String mode;
    /**
     * <b>NOTE:</b>
     * <p>
     * This is the URL PayPal will redirect the user’s browser to after they approve the payment on PayPal’s site.
     * <p>
     * After the user clicks "Pay" on PayPal, they are sent back to your site at returnUrl.
     * <p>
     * This is not a confirmation that the payment succeeded — it just means the user approved it.
     * <p>
     * <b>IMPORTANT</b>
     * <p>
     * You still have to call PayPal’s "execute payment" or "capture payment" API on your backend to finalize.
     */
    private String returnUrl;

    /**
     * <b>NOTE:</b>
     * <p>
     * The URL PayPal redirects the user to if they cancel during checkout on PayPal’s site.
     * <p>
     * If the user clicks “Cancel and return to merchant” before completing the payment.
     * <b>IMPORTANT</b>
     * <p>
     * Let you handle the UI for “payment canceled” or restore the shopping cart.
     */
    private String cancelUrl;
}