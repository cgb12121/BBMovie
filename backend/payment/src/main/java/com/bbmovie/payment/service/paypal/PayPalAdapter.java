package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PayPalPaymentException;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

import static com.bbmovie.payment.service.paypal.PaypalTransactionStatus.*;

@Log4j2
@Service("paypal")
public class PayPalAdapter implements PaymentProviderAdapter {

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.mode}")
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
    @Value("${payment.paypal.return-url}")
    private String returnUrl; //(sometimes called successUrl or redirectUrl)

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
    @Value("${payment.paypal.cancel-url:http://localhost:8080/api/payment/cancel}")
    private String cancelUrl;

    private APIContext getApiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }

    @Override
    public PaymentCreationResponse createPaymentRequest(PaymentRequest request, HttpServletRequest httpServletRequest) {
        /*
                Init object for request:
                {
                  "intent": The payment intent from the request,
                  "payer": {
                    "payment_method": "paypal"
                  },
                  "transactions": [
                    {
                      "amount": {
                        "currency": "[...]",
                        "total": "[...]"
                      },
                      "description": "[...]"
                    }
                  ],
                  "redirect_urls": {
                    "return_url": "[...]",
                    "cancel_url": "[...]"
                  }
                }

         */
        Payment payment = createPayment(request);
        log.info("Created PayPal payment {}", payment.toJSON());
        try {
            /*
                    Execute request to PayPal: Payment.create(getApiContext())

                    Return object:

                            {
                              "id": A unique string identifier for the payment,
                              "intent": The payment intent from the request,
                              "payer": {
                                "payment_method": "paypal"
                              },
                              "transactions": [
                                {
                                  "related_resources": [],
                                  "amount": {
                                    "currency": "[...]",
                                    "total": "[...]"
                                  },
                                  "description": "[...]"
                                }
                              ],
                              "state": "created",
                              "create_time": "[ISO 8601]",
                              "links": [
                                {
                                  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                        This is an API link to retrieve (GET) the current payment details from PayPal's servers.
                                        It's for your backend to check or update the payment status later.
                                        Not for the user.
                                  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                  "href": "https://api.sandbox.paypal.com/v1/payments/payment/{id}",
                                  "rel": "self",
                                  "method": "GET"
                                },
                                {
                                  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                        This is the link you should use for the user.
                                        Redirect the user to this URL (via a browser or app) to approve and complete the
                                        payment on PayPal's website.
                                   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                  "href": "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd[what are these here for?]",
                                  "rel": "approval_url",
                                  "method": "REDIRECT"
                                },
                                {

                                 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                     This is an API link to execute (POST) the payment after user approval.
                                     Your backend calls this to finalize the transaction once the user returns from the approval page.
                                     Not for the user.
                                 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                  "href": "https://api.sandbox.paypal.com/v1/payments/payment/{id}/execute",
                                  "rel": "execute",
                                  "method": "POST"
                                }
                              ]
                            }
             */
            Payment createdPayment = payment.create(getApiContext());
            log.info("Created PayPal payment {}", createdPayment.toJSON());

            String approvalUrl = null;
            for (Links link : createdPayment.getLinks()) {
                if ("approval_url".equals(link.getRel())) {
                    approvalUrl = link.getHref();
                    break;
                }
            }

            if (approvalUrl == null) {
                throw new PayPalPaymentException("Approval URL not found in PayPal response");
            }

            PaymentStatus status = createdPayment.getState().equalsIgnoreCase(APPROVED.getStatus())
                    ? PaymentStatus.SUCCEEDED
                    : PaymentStatus.PENDING;

            return new PaymentCreationResponse(createdPayment.getId(), status, approvalUrl);
        } catch (PayPalRESTException e) {
            log.error("Unable to create PayPal payment", e);
            throw new PayPalPaymentException(e.getDetails());
        }
    }


    @Override
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        try {
            String paymentId = paymentData.get("paymentId");
            if (paymentId == null) {
                throw new PayPalPaymentException("Missing paymentId in verification data");
            }
            // Shows details for a payment, by ID.
            Payment payment = Payment.get(getApiContext(), paymentId);
            log.info("Got payment details from PayPal {}", payment.toJSON());

            String stateOfPayment = payment.getState();
            boolean success = stateOfPayment.equalsIgnoreCase(COMPLETED.getStatus());

            String messageForClient = getMessage(stateOfPayment);

            return PaymentVerificationResponse.builder()
                    .isValid(success)
                    .transactionId(payment.getId())
                    .code(payment.getState())
                    .message(messageForClient)
                    .providerPayloadStringJson(payment.getTransactions())
                    .responseToProviderStringJson(null)
                    .build();
        } catch (PayPalRESTException e) {
            log.error("Unable to verify PayPal payment", e);
            throw new PayPalPaymentException("Unable to verify PayPal payment");
        }
    }

    @Override
    public PaymentVerificationResponse handleWebhook(CallbackRequestContext ctx) {
        // Typically you would validate headers and raw payload signature with PayPal SDK or your verifier.
        // For now, treat the presence of payload as a basic check and return success=false to force implementors to finish
        boolean hasPayload = ctx.getRawBody() != null && !ctx.getRawBody().isBlank();
        return new PaymentVerificationResponse(hasPayload, null, null, null, null, null);
    }

    @Override
    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        return null;
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        try {
            Sale sale = Sale.get(getApiContext(), paymentId);
            RefundRequest refundRequest = new RefundRequest();
            DetailedRefund refund = sale.refund(getApiContext(), refundRequest);
            String status = COMPLETED.getStatus().equals(refund.getState())
                    ? PaymentStatus.SUCCEEDED.getStatus()
                    : PaymentStatus.FAILED.getStatus();
            return new RefundResponse(refund.getId(), status);
        } catch (PayPalRESTException e) {
            log.error("Unable to refund PayPal payment", e);
            throw new PayPalPaymentException("Unable to refund PayPal payment");
        }
    }

    private Payment createPayment(PaymentRequest request) {
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(request.getAmount() != null ? request.getAmount().toString() : null);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Order " + request.getOrderId());

        return createPayment(transaction);
    }

    private Payment createPayment(Transaction transaction) {
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(returnUrl);
        redirectUrls.setCancelUrl(cancelUrl);

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Collections.singletonList(transaction));
        payment.setRedirectUrls(redirectUrls);
        return payment;
    }

    private static String getMessage(String stateOfPayment) {
        String messageForClient = "";
        if (stateOfPayment.equalsIgnoreCase(COMPLETED.getStatus())) {
            messageForClient = "The payment is completed, please check your order.";
        }
        if (stateOfPayment.equalsIgnoreCase(APPROVED.getStatus())) {
            messageForClient = "The payment is pending, please check your order.";
        }
        if (stateOfPayment.equalsIgnoreCase(FAILED.getStatus())) {
            messageForClient = "The payment is failed. if your balance has been deducted, please contact the admin or PayPal hotline for support.";
        }
        return messageForClient;
    }
}
