<template>
  <div :class="['team-card', { 'team-card--compact': compact }]">
    <div class="team-card-header">
      <div class="team-card-heading">
        <span class="team-card-title">创作者团队</span>
        <span class="team-card-subtitle">GitHub 风格成员视图，清晰展示主创与协作关系</span>
      </div>
      <div class="team-card-stat">
        <span class="team-card-count">{{ teamCount }}</span>
        <span class="team-card-count-label">成员</span>
      </div>
    </div>
    <div v-if="teamCount > 0 && !compact" class="team-summary">
      <span class="summary-pill">主创 {{ app?.user?.userName || '未设置' }}</span>
      <span class="summary-pill">协作者 {{ collaboratorCount }} 人</span>
    </div>
    <div class="team-list">
      <div v-for="member in teamMembers" :key="member.key" class="team-item">
        <button class="team-user team-user-button" type="button" @click="openMemberProfile(member)">
          <a-avatar :src="member.avatar" :size="avatarSize" class="team-avatar">
            {{ member.name.charAt(0) || 'U' }}
          </a-avatar>
          <div class="team-user-meta">
            <div class="team-user-topline">
              <span class="team-user-name">{{ member.name }}</span>
              <span v-if="!compact" class="team-user-id">{{ member.meta }}</span>
            </div>
            <span class="team-user-role">{{ compact ? member.shortDescription : member.description }}</span>
          </div>
        </button>
        <span :class="['team-badge', `team-badge--${member.badgeTone}`]">{{ member.badgeLabel }}</span>
      </div>
      <div v-if="teamCount === 0" class="team-empty">暂无团队信息</div>
    </div>
    <UserProfileModal
      v-model:open="profileOpen"
      :seed-avatar="activeProfile?.avatar"
      :seed-name="activeProfile?.name"
      :user-id="activeProfile?.userId"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import UserProfileModal from '@/components/UserProfileModal.vue'

type TeamMember = {
  key: string | number
  userId?: number
  name: string
  avatar?: string
  meta: string
  description: string
  shortDescription: string
  badgeLabel: string
  badgeTone: 'dark' | 'muted'
}

interface Props {
  app?: API.AppVO | null
  collaborators?: API.AppCollaboratorVO[]
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  app: null,
  collaborators: () => [],
  compact: false
})

const teamCount = computed(() => {
  return (props.app?.user ? 1 : 0) + (props.collaborators?.length || 0)
})

const collaboratorCount = computed(() => props.collaborators?.length || 0)

const avatarSize = computed(() => props.compact ? 32 : 36)

const profileOpen = ref(false)
const activeProfile = ref<TeamMember | null>(null)

const getRoleLabel = (role?: string) => role === 'viewer' ? 'Viewer' : 'Editor'

const getRoleDescription = (role?: string) => {
  if (role === 'viewer') return '协作成员 · 仅查看权限'
  return '协作成员 · 可编辑内容'
}

const getShortRoleDescription = (role?: string) => {
  if (role === 'viewer') return '仅查看权限'
  return '可编辑内容'
}

const openMemberProfile = (member: TeamMember) => {
  if (!member.userId) return
  activeProfile.value = member
  profileOpen.value = true
}

const teamMembers = computed<TeamMember[]>(() => {
  const members: TeamMember[] = []

  if (props.app?.user) {
    members.push({
      key: `owner-${props.app.user.id || 'unknown'}`,
      userId: props.app.user.id,
      name: props.app.user.userName || '未知用户',
      avatar: props.app.user.userAvatar,
      meta: props.app.user.id ? `ID ${props.app.user.id}` : 'Owner',
      description: '主创作者 · 数字产物拥有者',
      shortDescription: '主创作者',
      badgeLabel: 'Owner',
      badgeTone: 'dark'
    })
  }

  props.collaborators.forEach((member) => {
    members.push({
      key: member.id || member.userId || `collaborator-${members.length}`,
      userId: member.userId,
      name: member.userName || '未知用户',
      avatar: member.userAvatar,
      meta: member.userId ? `ID ${member.userId}` : 'Collaborator',
      description: getRoleDescription(member.role),
      shortDescription: getShortRoleDescription(member.role),
      badgeLabel: getRoleLabel(member.role),
      badgeTone: 'muted'
    })
  })

  return members
})
</script>

<style scoped>
.team-card {
  border: 1px solid #d0d7de;
  border-radius: 14px;
  background: #ffffff;
  padding: 14px;
  box-shadow: 0 1px 0 rgba(27, 31, 36, 0.04);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.team-card:hover {
  transform: translateY(-1px);
  border-color: #c7d2da;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
}

.team-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.team-card-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.team-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #24292f;
}

.team-card-subtitle {
  font-size: 11px;
  color: #656d76;
  line-height: 1.5;
}

.team-card-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid #d8dee4;
  background: #ffffff;
}

.team-card-count {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
  color: #24292f;
}

.team-card-count-label {
  font-size: 11px;
  color: #656d76;
  margin-top: 4px;
}

.team-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.summary-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #d8dee4;
  background: rgba(255, 255, 255, 0.88);
  color: #57606a;
  font-size: 11px;
  font-weight: 500;
}

.team-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.team-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #d8dee4;
  background: rgba(255, 255, 255, 0.92);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.team-item:hover {
  transform: translateY(-1px);
  border-color: #c7d2da;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.05);
}

.team-user {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.team-user-button {
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.team-user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.team-user-topline {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
}

.team-user-name {
  font-size: 13px;
  font-weight: 600;
  color: #24292f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-user-id {
  font-size: 11px;
  color: #8c959f;
}

.team-user-role {
  font-size: 11px;
  color: #656d76;
  line-height: 1.4;
}

.team-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.team-badge--dark {
  background: #24292f;
  border-color: #24292f;
  color: #ffffff;
}

.team-badge--muted {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

.team-empty {
  font-size: 12px;
  color: #656d76;
  text-align: center;
  padding: 14px 0 4px;
}

:deep(.team-avatar.ant-avatar) {
  width: 36px !important;
  min-width: 36px;
  height: 36px !important;
  line-height: 36px !important;
  flex: 0 0 36px;
  background: #eaeef2;
  color: #57606a;
  font-size: 13px;
  font-weight: 600;
}

.team-card--compact {
  padding: 10px;
  border-radius: 12px;
  box-shadow: none;
}

.team-card--compact:hover {
  transform: none;
  box-shadow: none;
}

.team-card--compact .team-card-header {
  align-items: center;
  margin-bottom: 8px;
}

.team-card--compact .team-card-subtitle {
  display: none;
}

.team-card--compact .team-card-stat {
  min-width: 48px;
  padding: 6px 8px;
}

.team-card--compact .team-card-count {
  font-size: 14px;
}

.team-card--compact .team-card-count-label {
  font-size: 10px;
}

.team-card--compact .team-list {
  gap: 6px;
}

.team-card--compact .team-item {
  padding: 8px 10px;
  gap: 8px;
  border-radius: 10px;
}

.team-card--compact .team-user {
  gap: 8px;
}

.team-card--compact .team-user-name {
  font-size: 12px;
}

.team-card--compact .team-user-role {
  font-size: 10px;
}

.team-card--compact .team-badge {
  height: 22px;
  padding: 0 7px;
  font-size: 10px;
}

.team-card--compact :deep(.team-avatar.ant-avatar) {
  width: 32px !important;
  min-width: 32px;
  height: 32px !important;
  line-height: 32px !important;
  flex: 0 0 32px;
  font-size: 12px;
}

:deep(.team-avatar .ant-avatar-string) {
  transform: none !important;
}

:deep(.team-avatar img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

@media (max-width: 640px) {
  .team-card-header,
  .team-item {
    flex-direction: column;
    align-items: stretch;
  }

  .team-card-stat {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }

  .team-badge {
    width: fit-content;
  }
}
</style>
