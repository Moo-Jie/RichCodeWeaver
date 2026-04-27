# Prometheus + Grafana 部署攻略

### 1. 启动应用服务

确保 `rich-code-weaver-generator` 服务已启动（默认端口 8101）。

### 2. 启动 Prometheus

使用项目根目录的配置文件启动 Prometheus：

```bash
# Windows
prometheus.exe --config.file=prometheus.yml

# Linux/Mac
./prometheus --config.file=prometheus.yml
```

访问 Prometheus Web UI：http://localhost:9090

### 3. 验证指标采集

在 Prometheus UI 中查询自定义指标：

```promql
# 查看所有 AI 模型请求
ai_model_requests_total

# 查看 Token 消耗
ai_model_tokens_total

# 查看响应时间
ai_model_response_duration_seconds
```

### 4. 启动 Grafana

```bash
# Windows
grafana-server.exe

# Linux/Mac
./grafana-server
```

访问 Grafana Web UI：http://localhost:3000（默认账号密码：admin/admin）

## 四、配置 Grafana 看板

### 1. 添加数据源

1. 登录 Grafana
2. 进入 Configuration -> Data Sources
3. 点击 "Add data source"
4. 选择 "Prometheus"
5. 配置 URL：http://localhost:9090
6. 点击 "Save & Test"

### 2. 创建看板

可以手动创建看板，或使用 AI 生成看板配置。