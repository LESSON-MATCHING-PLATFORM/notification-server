package com.kosa.noticeserver.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor
@ToString
public class TokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "token")
    private String token;

    public TokenEntity(
            @NotBlank String token,
            @NotBlank String userId
    ) {
        this.token = token;
        this.userId = userId;
    }
}
