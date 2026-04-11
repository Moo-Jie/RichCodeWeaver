package com.rich.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("community_post")
public class CommunityPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("category")
    private String category;

    @Column("userId")
    private Long userId;

    @Column("viewCount")
    private Integer viewCount;

    @Column("likeCount")
    private Integer likeCount;

    @Column("replyCount")
    private Integer replyCount;

    @Column("isTop")
    private Integer isTop;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
