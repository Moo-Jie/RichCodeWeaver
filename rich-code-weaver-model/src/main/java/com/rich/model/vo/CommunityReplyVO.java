package com.rich.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommunityReplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    private Integer likeCount;

    private LocalDateTime createTime;

    private UserVO user;

    private Boolean hasLiked;
}
