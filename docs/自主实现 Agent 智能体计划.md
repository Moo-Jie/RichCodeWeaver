## 自主实现 Agent 智能体攻略

**（SpringAI框架版本，如果是Langchain4j框架自行更改语法）**



下面我们仿照 OpenManus 的实现原理进行开发，虽然 OpenManus 代码量很大，但其实很多代码都是在实现智能体所需的支持系统，比如调用大模型、会话记忆、工具调用能力等。如果使用 AI 开发框架，这些能力都不需要我们自己实现，代码量会简单很多。下面就让我们基于 Spring AI 框架，实现一个简化版的 Agent 智能体。

### 定义数据模型

新建 `agent.model` 包，将所有用到的数据模型（实体类、枚举类等）都放到该包下。

目前我们只需要定义 Agent 的状态枚举，用于控制智能体的执行。AgentState 代码如下：

```java
/**  
 * 代理执行状态的枚举类  
 */  
public enum AgentState {  
  
    /**  
     * 空闲状态  
     */  
    IDLE,  
  
    /**  
     * 运行中状态  
     */  
    RUNNING,  
  
    /**  
     * 已完成状态  
     */  
    FINISHED,  
  
    /**  
     * 错误状态  
     */  
    ERROR  
}
```

### 核心架构开发

首先定义智能体的核心架构，包括以下类：

- BaseAgent：智能体基类，定义基本信息和多步骤执行流程
- ReActAgent：实现思考和行动两个步骤的智能体
- ToolCallAgent：实现工具调用能力的智能体
- YuManus：最终可使用的 Manus 实例

#### 1、开发基础 Agent 类

参考 OpenManus 的实现方式，BaseAgent 的代码如下：

```java
/**  
 * 抽象基础代理类，用于管理代理状态和执行流程。  
 *   
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。  
 * 子类必须实现step方法。  
 */  
@Data  
@Slf4j  
public abstract class BaseAgent {  
  
    // 核心属性  
    private String name;  
  
    // 提示  
    private String systemPrompt;  
    private String nextStepPrompt;  
  
    // 状态  
    private AgentState state = AgentState.IDLE;  
  
    // 执行控制  
    private int maxSteps = 10;  
    private int currentStep = 0;  
  
    // LLM  
    private ChatClient chatClient;  
  
    // Memory（需要自主维护会话上下文）  
    private List<Message> messageList = new ArrayList<>();  
  
    /**  
     * 运行代理  
     *  
     * @param userPrompt 用户提示词  
     * @return 执行结果  
     */  
    public String run(String userPrompt) {  
        if (this.state != AgentState.IDLE) {  
            throw new RuntimeException("Cannot run agent from state: " + this.state);  
        }  
        if (StringUtil.isBlank(userPrompt)) {  
            throw new RuntimeException("Cannot run agent with empty user prompt");  
        }  
        // 更改状态  
        state = AgentState.RUNNING;  
        // 记录消息上下文  
        messageList.add(new UserMessage(userPrompt));  
        // 保存结果列表  
        List<String> results = new ArrayList<>();  
        try {  
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {  
                int stepNumber = i + 1;  
                currentStep = stepNumber;  
                log.info("Executing step " + stepNumber + "/" + maxSteps);  
                // 单步执行  
                String stepResult = step();  
                String result = "Step " + stepNumber + ": " + stepResult;  
                results.add(result);  
            }  
            // 检查是否超出步骤限制  
            if (currentStep >= maxSteps) {  
                state = AgentState.FINISHED;  
                results.add("Terminated: Reached max steps (" + maxSteps + ")");  
            }  
            return String.join("\n", results);  
        } catch (Exception e) {  
            state = AgentState.ERROR;  
            log.error("Error executing agent", e);  
            return "执行错误" + e.getMessage();  
        } finally {  
            // 清理资源  
            this.cleanup();  
        }  
    }  
  
    /**  
     * 执行单个步骤  
     *  
     * @return 步骤执行结果  
     */  
    public abstract String step();  
  
    /**  
     * 清理资源  
     */  
    protected void cleanup() {  
        // 子类可以重写此方法来清理资源  
    }  
}
```

上述代码中，我们要注意 3 点：

1. 包含 chatClient 属性，由调用方传入具体调用大模型的对象，而不是写死使用的大模型，更灵活
2. 包含 messageList 属性，用于维护消息上下文列表
3. 通过 state 属性来控制智能体的执行流程

#### 2、开发 ReActAgent 类

参考 OpenManus 的实现方式，继承自 BaseAgent，并且将 step 方法分解为 think 和 act 两个抽象方法。ReActAgent 的代码如下：

```java
/**  
 * ReAct (Reasoning and Acting) 模式的代理抽象类  
 * 实现了思考-行动的循环模式  
 */  
@EqualsAndHashCode(callSuper = true)  
@Data  
public abstract class ReActAgent extends BaseAgent {  
  
    /**  
     * 处理当前状态并决定下一步行动  
     *  
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行  
     */  
    public abstract boolean think();  
  
    /**  
     * 执行决定的行动  
     *  
     * @return 行动执行结果  
     */  
    public abstract String act();  
  
    /**  
     * 执行单个步骤：思考和行动  
     *  
     * @return 步骤执行结果  
     */  
    @Override  
    public String step() {  
        try {  
            boolean shouldAct = think();  
            if (!shouldAct) {  
                return "思考完成 - 无需行动";  
            }  
            return act();  
        } catch (Exception e) {  
            // 记录异常日志  
            e.printStackTrace();  
            return "步骤执行失败: " + e.getMessage();  
        }  
    }  
}
```

#### 3、开发 ToolCallAgent 类

ToolCallAgent 负责实现工具调用能力，继承自 ReActAgent，具体实现了 think 和 act 两个抽象方法。

我们有 3 种方案来实现 ToolCallAgent：

1）基于 Spring AI 的工具调用能力，手动控制工具执行。

其实 Spring 的 ChatClient 已经支持选择工具进行调用（内部完成了 think、act、observe），但这里我们要自主实现，可以使用 Spring AI 提供的 [手动控制工具执行](https://docs.spring.io/spring-ai/reference/api/tools.html#_user_controlled_tool_execution)。

2）基于 Spring AI 的工具调用能力，简化调用流程。

由于 Spring AI 完全托管了工具调用，我们可以直接把所有工具调用的代码作为 think 方法，而 act 方法不定义任何动作。

3）自主实现工具调用能力。

也就是工具调用章节提到的实现原理：自己写 Prompt，引导 AI 回复想要调用的工具列表和调用参数，然后再执行工具并将结果返送给 AI 再次执行。

使用哪种方案呢？

如果是为了学习 ReAct 模式，让流程更清晰，推荐第一种；如果只是为了快速实现，推荐第二种；不建议采用第三种方案，过于原生，开发成本高。

下面我们采用第一种方案实现 ToolCallAgent，先定义所需的属性和构造方法：

```java
/**  
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类  
 */  
@EqualsAndHashCode(callSuper = true)  
@Data  
@Slf4j  
public class ToolCallAgent extends ReActAgent {  
  
    // 可用的工具  
    private final ToolCallback[] availableTools;  
  
    // 保存了工具调用信息的响应  
    private ChatResponse toolCallChatResponse;  
  
    // 工具调用管理者  
    private final ToolCallingManager toolCallingManager;  
  
    // 禁用内置的工具调用机制，自己维护上下文  
    private final ChatOptions chatOptions;  
  
    public ToolCallAgent(ToolCallback[] availableTools) {  
        super();  
        this.availableTools = availableTools;  
        this.toolCallingManager = ToolCallingManager.builder().build();  
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文  
        this.chatOptions = DashScopeChatOptions.builder()  
                .withProxyToolCalls(true)  
                .build();  
    }  
}
```

注意，在上述代码中，我们通过将 DashScopeChatOptions 的 withProxyToolCalls 选项设置为 true，来禁止 Spring AI 托管工具调用，而是我们自主实现。

![img](https://pic.code-nav.cn/course_picture/1608440217629360130/EGJoiTueixnROgjz.webp)

虽然官方提供的示例代码是设置 internalToolExecutionEnabled 为 false 来禁用 Spring AI 托管工具调用，但是由于我们使用的是阿里的 DashScopeChatModel 大模型客户端，如果按照下面的方式，会直接导致工具调用失效！

```java
ChatOptions chatOptions = ToolCallingChatOptions.builder()  
    .toolCallbacks(new CustomerTools())  
    .internalToolExecutionEnabled(false)  
    .build();
```

下面我们实现 think 方法，传入工具列表并调用大模型，得到需要调用的工具列表：

```java
/**  
 * 处理当前状态并决定下一步行动  
 *  
 * @return 是否需要执行行动  
 */  
@Override  
public boolean think() {  
    if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {  
        UserMessage userMessage = new UserMessage(getNextStepPrompt());  
        getMessageList().add(userMessage);  
    }  
    List<Message> messageList = getMessageList();  
    Prompt prompt = new Prompt(messageList, chatOptions);  
    try {  
        // 获取带工具选项的响应  
        ChatResponse chatResponse = getChatClient().prompt(prompt)  
                .system(getSystemPrompt())  
                .tools(availableTools)  
                .call()  
                .chatResponse();  
        // 记录响应，用于 Act  
        this.toolCallChatResponse = chatResponse;  
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();  
        // 输出提示信息  
        String result = assistantMessage.getText();  
        List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();  
        log.info(getName() + "的思考: " + result);  
        log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");  
        String toolCallInfo = toolCallList.stream()  
                .map(toolCall -> String.format("工具名称：%s，参数：%s",  
                        toolCall.name(),  
                        toolCall.arguments())  
                )  
                .collect(Collectors.joining("\n"));  
        log.info(toolCallInfo);  
        if (toolCallList.isEmpty()) {  
            // 只有不调用工具时，才记录助手消息  
            getMessageList().add(assistantMessage);  
            return false;  
        } else {  
            // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录  
            return true;  
        }  
    } catch (Exception e) {  
        log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());  
        getMessageList().add(  
                new AssistantMessage("处理时遇到错误: " + e.getMessage()));  
        return false;  
    }  
}
```

最后实现 act 方法，执行工具调用列表，得到返回结果，并将工具的响应添加到消息列表中：

```java
/**  
 * 执行工具调用并处理结果  
 *  
 * @return 执行结果  
 */  
@Override  
public String act() {  
    if (!toolCallChatResponse.hasToolCalls()) {  
        return "没有工具调用";  
    }  
    // 调用工具  
    Prompt prompt = new Prompt(getMessageList(), chatOptions);  
    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);  
    // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果  
    setMessageList(toolExecutionResult.conversationHistory());  
    // 当前工具调用的结果  
    ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());  
    String results = toolResponseMessage.getResponses().stream()  
            .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())  
            .collect(Collectors.joining("\n"));  
    log.info(results);  
    return results;  
}
```

注意维护消息上下文，不要重复添加了消息，因为 `toolExecutionResult.conversationHistory()` 方法已经包含了助手消息和工具调用返回的结果。

可以带看下 toolCallingManager.executeToolCalls 的源码，会自动追加助手消息到上下文中：

![img](https://pic.code-nav.cn/course_picture/1608440217629360130/5LRrBoyPvs2kAJny.webp)![img](https://pic.code-nav.cn/course_picture/1608440217629360130/FlxqTqefcho72bGH.webp)

千万别忘了，我们还需要定义一个终止工具，让智能体可以自行决定任务结束。

1）在 tools 包下新建 TerminateTool：

```java
public class TerminateTool {  
  
    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)  
    public String doTerminate() {  
        return "任务结束";  
    }  
}
```

2）修改 ToolRegistration，注册中止工具：

```java
TerminateTool terminateTool = new TerminateTool();  
  
return ToolCallbacks.from(  
        fileOperationTool,  
        webSearchTool,  
        webScrapingTool,  
        resourceDownloadTool,  
        terminalOperationTool,  
        pdfGenerationTool,  
        terminateTool  
);
```

3）完善 act 方法，当调用了终止工具时，修改 agent 的状态为 “已结束”：

```java
// 当前工具调用的结果  
ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());  
String results = toolResponseMessage.getResponses().stream()  
        .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())  
        .collect(Collectors.joining("\n"));  
// 判断是否调用了终止工具  
boolean terminateToolCalled = toolResponseMessage.getResponses().stream()  
        .anyMatch(response -> "doTerminate".equals(response.name()));  
if (terminateToolCalled) {  
    setState(AgentState.FINISHED);  
}  
log.info(results);  
return results;
```

#### 4、开发 YuManus 类

YuManus 是可以直接提供给其他方法调用的 AI 超级智能体实例，继承自 ToolCallAgent，需要给智能体设置各种参数，比如对话客户端 chatClient、工具调用列表等。

代码如下：

```java
@Component  
public class YuManus extends ToolCallAgent {  
  
    public YuManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {  
        super(allTools);  
        this.setName("yuManus");  
        String SYSTEM_PROMPT = """  
                You are YuManus, an all-capable AI assistant, aimed at solving any task presented by the user.  
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.  
                """;  
        this.setSystemPrompt(SYSTEM_PROMPT);  
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;  
        this.setNextStepPrompt(NEXT_STEP_PROMPT);  
        this.setMaxSteps(20);  
        // 初始化客户端  
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)  
                .defaultAdvisors(new MyLoggerAdvisor())  
                .build();  
        this.setChatClient(chatClient);  
    }  
}
```

### 测试智能体

编写单元测试类，让超级智能体完成一个较为复杂的任务：

```java
@SpringBootTest  
class YuManusTest {  
  
    @Resource  
    private YuManus yuManus;  
  
    @Test  
    void run() {  
        String userPrompt = """  
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点，  
                并结合一些网络图片，制定一份详细的约会计划，  
                并以 PDF 格式输出""";  
        String answer = yuManus.run(userPrompt);  
        Assertions.assertNotNull(answer);  
    }  
}
```

以 Debug 模式运行，能够查看到 AI 每一步的思考过程：

![img](https://pic.code-nav.cn/course_picture/1608440217629360130/WLIrziReAJr9zBVA.webp)

执行完成后，可以看到每一步的执行结果，并且可以在本地查看到已下载的图片和生成的 PDF。

![img](https://pic.code-nav.cn/course_picture/1608440217629360130/TOhzfpBAWsewYqJy.webp)

注意，由于 AI 是存在随机性的，执行的步骤数也不固定。经过测试大家会发现，拥有自主规划能力的超级智能体虽然能够完成复杂的任务，但缺点是 **非常浪费 tokens，并且可能陷入无限循环。** 所以还是要按需使用。

感兴趣的同学可以参考 OpenManus 的实现来进一步优化超级智能体，比如防止陷入无限循环、能够和用户交互等。
