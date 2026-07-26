<template>
    <div v-if="items.length" class="selected-media-preview">
        <header>
            <strong>{{ title }}</strong>
            <span>{{ items.length }} {{ $t('lms.files') }}</span>
        </header>

        <div class="selected-media-preview__grid">
            <article v-for="item in items" :key="item.uid" class="selected-media-preview__card">
                <img v-if="item.mimeType.startsWith('image/')" :src="item.url" :alt="item.name" />
                <video v-else-if="item.mimeType.startsWith('video/')" :src="item.url" muted controls />
                <div v-else class="selected-media-preview__file">
                    <el-icon><Document /></el-icon>
                </div>

                <div class="selected-media-preview__meta">
                    <strong :title="item.name">{{ item.name }}</strong>
                    <small>{{ formatSize(item.size) }}</small>
                </div>

                <div v-if="editable" class="lms-row-actions">
                    <el-tooltip :content="$t('lms.delete')" placement="top" :show-after="300">
                        <el-button
                            class="lms-row-action is-danger"
                            circle
                            :aria-label="$t('lms.delete')"
                            @click="$emit('remove', item.uid)"
                        >
                            <el-icon><Delete /></el-icon>
                        </el-button>
                    </el-tooltip>
                </div>
            </article>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Delete, Document } from '@element-plus/icons-vue'

export interface SelectedMediaPreviewItem {
    uid: string
    name: string
    size: number
    mimeType: string
    url: string
}

defineProps<{ title: string; items: SelectedMediaPreviewItem[]; editable?: boolean }>()
defineEmits<{ remove: [string] }>()

function formatSize(size: number) {
    if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`
    return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.selected-media-preview {
    margin-top: 14px;
    padding: 14px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
    background: var(--el-fill-color-extra-light);
}

.selected-media-preview header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.selected-media-preview header strong {
    color: var(--el-text-color-primary);
}

.selected-media-preview header span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.selected-media-preview__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
}

.selected-media-preview__card {
    min-width: 0;
    overflow: hidden;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    background: var(--el-bg-color);
}

.selected-media-preview__card img,
.selected-media-preview__card video,
.selected-media-preview__file {
    display: block;
    width: 100%;
    height: 96px;
    object-fit: cover;
    background: #eef2f7;
}

.selected-media-preview__file {
    display: grid;
    place-items: center;
    color: var(--el-color-primary);
    font-size: 28px;
}

.selected-media-preview__meta {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 2px;
    padding: 9px 10px 0;
}

.selected-media-preview__meta strong {
    overflow: hidden;
    color: var(--el-text-color-regular);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.selected-media-preview__meta small {
    color: var(--el-text-color-secondary);
}

.selected-media-preview__card .lms-row-actions {
    display: flex;
    justify-content: flex-end;
    padding: 4px 8px 8px;
}

.selected-media-preview__card :deep(.lms-row-action) {
    width: 30px;
    height: 30px;
    margin: 0;
}
</style>
