package com.rich.codeweaver.service.impl.social;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.mapper.CommunityPostMapper;
import com.rich.codeweaver.model.entity.*;
import com.rich.codeweaver.model.vo.CommunityPostVO;
import com.rich.codeweaver.model.vo.UserVO;
import com.rich.codeweaver.service.social.CommunityPostLikeService;
import com.rich.codeweaver.service.social.CommunityPostService;
import com.rich.codeweaver.service.social.CommunityReplyLikeService;
import com.rich.codeweaver.service.social.CommunityReplyService;
import com.rich.codeweaver.service.user.UserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 社区帖子服务实现。
 * 负责帖子发布、列表查询、详情组装以及基础统计更新。
 */
@Service
public class CommunityPostServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost>
        implements CommunityPostService {

    @Resource
    @Lazy
    private CommunityPostLikeService communityPostLikeService;

    @Resource
    @Lazy
    private CommunityReplyService communityReplyService;

    @Resource
    @Lazy
    private CommunityReplyLikeService communityReplyLikeService;

    @Resource
    private UserService userService;

    /**
     * 保存新帖子，并初始化各类统计字段。
     */
    @Override
    public Long addPost(String title, String content, String category, Long userId) {
        CommunityPost post = CommunityPost.builder()
                .title(title)
                .content(content)
                .category(category)
                .userId(userId)
                .viewCount(0)
                .likeCount(0)
                .replyCount(0)
                .isTop(0)
                .build();
        save(post);
        return post.getId();
    }

    /**
     * 分页查询帖子列表。
     * 支持分类、关键词和多种排序方式，同时补充用户信息、点赞状态和最新评论预览。
     */
    @Override
    public Page<CommunityPostVO> listPostByPage(String category, String searchText, String sortField,
                                                String sortOrder, Long currentUserId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .from(CommunityPost.class)
                .eq("category", category, StrUtil.isNotBlank(category));
        if (StrUtil.isNotBlank(searchText)) {
            query.and((Consumer<QueryWrapper>) qw -> qw
                    .like("title", searchText.trim())
                    .or((Consumer<QueryWrapper>) inner -> inner.like("content", searchText.trim())));
        }
        String finalSortField = StrUtil.isNotBlank(sortField) ? sortField : "lastActive";
        String finalSortOrder = StrUtil.isNotBlank(sortOrder) ? sortOrder : "descend";
        boolean asc = "ascend".equals(finalSortOrder) || "asc".equalsIgnoreCase(finalSortOrder);
        if ("views".equalsIgnoreCase(finalSortField) || "viewCount".equalsIgnoreCase(finalSortField)) {
            query.orderBy("isTop DESC, viewCount " + (asc ? "ASC" : "DESC") + ", updateTime DESC");
        } else if ("likes".equalsIgnoreCase(finalSortField) || "likeCount".equalsIgnoreCase(finalSortField)) {
            query.orderBy("isTop DESC, likeCount " + (asc ? "ASC" : "DESC") + ", updateTime DESC");
        } else if ("replies".equalsIgnoreCase(finalSortField) || "replyCount".equalsIgnoreCase(finalSortField)) {
            query.orderBy("isTop DESC, replyCount " + (asc ? "ASC" : "DESC") + ", updateTime DESC");
        } else if ("latest".equalsIgnoreCase(finalSortField) || "createTime".equalsIgnoreCase(finalSortField)) {
            query.orderBy("isTop DESC, createTime " + (asc ? "ASC" : "DESC"));
        } else {
            query.orderBy("isTop DESC, updateTime DESC, createTime DESC");
        }
        Page<CommunityPost> postPage = page(Page.of(pageNum, pageSize), query);
        List<CommunityPostVO> voList = postPage.getRecords().stream()
                .map(post -> convertToVO(post, currentUserId, true))
                .collect(Collectors.toList());
        return new Page<>(voList, pageNum, pageSize, postPage.getTotalRow());
    }

    /**
     * 获取单个帖子详情。
     */
    @Override
    public CommunityPostVO getPostVO(Long postId, Long currentUserId) {
        CommunityPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        return convertToVO(post, currentUserId, false);
    }

    /**
     * 删除帖子，同时清理关联的回复、帖子点赞和回复点赞数据。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePost(Long postId) {
        CommunityPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        // 查出帖子下所有回复 ID，用于清理回复点赞
        List<Long> replyIds = communityReplyService.list(
                QueryWrapper.create().from(CommunityReply.class).where("postId = ?", postId)
        ).stream().map(CommunityReply::getId).collect(Collectors.toList());
        // 清理回复点赞
        if (!replyIds.isEmpty()) {
            communityReplyLikeService.remove(
                    QueryWrapper.create().from(CommunityReplyLike.class).in("replyId", replyIds)
            );
        }
        // 清理帖子下的回复（逻辑删除）
        communityReplyService.remove(
                QueryWrapper.create().from(CommunityReply.class).where("postId = ?", postId)
        );
        // 清理帖子点赞
        communityPostLikeService.remove(
                QueryWrapper.create().from(CommunityPostLike.class).where("postId = ?", postId)
        );
        // 删除帖子（逻辑删除）
        removeById(postId);
    }

    /**
     * 更新帖子置顶状态。
     */
    @Override
    public void updateTopStatus(Long postId, Integer isTop) {
        CommunityPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        UpdateChain.of(CommunityPost.class)
                .set("isTop", isTop)
                .where("id = ?", postId)
                .update();
    }

    /**
     * 以原子方式增加浏览量，避免并发覆盖。
     */
    @Override
    public void increaseViewCount(Long postId) {
        UpdateChain.of(CommunityPost.class)
                .setRaw("viewCount", "viewCount + 1")
                .where("id = ?", postId)
                .update();
    }

    /**
     * 根据回复表重新同步帖子回复数。
     * 适用于需要重新校准统计值的场景。
     */
    @Override
    public void updateReplyCount(Long postId) {
        QueryWrapper replyQuery = QueryWrapper.create()
                .from(com.rich.codeweaver.model.entity.CommunityReply.class)
                .where("postId = ?", postId);
        long replyCount = communityReplyService.count(replyQuery);
        UpdateChain.of(CommunityPost.class)
                .set("replyCount", (int) replyCount)
                .where("id = ?", postId)
                .update();
    }

    /**
     * 将帖子实体转换为帖子展示对象。
     * 根据场景决定是否附带最新评论预览，避免详情页做无意义的额外查询。
     */
    private CommunityPostVO convertToVO(CommunityPost post, Long currentUserId, boolean withLatestReplies) {
        if (post == null) {
            return null;
        }
        CommunityPostVO vo = new CommunityPostVO();
        BeanUtil.copyProperties(post, vo);
        try {
            User user = userService.getById(post.getUserId());
            if (user != null) {
                UserVO userVO = userService.getUserVO(user);
                vo.setUser(userVO);
            }
        } catch (Exception ignored) {
        }
        vo.setHasLiked(currentUserId != null && communityPostLikeService.hasLiked(post.getId(), currentUserId));
        if (withLatestReplies) {
            vo.setLatestReplies(communityReplyService.listLatestReplies(post.getId(), currentUserId, 2));
        }
        return vo;
    }
}
