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
              :width="60"
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
                type="default"
                size="small"
                @click="showEditModal(record)"
              >
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>

              <a-button
                type="default"
                size="small"
                @click="showResetPasswordModal(record)"
                class="reset-btn"
              >
                <template #icon>
                  <LockOutlined />
                </template>
                重置密码
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

    <!-- 编辑用户模态框 -->
    <a-modal
      v-model:visible="editVisible"
      title="编辑用户信息"
      @ok="handleEditSubmit"
      :ok-text="'提交'"
      :cancel-text="'取消'"
      :keyboard="false"
      :mask-closable="false"
      class="edit-modal"
    >
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户账号">
          <a-input v-model:value="editForm.userAccount" disabled />
        </a-form-item>
        <a-form-item label="用户昵称">
          <a-input v-model:value="editForm.userName" placeholder="请输入用户昵称" />
        </a-form-item>
        <a-form-item label="用户角色">
          <a-select v-model:value="editForm.userRole" placeholder="请选择用户角色">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            placeholder="请输入用户简介..."
            :rows="4"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码模态框 -->
    <a-modal
      v-model:visible="passwordVisible"
      title="重置用户密码"
      @ok="handlePasswordSubmit"
      class="password-modal">
      <br><br>
      <a-alert
        message="重置后密码将更改为:  zmrq@[账户]"
        type="info"
        show-icon
        style="margin-bottom: 20px;"
      />
      <br>
      <p>确定要重置用户 <strong>{{ passwordForm.userName }}</strong> 的密码吗？</p>
    </a-modal>
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
  ReloadOutlined,
  LockOutlined
} from '@ant-design/icons-vue'
import {
  deleteUser,
  listUserVoByPage,
  resetUserPassword,
  updateUser,
  updateUserPassword
} from '@/api/userController'
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
    width: 220,
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

// 模态框状态
const editVisible = ref(false)
const passwordVisible = ref(false)

// 编辑表单
const editForm = reactive({
  id: '',
  userAccount: '',
  userName: '',
  userRole: '',
  userProfile: ''
})

// 密码表单
const passwordForm = reactive({
  id: '',
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

// 显示编辑模态框
const showEditModal = (user: API.UserVO) => {
  Object.assign(editForm, {
    id: user.id,
    userAccount: user.userAccount,
    userName: user.userName,
    userRole: user.userRole,
    userProfile: user.userProfile
  })
  editVisible.value = true
}

// 提交编辑信息
const handleEditSubmit = async () => {
  try {
    const res = await updateUser(editForm)
    if (res.data.code === 0) {
      message.success('用户信息更新成功')
      editVisible.value = false
      await fetchData()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新用户信息失败：', error)
    message.error('更新失败')
  }
}

// 显示重置密码模态框
const showResetPasswordModal = (user: API.UserVO) => {
  Object.assign(passwordForm, {
    id: user.id,
    userName: user.userName
  })
  passwordVisible.value = true
}

// 提交重置密码
const handlePasswordSubmit = async () => {
  try {
    const res = await resetUserPassword({ userId: passwordForm.id })
    if (res.data.code === 0) {
      message.success(`密码重置成功，新密码为：zmrq@${passwordForm.userName}`)
      passwordVisible.value = false
    } else {
      message.error('重置失败：' + res.data.message)
    }
  } catch (error) {
    console.error('重置密码失败：', error)
    message.error('重置失败:' + error)
  }
}

// 删除用户
const doDelete = async (id: string | undefined) => {
  if (!id) return

  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('用户删除成功')
      await fetchData()
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
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  min-height: calc(100vh - 48px);
  position: relative;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #333333;

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
    font-family: 'Comic Neue', cursive;
    font-size: 2.8rem;
    font-weight: 700;
    color: #2c3e50;
    margin-bottom: 8px;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  }

  p {
    font-size: 1.2rem;
    color: #7f8c8d;
    font-weight: 400;
    font-family: 'Comic Neue', cursive;
    max-width: 600px;
    margin: 0 auto;
  }
}

.search-panel, .user-table {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
    transform: translateY(-3px);
  }

  h2 {
    font-size: 1.4rem;
    color: #2c3e50;
    margin-bottom: 15px;
    padding-bottom: 10px;
    border-bottom: 2px solid #f0f0f0;
    font-family: 'Comic Neue', cursive;
    font-weight: 600;
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
      border-radius: 12px;
      font-family: 'Nunito', sans-serif;
      font-weight: 600;
      font-size: 1rem;
      transition: all 0.3s ease;
      align-items: center;
      gap: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border: none;

      &:first-child {
        background: linear-gradient(135deg, #a8e6cf 0%, #dcedc1 100%);
        color: #2c3e50;

        &:hover {
          transform: translateY(-3px);
          box-shadow: 0 6px 16px rgba(168, 230, 207, 0.4);
        }
      }

      &:last-child {
        background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
        color: white;

        &:hover {
          transform: translateY(-3px);
          box-shadow: 0 6px 16px rgba(116, 235, 213, 0.4);
        }
      }
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

    .reset-btn {
      color: #fda27e;
      border-color: #fda684;

      &:hover {
        color: #ff9c6e;
        border-color: #ff9c6e;
        background: rgba(255, 122, 69, 0.1);
      }
    }
  }
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #2c3e50;
  font-family: 'Comic Neue', cursive;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  transition: background 0.3s;
}

:deep(.ant-table-row:hover td) {
  background: rgba(168, 230, 207, 0.1) !important;
}

:deep(.ant-pagination) {
  display: flex;
  justify-content: center;
  margin-top: 25px;
  padding: 10px 0;
  background: #f8f9fa;
  border-radius: 12px;
}

// 模态框样式
:deep(.edit-modal), :deep(.password-modal) {
  .ant-modal {
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 15px 40px rgba(92, 74, 72, 0.2);
  }

  .ant-modal-content {
    background: linear-gradient(135deg, #fffaf0 0%, #fdf6e3 100%);
    border: 1px solid rgba(198, 180, 165, 0.4);
  }

  .ant-modal-header {
    background: transparent;
    border-bottom: 1px solid #f0e6d9;
    padding: 22px 24px;
    border-radius: 20px 20px 0 0;

    .ant-modal-title {
      color: #5c4a48;
      font-size: 1.4rem;
      font-weight: 600;
      text-align: center;
    }
  }

  .ant-modal-body {
    padding: 28px 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid #f0e6d9;
    padding: 18px 24px;
    text-align: center;

    .ant-btn {
      border-radius: 10px;
      padding: 0 22px;
      height: 38px;
      font-weight: 500;
      transition: all 0.2s ease;

      &.ant-btn-default {
        border-color: #d9d9d9;
        color: #5c4a48;

        &:hover {
          border-color: #c6a08a;
          color: #c6a08a;
        }
      }

      &.ant-btn-primary {
        background: #c6a08a;
        border-color: #c6a08a;
        color: white;

        &:hover {
          background: #b8917a;
          border-color: #b8917a;
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(198, 160, 138, 0.3);
        }
      }
    }
  }

  .ant-form {
    .ant-form-item {
      margin-bottom: 22px;

      .ant-form-item-label {
        label {
          color: #5c4a48;
          font-weight: 600;
          font-size: 0.95rem;
        }
      }

      .ant-input, .ant-input-textarea, .ant-select {
        border-radius: 10px;
        border: 1px solid #d9d9d9;
        padding: 10px 14px;
        font-size: 0.95rem;
        transition: all 0.2s ease;

        &:focus, &:focus-within {
          border-color: #c6a08a;
          box-shadow: 0 0 0 2px rgba(198, 160, 138, 0.2);
        }

        &::placeholder {
          color: #a8a8a8;
        }
      }

      .ant-input-textarea {
        min-height: 100px;
        resize: vertical;
      }
    }
  }
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

  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>
