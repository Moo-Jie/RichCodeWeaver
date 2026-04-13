<template>
  <div id="userManagePage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>用户管理中心</h1>
      <p>管理系统用户账户</p>
    </div>

    <AdminBackToDashboardButton />

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选用户</h2>
      <a-form :model="searchParams" layout="inline" @finish="doSearch">
        <a-form-item class="search-item" label="账号">
          <a-input
            v-model:value="searchParams.userAccount"
            allow-clear
            aria-label="搜索用户账号"
            placeholder="输入用户账号"
            suffix-icon="search"
          />
        </a-form-item>

        <a-form-item class="search-item" label="用户名">
          <a-input
            v-model:value="searchParams.userName"
            allow-clear
            aria-label="搜索用户名"
            placeholder="输入用户名"
            suffix-icon="user"
          />
        </a-form-item>

        <a-form-item class="search-item" label="用户角色">
          <a-select
            v-model:value="searchParams.userRole"
            aria-label="选择用户角色"
            placeholder="全部角色"
            style="width: 120px"
          >
            <a-select-option value="">全部角色</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item class="search-actions">
          <a-button aria-label="搜索用户" html-type="submit" type="primary">
            <template #icon>
              <SearchOutlined />
            </template>
            搜索
          </a-button>
          &nbsp;
          <a-button aria-label="重置搜索条件" @click="resetSearch">
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
        :scroll="{ x: 1500 }"
        rowKey="id"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-image
              v-if="record.userAvatar"
              :fallback="fallbackImage"
              :height="60"
              :src="record.userAvatar"
              :width="60"
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
                :aria-label="`编辑用户 ${record.userName}`"
                size="small"
                type="default"
                @click="showEditModal(record)"
              >
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>

              <a-button
                :aria-label="`重置用户 ${record.userName} 的密码`"
                class="reset-btn"
                size="small"
                type="default"
                @click="showResetPasswordModal(record)"
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
                <a-button :aria-label="`删除用户 ${record.userName}`" danger size="small">
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
      :cancel-text="'取消'"
      :keyboard="false"
      :mask-closable="false"
      :ok-text="'提交'"
      class="edit-modal"
      title="编辑用户信息"
      @ok="handleEditSubmit"
    >
      <a-form :label-col="{ span: 6 }" :model="editForm" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户账号">
          <a-input v-model:value="editForm.userAccount" aria-label="用户账号（不可编辑）" disabled />
        </a-form-item>
        <a-form-item label="用户昵称">
          <a-input v-model:value="editForm.userName" aria-label="编辑用户昵称"
                   placeholder="请输入用户昵称" />
        </a-form-item>
        <a-form-item label="用户角色">
          <a-select v-model:value="editForm.userRole" aria-label="选择用户角色"
                    placeholder="请选择用户角色">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            :maxlength="200"
            :rows="4"
            aria-label="编辑用户个人简介"
            placeholder="请输入用户简介..."
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码模态框 -->
    <a-modal
      v-model:visible="passwordVisible"
      class="password-modal"
      title="重置用户密码"
      @ok="handlePasswordSubmit">
      <br><br>
      <a-alert
        message="重置后密码将更改为:  zmrq@[账户]"
        show-icon
        style="margin-bottom: 20px;"
        type="info"
      />
      <br>
      <p>确定要重置用户 <strong>{{ passwordForm.userName }}</strong> 的密码吗？</p>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {message} from 'ant-design-vue'
import {
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  EllipsisOutlined,
  LockOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import {deleteUser, listUserVoByPage, resetUserPassword, updateUser} from '@/api/userController'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'
import dayjs from 'dayjs'

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
  id: ''
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
    message.error('获取数据失败:' + res.data.message)
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
    message.error('更新失败:' + res.data.message)
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
    message.error('重置失败::' + res.data.message)
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
    message.error('删除操作失败:' + res.data.message)
  }
}

// 页面加载时请求数据
onMounted(() => {
  fetchData()
})
</script>

<style lang="less" scoped>
#userManagePage {
  padding: 32px;
  width: 100%;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.search-panel, .user-table {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  margin-bottom: 20px;
  overflow: hidden;
  transition: all 0.2s ease;

  &:hover {
    border-color: #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  h2 {
    font-size: 16px;
    color: #1a1a1a;
    margin-bottom: 16px;
    font-weight: 600;
  }
}

.search-panel {
  padding: 20px;

  .ant-form {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .search-item {
    flex: 1;
    min-width: 200px;
    margin-bottom: 0;
  }

  .search-actions {
    display: flex;
    gap: 8px;
    align-self: flex-end;

    .ant-btn {
      height: 36px;
      padding: 0 16px;
      border-radius: 10px;
      font-weight: 500;
      font-size: 14px;
      transition: all 0.2s ease;
      border: 1px solid #e5e5e5;

      &:first-child {
        background: #1a1a1a;
        color: #fff;
        border-color: #1a1a1a;

        &:hover {
          background: #333;
        }
      }

      &:last-child {
        background: #fff;
        color: #666;

        &:hover {
          background: #fafafa;
          border-color: #d0d0d0;
        }
      }
    }
  }
}


.user-table {
  padding: 20px;

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 {
      margin: 0;
    }

    .table-tips {
      color: #999;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .avatar-img {
    border-radius: 8px;
    overflow: hidden;
  }

  .no-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    background: #fafafa;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    color: #bbb;
    font-size: 12px;

    span {
      margin-left: 4px;
    }
  }

  .profile-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
    color: #666;
  }

  .time-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #666;
  }

  .action-buttons {
    display: flex;
    gap: 8px;

    .ant-btn {
      font-size: 13px;
      padding: 4px 12px;
      border-radius: 8px;
      height: 30px;
      border: 1px solid #e5e5e5;
      transition: all 0.2s ease;

      &:hover {
        border-color: #d0d0d0;
        background: #fafafa;
      }
    }

    .reset-btn {
      color: #ff6b6b;
      border-color: #ff6b6b;

      &:hover {
        background: rgba(255, 107, 107, 0.1);
      }
    }
  }
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  font-size: 14px;
  color: #666;
}

:deep(.ant-table-row:hover td) {
  background: #fafafa !important;
}

:deep(.ant-pagination) {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

// 模态框样式
:deep(.edit-modal), :deep(.password-modal) {
  .ant-modal {
    border-radius: 14px;
  }

  .ant-modal-content {
    background: #fff;
    border: 1px solid #f0f0f0;
  }

  .ant-modal-header {
    background: #fafafa;
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 20px;

    .ant-modal-title {
      color: #1a1a1a;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .ant-modal-body {
    padding: 20px;
  }

  .ant-modal-footer {
    border-top: 1px solid #f0f0f0;
    padding: 12px 20px;

    .ant-btn {
      border-radius: 8px;
      padding: 0 16px;
      height: 36px;
      font-weight: 500;
      font-size: 14px;

      &.ant-btn-default {
        border-color: #e5e5e5;
        color: #666;

        &:hover {
          background: #fafafa;
          border-color: #d0d0d0;
        }
      }

      &.ant-btn-primary {
        background: #1a1a1a;
        border-color: #1a1a1a;
        color: white;

        &:hover {
          background: #333;
        }
      }
    }
  }

  .ant-form {
    .ant-form-item {
      margin-bottom: 16px;

      .ant-form-item-label {
        label {
          color: #1a1a1a;
          font-weight: 500;
          font-size: 14px;
        }
      }

      .ant-input, .ant-input-textarea, .ant-select {
        border-radius: 8px;
        border: 1px solid #e5e5e5;
        font-size: 14px;

        &:focus, &:focus-within {
          border-color: #1a1a1a;
        }

        &::placeholder {
          color: #bbb;
        }
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

/* Keyboard navigation focus indicators */
:deep(.ant-input:focus),
:deep(.ant-input-focused),
:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-textarea:focus) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
  border-color: #4096ff;
}

:deep(.ant-btn:focus-visible) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

:deep(.ant-table-row:focus-within) {
  outline: 2px solid #4096ff;
  outline-offset: -2px;
}

:deep(.ant-pagination-item:focus-visible),
:deep(.ant-pagination-prev:focus-visible),
:deep(.ant-pagination-next:focus-visible) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

.action-buttons {
  .ant-btn:focus-visible {
    outline: 3px solid #4096ff;
    outline-offset: 2px;
  }
}

@media (max-width: 768px) {
  #userManagePage {
    padding: 16px;
  }

  .page-header {
    h1 {
      font-size: 2rem;
    }

    p {
      font-size: 1rem;
    }
  }

  .search-panel, .user-table {
    padding: 18px;
    margin-bottom: 20px;

    h2 {
      font-size: 1.2rem;
    }
  }

  .table-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 10px;

    h2 {
      font-size: 1.2rem;
    }

    .table-tips {
      font-size: 0.85rem;
      flex-direction: column;
      align-items: flex-start;
      gap: 5px;
    }
  }

  .action-buttons {
    flex-wrap: wrap;
    gap: 6px;

    .ant-btn {
      font-size: 0.85rem;
      padding: 4px 10px;
      height: 30px;
    }
  }

  .search-actions {
    .ant-btn {
      height: 36px;
      padding: 0 14px;
      font-size: 0.9rem;
    }
  }

  :deep(.ant-table) {
    font-size: 0.9rem;
  }

  :deep(.ant-modal) {
    max-width: calc(100vw - 32px);
    margin: 16px auto;
  }
}
</style>
