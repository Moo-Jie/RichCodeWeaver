package com.rich.app.service;

import com.mybatisflex.core.service.IService;
import com.rich.model.entity.TaskExecution;

/**
 * 任务执行记录服务接口
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
public interface TaskExecutionService extends IService<TaskExecution> {

    /**
     * 创建任务执行记录
     *
     * @param taskType 任务类型
     * @param appId 产物ID
     * @param userId 用户ID
     * @return 任务执行记录ID
     */
    Long createTask(String taskType, Long appId, Long userId);

    /**
     * 更新任务状态和进度
     *
     * @param appId 产物ID
     * @param status 任务状态
     * @param progress 任务进度
     */
    void updateStatus(Long appId, String status, Integer progress);

    /**
     * 更新任务结果
     *
     * @param appId 产物ID
     * @param result 任务结果
     */
    void updateResult(Long appId, String result);

    /**
     * 根据产物ID查询最新的任务执行记录
     *
     * @param appId 产物ID
     * @return 任务执行记录
     */
    TaskExecution getLatestByAppId(Long appId);
}
