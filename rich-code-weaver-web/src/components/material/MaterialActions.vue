<template>
  <div class="material-actions" @click.stop>
    <button type="button" class="material-action-btn" title="编辑素材" @click="emit('edit', item)">
      <EditOutlined />
    </button>
    <a-popconfirm title="确定删除此素材吗？" @confirm="emit('delete', item)">
      <button type="button" class="material-action-btn is-danger" title="删除素材">
        <DeleteOutlined />
      </button>
    </a-popconfirm>
  </div>
</template>

<script lang="ts" setup>
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

defineProps<{ item: API.MaterialVO }>()
const emit = defineEmits<{
  edit: [item: API.MaterialVO]
  delete: [item: API.MaterialVO]
}>()
</script>

<style scoped>
.material-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
  min-width: 59px;
  overflow: hidden;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
}

.material-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 29px;
  height: 27px;
  padding: 0;
  border: 0;
  border-radius: 0;
  color: #777;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease;
}

.material-action-btn + .material-action-btn {
  border-left: 1px solid #f0f0f0;
}

.material-action-btn:hover {
  color: #1a1a1a;
  background: #f5f5f5;
}

.material-action-btn.is-danger:hover {
  color: #1a1a1a;
  background: #f5f5f5;
}
</style>
