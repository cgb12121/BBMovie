package com.bbmovie.auth.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    String displayedUsername;
    String profilePictureUrl;
}