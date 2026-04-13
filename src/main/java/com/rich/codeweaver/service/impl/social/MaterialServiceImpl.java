package com.rich.codeweaver.service.impl.social;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import com.rich.codeweaver.mapper.MaterialCategoryMapper;
import com.rich.codeweaver.mapper.MaterialMapper;
import com.rich.codeweaver.model.dto.material.MaterialQueryRequest;
import com.rich.codeweaver.model.entity.Material;
import com.rich.codeweaver.model.entity.MaterialCategory;
import com.rich.codeweaver.model.entity.User;
import com.rich.codeweaver.model.vo.MaterialVO;
import com.rich.codeweaver.model.vo.UserVO;
import com.rich.codeweaver.service.social.MaterialService;
import com.rich.codeweaver.service.user.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 素材 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/3/30
 */
@Service
@Slf4j
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material>
        implements MaterialService {

    @Resource
    private MaterialCategoryMapper materialCategoryMapper;

    @Resource
    private UserService userService;

    /**
     * 构建查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(MaterialQueryRequest queryRequest) {
        if (queryRequest == null) {
            return QueryWrapper.create();
        }

        Long id = queryRequest.getId();
        String materialName = queryRequest.getMaterialName();
        Long categoryId = queryRequest.getCategoryId();
        String materialType = queryRequest.getMaterialType();
        String tags = queryRequest.getTags();
        Integer isPublic = queryRequest.getIsPublic();
        Long userId = queryRequest.getUserId();
        String searchText = queryRequest.getSearchText();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id, id != null)
                .eq("categoryId", categoryId, categoryId != null)
                .eq("materialType", materialType, StrUtil.isNotBlank(materialType))
                .eq("isPublic", isPublic, isPublic != null)
                .eq("userId", userId, userId != null)
                .like("materialName", materialName, StrUtil.isNotBlank(materialName))
                .like("tags", tags, StrUtil.isNotBlank(tags));

        // 关键词搜索（搜索名称、描述、标签）
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and((Consumer<QueryWrapper>) qw -> qw
                    .like("materialName", searchText)
                    .or((Consumer<QueryWrapper>) inner -> inner.like("description", searchText))
                    .or((Consumer<QueryWrapper>) inner -> inner.like("tags", searchText)));
        }

        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equalsIgnoreCase(sortOrder) || "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    /**
     * 将实体转换为 VO
     */
    @Override
    public MaterialVO getMaterialVO(Material material) {
        if (material == null) {
            return null;
        }
        MaterialVO vo = new MaterialVO();
        BeanUtil.copyProperties(material, vo);

        // 查询分类名称
        if (material.getCategoryId() != null) {
            MaterialCategory category = materialCategoryMapper.selectOneById(material.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        return vo;
    }

    /**
     * 批量转换为 VO 列表
     */
    @Override
    public List<MaterialVO> getMaterialVOList(List<Material> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 批量查询分类信息
        Set<Long> categoryIds = list.stream()
                .map(Material::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(categoryIds)) {
            List<MaterialCategory> categories = materialCategoryMapper.selectListByIds(new ArrayList<>(categoryIds));
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getCategoryName));
        }

        // 批量查询用户信息
        Set<Long> userIds = list.stream()
                .map(Material::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<User> users = userService.listByIds(userIds);
            userVOMap = users.stream()
                    .collect(Collectors.toMap(User::getId, userService::getUserVO));
        }

        // 转换为 VO
        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        return list.stream().map(material -> {
            MaterialVO vo = new MaterialVO();
            BeanUtil.copyProperties(material, vo);
            // 设置分类名称
            vo.setCategoryName(finalCategoryNameMap.get(material.getCategoryId()));
            // 设置用户信息
            vo.setUser(finalUserVOMap.get(material.getUserId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询用户的素材列表
     */
    @Override
    public List<Material> listByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return this.list(QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false));
    }

    /**
     * 根据id列表查询素材
     */
    @Override
    public List<Material> listByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return this.list(QueryWrapper.create().in("id", ids));
    }

    /**
     * 增加素材使用次数
     */
    @Override
    public void incrementUseCount(Long materialId) {
        if (materialId == null) {
            return;
        }
        // 使用原子更新，避免并发问题
        UpdateChain.of(Material.class)
                .setRaw("useCount", "useCount + 1")
                .eq("id", materialId)
                .update();
    }

    /**
     * 批量增加素材使用次数
     */
    @Override
    public void batchIncrementUseCount(List<Long> materialIds) {
        if (CollUtil.isEmpty(materialIds)) {
            return;
        }
        // 使用原子更新，避免并发问题
        UpdateChain.of(Material.class)
                .setRaw("useCount", "useCount + 1")
                .in("id", materialIds)
                .update();
    }

    /**
     * 将素材列表格式化为提示词文本
     * 用于拼接到对话提示词中
     */
    @Override
    public String formatMaterialsForPrompt(List<Material> materials) {
        if (CollUtil.isEmpty(materials)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n---\n");
        sb.append("【必须使用的素材资源】以下是用户提供的素材，你**必须**在生成的代码中使用这些素材，不可忽略或用占位内容替代：\n\n");

        // 按类型分组
        Map<String, List<Material>> groupedMaterials = materials.stream()
                .collect(Collectors.groupingBy(Material::getMaterialType));

        // 图片素材
        List<Material> images = groupedMaterials.get("image");
        if (CollUtil.isNotEmpty(images)) {
            sb.append("## 图片素材（必须全部使用）\n");
            for (int i = 0; i < images.size(); i++) {
                Material img = images.get(i);
                sb.append(String.format("%d. **%s**\n", i + 1, img.getMaterialName()));
                sb.append(String.format("   - URL: %s\n", img.getContent()));
                if (StrUtil.isNotBlank(img.getDescription())) {
                    sb.append(String.format("   - 用途说明: %s\n", img.getDescription()));
                }
            }
            sb.append("\n");
        }

        // 文本素材
        List<Material> texts = groupedMaterials.get("text");
        if (CollUtil.isNotEmpty(texts)) {
            sb.append("## 文本素材（必须融入页面内容）\n");
            for (int i = 0; i < texts.size(); i++) {
                Material text = texts.get(i);
                sb.append(String.format("%d. **%s**\n", i + 1, text.getMaterialName()));
                sb.append(String.format("   内容:\n   ```\n   %s\n   ```\n", text.getContent()));
                if (StrUtil.isNotBlank(text.getDescription())) {
                    sb.append(String.format("   用途说明: %s\n", text.getDescription()));
                }
            }
            sb.append("\n");
        }

        // 视频素材
        List<Material> videos = groupedMaterials.get("video");
        if (CollUtil.isNotEmpty(videos)) {
            sb.append("## 视频素材\n");
            for (int i = 0; i < videos.size(); i++) {
                Material video = videos.get(i);
                sb.append(String.format("%d. **%s**: %s\n", i + 1, video.getMaterialName(), video.getContent()));
                if (StrUtil.isNotBlank(video.getDescription())) {
                    sb.append(String.format("   用途说明: %s\n", video.getDescription()));
                }
            }
            sb.append("\n");
        }

        // 音频素材
        List<Material> audios = groupedMaterials.get("audio");
        if (CollUtil.isNotEmpty(audios)) {
            sb.append("## 音频素材\n");
            for (int i = 0; i < audios.size(); i++) {
                Material audio = audios.get(i);
                sb.append(String.format("%d. **%s**: %s\n", i + 1, audio.getMaterialName(), audio.getContent()));
                if (StrUtil.isNotBlank(audio.getDescription())) {
                    sb.append(String.format("   用途说明: %s\n", audio.getDescription()));
                }
            }
            sb.append("\n");
        }

        // 链接素材
        List<Material> links = groupedMaterials.get("link");
        if (CollUtil.isNotEmpty(links)) {
            sb.append("## 参考链接\n");
            for (int i = 0; i < links.size(); i++) {
                Material link = links.get(i);
                sb.append(String.format("%d. **%s**: %s\n", i + 1, link.getMaterialName(), link.getContent()));
                if (StrUtil.isNotBlank(link.getDescription())) {
                    sb.append(String.format("   说明: %s\n", link.getDescription()));
                }
            }
            sb.append("\n");
        }

        sb.append("---\n");
        return sb.toString();
    }
}
