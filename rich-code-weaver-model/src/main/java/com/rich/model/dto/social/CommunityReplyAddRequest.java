package com.rich.model.dto.social;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommunityReplyAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long postId;

    private String content;
}
