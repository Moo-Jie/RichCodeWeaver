package com.rich.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.rich.model.entity.AppCollaborator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 产物协作者 Mapper接口
 *
 * @author DuRuiChi
 * @since 2026-04-07
 */
@Mapper
public interface AppCollaboratorMapper extends BaseMapper<AppCollaborator> {

    /**
     * 批量查询产物基本信息（名称和封面）
     * 用于填充协作者VO中的产物信息
     */
    @Select("<script>" +
            "SELECT id, appName, cover, userId FROM app WHERE isDelete = 0 AND id IN " +
            "<foreach item='appId' collection='appIds' open='(' separator=',' close=')'>" +
            "#{appId}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectAppInfoByIds(@Param("appIds") Collection<Long> appIds);

    /**
     * 查询单个产物基本信息（含 owner）
     */
    @Select("SELECT id, appName, cover, userId FROM app WHERE isDelete = 0 AND id = #{appId} LIMIT 1")
    Map<String, Object> selectAppInfoById(@Param("appId") Long appId);
}
