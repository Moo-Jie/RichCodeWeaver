package com.rich.social.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.model.entity.CommunityPost;
import com.rich.model.vo.CommunityPostVO;

/**
 * 社区帖子服务接口。
 */
public interface CommunityPostService extends IService<CommunityPost> {

    /**
     * 新增帖子。
     */
    Long addPost(String title, String content, String category, Long userId);

    /**
     * 分页查询帖子列表。
     */
    Page<CommunityPostVO> listPostByPage(String category, String searchText, String sortField,
                                         String sortOrder, Long currentUserId, long pageNum, long pageSize);

    /**
     * 获取帖子详情视图对象。
     */
    CommunityPostVO getPostVO(Long postId, Long currentUserId);

    /**
     * 更新帖子置顶状态。
     */
    void updateTopStatus(Long postId, Integer isTop);

    /**
     * 删除帖子（同时清理关联的回复与点赞数据）。
     */
    void deletePost(Long postId);

    /**
     * 增加帖子浏览量。
     */
    void increaseViewCount(Long postId);

    /**
     * 重新同步帖子回复数。
     */
    void updateReplyCount(Long postId);
}
