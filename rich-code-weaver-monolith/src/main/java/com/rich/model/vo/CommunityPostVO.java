package com.rich.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityPostVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String content;

    private String category;

    private Long userId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer replyCount;

    private Integer isTop;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private Boolean hasLiked;

    private List<CommunityReplyVO> latestReplies;
}
