<template>
    <div class="media-list">
        <div v-for="item in modelValue" :key="item.mediaId" class="media-card">
            <el-image v-if="item.mimeType?.startsWith('image/')" :src="mediaContentUrl(item.mediaId)" fit="cover" /><video
                v-else-if="item.mimeType?.startsWith('video/')"
                :src="mediaContentUrl(item.mediaId)"
                controls
            /><el-icon v-else size="30"><Document /></el-icon>
            <div class="name" :title="item.originalName">{{ item.originalName }}</div>
            <div class="lms-row-actions">
                <el-tooltip :content="$t('lms.download')" placement="top" :show-after="300">
                    <el-button
                        class="lms-row-action"
                        circle
                        :aria-label="$t('lms.download')"
                        @click="downloadMedia(item)"
                    >
                        <el-icon><Download /></el-icon>
                    </el-button>
                </el-tooltip>
                <el-tooltip v-if="editable" :content="$t('lms.delete')" placement="top" :show-after="300">
                    <el-button
                        class="lms-row-action is-danger"
                        circle
                        :aria-label="$t('lms.delete')"
                        @click="$emit('remove', item)"
                    >
                        <el-icon><Delete /></el-icon>
                    </el-button>
                </el-tooltip>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { Delete, Document, Download } from '@element-plus/icons-vue'
import { mediaContentUrl, downloadMedia } from '/@/api/lms/mediaApi'
import type { MediaInfo } from '/@/types/lms/common'
defineProps<{ modelValue: MediaInfo[]; editable?: boolean }>()
defineEmits<{ remove: [MediaInfo] }>()
</script>
<style scoped>
.media-list {
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
}
.media-card {
    width: 154px;
    overflow: hidden;
    border: 1px solid #e5eaf2;
    border-radius: 12px;
    padding: 9px;
    background: #fff;
    text-align: left;
    box-shadow: 0 4px 14px rgba(31, 45, 61, 0.06);
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease;
}
.media-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 9px 22px rgba(31, 45, 61, 0.11);
}
.media-card :deep(.el-image),
.media-card video {
    display: block;
    width: 134px;
    height: 92px;
    object-fit: cover;
    border-radius: 8px;
    background: #eef2f7;
}
.media-card > :deep(.el-icon) {
    display: flex;
    width: 134px;
    height: 92px;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: #eff6ff;
    color: #2563eb;
}
.name {
    margin: 9px 2px 3px;
    color: #344054;
    font-size: 12px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.media-card .lms-row-actions {
    display: flex;
    justify-content: flex-end;
    gap: 6px;
    padding: 3px 2px 0;
}

.media-card :deep(.lms-row-action) {
    width: 30px;
    height: 30px;
    margin: 0;
}
</style>
