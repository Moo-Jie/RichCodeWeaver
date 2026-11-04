package com.rich.ai.aiTools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 退出工具调用
 * 当任务完成或无需继续使用工具时调用此方法，避免继续调用工具导致无限循环
 *
 * @author DuRuiChi
 * @create 2026/1/29
 **/
@Slf4j
@Component
public class ExitTool extends BaseTool {

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getToolDisplayName() {
        return "退出工具调用";
    }

    @Override
    public String getResultMsg(JSONObject arguments, String result) {
        return "\n\n[任务执行结束，已无需继续调用工具]\n\n";
    }

    /**
     * 退出工具调用
     * 当任务完成或无需继续使用工具时调用此方法，避免AI陷入无限循环调用工具的状态
     * 这是一个终止信号，告知AI系统当前任务已完成，应该停止工具调用并输出最终结果
     *
     * @return 退出确认信息，提示AI任务已完成
     */
    @Tool("当任务完成或无需继续工具调用时执行退出操作，防止产生循环")
    public String exit() {
        // 记录退出操作日志
        log.info("AI调用了退出工具，任务执行结束");

        // 返回退出确认信息，指示AI停止工具调用并输出最终结果
        return "当前任务已经完成，已无需继续调用任何工具，现需要输出最终结果";
    }
}