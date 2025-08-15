<template>
  <div id="userManagePage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>用户管理中心</h1>
      <p>管理系统用户账户</p>
    </div>

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选用户</h2>
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号" class="search-item">
          <a-input
            v-model:value="searchParams.userAccount"
            placeholder="输入用户账号"
            suffix-icon="search"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="用户名" class="search-item">
          <a-input
            v-model:value="searchParams.userName"
            placeholder="输入用户名"
            suffix-icon="user"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="用户角色" class="search-item">
          <a-select
            v-model:value="searchParams.userRole"
            placeholder="全部角色"
            style="width: 120px"
          >
            <a-select-option value="">全部角色</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item class="search-actions">
          <a-button type="primary" html-type="submit">
            <template #icon>
              <SearchOutlined />
            </template>
            搜索
          </a-button>
          &nbsp;
          <a-button @click="resetSearch">
            <template #icon>
              <ReloadOutlined />
            </template>
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 用户列表表格 -->
    <a-card class="user-table">
      <div class="table-header">
        <h2>用户列表</h2>
        <span class="table-tips">
          共 {{ pagination.total }} 位用户
          <a-tag color="gold">管理员以绿色标签显示</a-tag>
        </span>
      </div>

      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="doTableChange"
        :scroll="{ x: 1500 }"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-image
              v-if="record.userAvatar"
              :src="record.userAvatar"
              :width="80"
              :height="60"
              :fallback="fallbackImage"
              class="avatar-img"
            />
            <div v-else class="no-avatar">
              <UserOutlined />
              <span>无头像</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'userProfile'">
            <a-tooltip :title="record.userProfile">
              <div class="profile-text">
                <EllipsisOutlined />
                {{ record.userProfile || '无简介' }}
              </div>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'green' : 'blue'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="time-cell">
              <CalendarOutlined />
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}
            </div>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space class="action-buttons">
              <a-button
                type="primary"
                size="small"
                @click="editUser(record)"
              >
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>

              <a-popconfirm
                title="确定删除此用户吗？操作不可恢复"
                @confirm="doDelete(record.id)"
              >
                <a-button danger size="small">
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  UserOutlined,
  CalendarOutlined,
  EllipsisOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { deleteUser, listUserVoByPage } from '@/api/userController'
import dayjs from 'dayjs'

const router = useRouter()

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
    title: '账号',
    dataIndex: 'userAccount',
    width: 120,
    ellipsis: true
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 120,
    ellipsis: true
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 100
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    width: 200,
    ellipsis: true
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
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
    width: 180,
    fixed: 'right'
  }
]

// 用户数据
const data = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listUserVoByPage({
      ...searchParams
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取用户数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取用户数据失败：', error)
    message.error('获取数据失败')
  }
}

// 默认头像图像
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#f5f5f5"/><circle cx="200" cy="120" r="60" fill="%23999"/><circle cx="200" cy="320" r="120" fill="%23999"/></svg>'

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
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.userRole = ''
  searchParams.pageNum = 1
  fetchData()
}

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索用户
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 编辑用户
const editUser = (user: API.UserVO) => {
  message.info('编辑用户: ' + user.userName)
  // router.push(`/user/edit/${user.id}`)
}

// 删除用户
const doDelete = async (id: string | undefined) => {
  if (!id) return

  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('用户删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('用户删除失败：', error)
    message.error('删除操作失败')
  }
}

// 页面加载时请求数据
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
#userManagePage {
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

.search-panel, .user-table {
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
    min-width: 220px;
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
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }
}

.user-table {
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

  .avatar-img {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
  }

  .no-avatar {
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

  .profile-text {
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

  .time-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.92rem;
  }

  .action-buttons {
    display: flex;
    gap: 8px;

    .ant-btn {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 0.9rem;
      padding: 4px 12px;
      border-radius: 6px;
      height: 32px;
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
</style>
