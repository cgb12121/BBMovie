package com.bbmovie.gateway.security.anonymity.ipapi.response;

import lombok.Data;

/**
 * Represents abuse-related information for a specific entity.
 * <p>
 * The Abuse class is designed to encapsulate contact details and related
 * information for reporting abuse incidents or activities. This can include
 * details necessary for addressing complaints or inquiries regarding abusive
 * behavior. It is commonly used in contexts where abuse-related metadata needs
 * to be captured or processed.
 * <p>
 * Key Details:
 *  <p>- name: The name associated with the entity responsible or involved in the abuse.
 *  <p>- address: The physical or postal address for contacting the entity.
 *  <p>- email: An email address designated for reporting abuse or related concerns.
 *  <p>- phone: A phone number provided for abuse-related communication.
 */
@Data
public class Abuse {
    private String name;
    private String address;
    private String email;
    private String phone;
}