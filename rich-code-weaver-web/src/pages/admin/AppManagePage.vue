<template>
  <div id="appManagePage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>应用管理中心</h1>
      <p>管理应用作品</p>
    </div>

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选应用</h2>
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用名称" class="search-item">
          <a-input
            v-model:value="searchParams.appName"
            placeholder="输入应用名称关键词"
            suffix-icon="search"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="提示关键词" class="search-item">
          <a-input
            v-model:value="searchParams.initPrompt"
            placeholder="输入提示关键词"
            suffix-icon="search"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="创建者" class="search-item">
          <a-input
            v-model:value="searchParams.userId"
            placeholder="输入用户ID"
            suffix-icon="user"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="生成类型" class="search-item">
          <a-select
            v-model:value="searchParams.codeGenType"
            placeholder="全部生成类型"
            style="width: 180px"
          >
            <a-select-option>全部类型</a-select-option>
            <a-select-option
              v-for="option in CODE_GEN_TYPE_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="部署密钥">
          <a-input
            v-model:value="searchParams.initPrompt"
            placeholder="输入部署密钥"
            suffix-icon="deployKey"
            allow-clear
          />
        </a-form-item>

        <a-form-item class="search-actions">
          <a-button type="default" html-type="submit">搜索</a-button>
          &nbsp;
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 应用列表表格 -->
    <a-card class="app-table">
      <div class="table-header">
        <h2>应用列表</h2>
        <span class="table-tips">
          共 {{ pagination.total }} 个应用 |
          <a-tag color="gold" class="featured-tag">金色星标为星选应用</a-tag>
        </span>
      </div>

      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="doTableChange"
        :scroll="{ x: 1200 }"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'cover'">
            <a-image
              v-if="record.cover"
              :src="record.cover"
              :width="80"
              :height="60"
              :fallback="fallbackImage"
              class="app-cover"
            />
            <div v-else class="no-cover">
              <picture-outlined />
              <span>未上传封面</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'initPrompt'">
            <a-tooltip :title="record.initPrompt">
              <div class="prompt-text">
                <ellipsis-outlined />
                {{ record.initPrompt }}
              </div>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'codeGenType'">
            <a-tag :color="getTypeColor(record.codeGenType)">
              {{ record.codeGenType === 'single_html' ? '单文件结构' : record.codeGenType === 'multi_file' ? '多文件结构' : record.codeGenType === 'vue_project' ? 'VUE 项目工程' : formatCodeGenType(record.codeGenType)
              }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'deployKey'">
            <div @click="toggleDeployKey(record.id)" class="deploy-key-cell">
              <span v-if="showDeployKey[record.id]">{{ record.deployKey }}</span>
              <span v-else>*******</span>
              <component
                :is="showDeployKey[record.id] ? EyeOutlined : EyeInvisibleOutlined"
                class="eye-icon"
                :style="{ marginLeft: '5px', cursor: 'pointer' }"
              />
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'priority'">
            <a-tag v-if="record.priority === 99" color="gold">
              <star-filled />
              星选应用
            </a-tag>
            <span v-else>普通应用</span>
          </template>

          <template v-else-if="column.dataIndex === 'deployedTime'">
            <div v-if="record.deployedTime" class="time-cell">
              <calendar-outlined />
              {{ formatTime(record.deployedTime) }}
            </div>
            <div v-else class="text-gray">
              <clock-circle-outlined />
              <span>未部署</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="time-cell">
              <calendar-outlined />
              {{ formatTime(record.createTime) }}
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'user'">
            <div class="user-cell">
              <user-outlined />
              <UserInfo :user="record.user" size="small" />
            </div>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space class="action-buttons">
              <!-- 进入应用按钮 -->
              <a-button
                type="default"
                size="small"
                @click="enterApp(record)"
              >
                <template #icon>
                  <ArrowRightOutlined />
                </template>
                进入
              </a-button>
              <a-button
                type="default"
                size="small"
                @click="editApp(record)"
              >
                编辑
              </a-button>
              <!-- 信息卡片按钮 -->
              <a-button
                type="default"
                size="small"
                @click="showAppDetail(record)"
              >
                <template #icon>
                  <InfoCircleOutlined />
                </template>
                详情
              </a-button>

              <a-button
                type="default"
                size="small"
                @click="toggleFeatured(record)"
                :class="{ 'featured-btn': record.priority === 99 }"
              >
                <template v-if="record.priority === 99">
                  <star-filled />
                  取消星选
                </template>
                <template v-else>
                  <star-outlined />
                  设为星选
                </template>
              </a-button>

              <a-popconfirm
                title="确定要删除这个应用吗？此操作不可恢复"
                @confirm="deleteApp(record.id)"
              >
                <a-button danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 应用详情弹窗 -->
    <AppInfo
      v-model:open="appDetailVisible"
      :app="currentApp"
      :show-actions="false"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PictureOutlined,
  EllipsisOutlined,
  StarFilled,
  StarOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  UserOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  ArrowRightOutlined,
  EyeInvisibleOutlined
} from '@ant-design/icons-vue'
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/enums/codeGenTypes.ts'
import { formatTime } from '@/utils/timeUtil.ts'
import UserInfo from '@/components/UserInfo.vue'
import AppInfo from '@/components/AppInfo.vue'

const router = useRouter()
// 查看应用详情
const appDetailVisible = ref(false)
const currentApp = ref<API.AppVO | null>(null)

const showAppDetail = (app: API.AppVO) => {
  currentApp.value = app
  appDetailVisible.value = true
}

// 进入应用
const enterApp = (app: API.AppVO) => {
  if (app.id) {
    router.push(`/app/chat/${app.id}`)
  }
}

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    fixed: 'left',
    sorter: true
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 150,
    ellipsis: true
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    width: 200,
    ellipsis: true
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 120
  },
  {
    title: '推送等级',
    dataIndex: 'priority',
    width: 100
  },
  {
    title: '部署密钥',
    dataIndex: 'deployKey',
    width: 120
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 160
  },
  {
    title: '创建者',
    dataIndex: 'user',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
    sorter: true
  },
  {
    title: '操作',
    key: 'action',
    width: 230,
    fixed: 'right'
  }
]

// 应用数据
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  }
}

// 部署密钥显示状态管理
const showDeployKey = ref<Record<number, boolean>>({})

// 切换部署密钥显示状态
const toggleDeployKey = (id: number) => {
  showDeployKey.value[id] = !showDeployKey.value[id]
}

// 页面加载时请求数据
onMounted(() => {
  fetchData()
})

// 默认封面图像
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#f5f5f5"/><text x="50%" y="50%" font-family="Arial" font-size="20" text-anchor="middle" fill="#999">未提供封面</text></svg>'

// 根据生成类型获取标签颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'react': '#61dafb',
    'vue': '#42b883',
    'angular': '#dd0031',
    'html': '#e34c26',
    'nodejs': '#68a063',
    'flutter': '#04599C',
    'swift': '#ff2d55'
  }
  return colors[type] || 'blue'
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '30', '50'],
    showTotal: (total: number) => `共 ${total} 条记录`
  }
})

// 重置搜索
const resetSearch = () => {
  searchParams.appName = ''
  searchParams.userId = ''
  searchParams.codeGenType = ''
  searchParams.pageNum = 1
  fetchData()
}

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索应用
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

// 切换星选状态
const toggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return

  const newPriority = app.priority === 99 ? 0 : 99

  try {
    const res = await updateAppByAdmin({
      id: app.id,
      priority: newPriority
    })

    if (res.data.code === 0) {
      message.success(newPriority === 99 ? '已设为星选应用' : '已取消星选')
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    console.error('操作失败：', error)
    message.error('操作失败')
  }
}

// 删除应用
const deleteApp = async (id: number | undefined) => {
  if (!id) return

  try {
    const res = await deleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}
</script>

<style scoped lang="less">
#appManagePage {
  padding: 24px;
  background: linear-gradient(135deg, #fdfcf9 0%, #f7f5f2 100%);
  min-height: calc(100vh - 48px);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" opacity="0.05"><rect x="40" y="10" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="10" y="40" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="70" y="40" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="40" y="70" width="20" height="20" fill="%23666" rx="4" ry="4"/></svg>');
    background-size: 200px;
    pointer-events: none;
    z-index: 0;
  }
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 10px 0;

  h1 {
    font-size: 2.8rem;
    font-weight: 600;
    color: #5c4a48;
    margin-bottom: 8px;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  }

  p {
    font-size: 1.2rem;
    color: #7a787c;
    max-width: 600px;
    margin: 0 auto;
  }
}

.search-panel, .app-table {
  background: rgba(255, 253, 248, 0.92);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(155, 140, 125, 0.1);
  border: 1px solid rgba(198, 180, 165, 0.15);
  margin-bottom: 30px;
  overflow: hidden;
  position: relative;
  z-index: 1;

  h2 {
    font-size: 1.4rem;
    color: #5c4a48;
    margin-bottom: 15px;
    padding-bottom: 10px;
    border-bottom: 1px solid #eee;
  }
}

.search-panel {
  padding: 25px;

  .ant-form {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
  }

  .search-item {
    flex: 1;
    min-width: 280px;
    margin-bottom: 0;
  }

  .search-actions {
    display: flex;
    gap: 10px;
    align-self: flex-end;
    padding-left: 10px;

    .ant-btn {
      height: 40px;
      padding: 0 18px;
      border-radius: 8px;
    }
  }
}

.app-table {
  padding: 25px;

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;

    h2 {
      margin: 0;
      padding-bottom: 0;
      border: none;
    }

    .table-tips {
      color: #7a787c;
      font-size: 0.95rem;
      display: flex;
      align-items: center;
      gap: 8px;

      .featured-tag {
        margin-left: 5px;
        font-weight: 500;
      }
    }
  }

  .app-cover {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
  }

  .no-cover {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 60px;
    background: #f9f9f9;
    border: 1px dashed #ddd;
    border-radius: 8px;
    color: #999;
    font-size: 0.9rem;
    padding: 5px;

    span {
      margin-left: 5px;
    }
  }

  .prompt-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding: 5px 0;
    display: flex;
    align-items: center;

    .anticon {
      margin-right: 6px;
      color: #999;
    }
  }

  .time-cell, .user-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.92rem;
  }

  .text-gray {
    color: #999;
    display: flex;
    align-items: center;
    gap: 5px;
  }

  .action-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .ant-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 5px;
      font-size: 0.9rem;
      padding: 4px 12px;
      border-radius: 6px;
      height: 32px;
      width: 90px;
    }
  }

  .featured-btn {
    background: rgba(250, 173, 20, 0.1);
    color: #d48806;
    border-color: rgba(250, 173, 20, 0.3);

    &:hover {
      background: rgba(250, 173, 20, 0.15);
      border-color: rgba(250, 173, 20, 0.4);
    }
  }
}

:deep(.ant-table-thead > tr > th) {
  background: #f8f6f2;
  font-weight: 600;
  color: #5c4a48;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  transition: background 0.3s;
}

:deep(.ant-table-row:hover td) {
  background: rgba(198, 180, 165, 0.07) !important;
}

:deep(.ant-pagination) {
  display: flex;
  justify-content: center;
  margin-top: 25px;
  padding: 10px 0;
  background: #f9f7f4;
  border-radius: 8px;
}

@media (max-width: 992px) {
  .search-panel {
    .ant-form {
      flex-direction: column;
      gap: 18px;
    }

    .search-item, .search-actions {
      width: 100%;
      min-width: unset;
    }
  }

  .table-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 10px;

    .table-tips {
      align-self: flex-start;
    }
  }
}

.deploy-key-cell {
  cursor: pointer;
  display: flex;
  align-items: center;
}
</style>
