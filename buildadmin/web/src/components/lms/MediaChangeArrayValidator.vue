<template>
    <el-collapse v-if="totalItems" class="media-change-validator">
        <el-collapse-item name="mediaChanges">
            <template #title>
                <span class="media-change-validator__title">
                    {{ $t('lms.mediaFiles') }}
                    <el-tag size="small" effect="plain">{{ totalItems }} ID</el-tag>
                </span>
            </template>

            <div class="media-change-validator__grid">
                <section v-for="group in groups" :key="group.key" class="media-change-validator__group">
                    <header>
                        <strong>{{ $t(group.label) }}</strong>
                        <small>{{ model[group.key].length }} {{ $t('lms.files') }}</small>
                    </header>

                    <el-empty v-if="!model[group.key].length" :description="$t('lms.noData')" :image-size="48" />

                    <el-form-item
                        v-for="(_mediaId, index) in model[group.key]"
                        :key="`${group.key}-${index}-${_mediaId}`"
                        :prop="fieldProp(group.key, index)"
                        :rules="mediaIdRules(group.key, index)"
                    >
                        <div class="media-change-validator__row">
                            <span>#{{ index + 1 }}</span>
                            <el-input-number :model-value="model[group.key][index]" :controls="false" disabled />
                        </div>
                    </el-form-item>
                </section>
            </div>
        </el-collapse-item>
    </el-collapse>
</template>

<script setup lang="ts">
import { computed, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormItemRule } from 'element-plus'
import type { MediaChanges } from '/@/types/lms/common'

type MediaArrayKey = keyof MediaChanges

const props = defineProps<{ model: MediaChanges }>()
const emit = defineEmits<{ validateField: [prop: string] }>()
const { t } = useI18n()

const groups: Array<{ key: MediaArrayKey; label: string }> = [
    { key: 'retainedMediaIds', label: 'lms.retained' },
    { key: 'newMediaIds', label: 'lms.newItems' },
    { key: 'removedMediaIds', label: 'lms.removed' },
]

const totalItems = computed(() => groups.reduce((total, group) => total + props.model[group.key].length, 0))
const serialisedMediaChanges = computed(() => groups.map((group) => props.model[group.key].join(',')).join('|'))

function fieldProp(group: MediaArrayKey, index: number) {
    return `${group}.${index}`
}

function mediaIdRules(group: MediaArrayKey, index: number): FormItemRule[] {
    return [{ validator: validateMediaId(group, index), trigger: 'change' }]
}

function validateMediaId(group: MediaArrayKey, index: number) {
    return (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        const mediaId = Number(props.model[group][index])

        if (!Number.isInteger(mediaId) || mediaId <= 0) {
            callback(new Error(t('lms.mediaIdPositive')))
            return
        }

        if (props.model[group].indexOf(mediaId) !== index) {
            callback(new Error(t('lms.mediaIdDuplicate')))
            return
        }

        const conflictingGroup = groups.find((item) => item.key !== group && props.model[item.key].includes(mediaId))
        if (conflictingGroup) {
            callback(new Error(t('lms.mediaIdConflict', { group: t(conflictingGroup.label) })))
            return
        }

        callback()
    }
}

function validateRealtime() {
    groups.forEach((group) => {
        props.model[group.key].forEach((_mediaId, index) => emit('validateField', fieldProp(group.key, index)))
    })
}

watch(
    serialisedMediaChanges,
    () => {
        void nextTick(validateRealtime)
    },
    { flush: 'post' }
)
</script>

<style scoped>
.media-change-validator {
    margin-top: 16px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    background: var(--el-fill-color-extra-light);
}

.media-change-validator :deep(.el-collapse-item__header) {
    height: 44px;
    padding: 0 14px;
    border-bottom: 0;
    background: transparent;
}

.media-change-validator :deep(.el-collapse-item__wrap) {
    border-bottom: 0;
    background: transparent;
}

.media-change-validator :deep(.el-collapse-item__content) {
    padding: 0 14px 14px;
}

.media-change-validator__title {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    color: var(--el-text-color-regular);
    font-weight: 650;
}

.media-change-validator__grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
}

.media-change-validator__group {
    min-width: 0;
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-bg-color);
}

.media-change-validator__group header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
}

.media-change-validator__group strong {
    color: var(--el-text-color-primary);
    font-size: 13px;
}

.media-change-validator__group small {
    color: var(--el-text-color-secondary);
}

.media-change-validator__row {
    display: grid;
    width: 100%;
    grid-template-columns: 42px minmax(0, 1fr);
    gap: 8px;
    align-items: center;
}

.media-change-validator__row > span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: 650;
}

.media-change-validator__row :deep(.el-input-number) {
    width: 100%;
}

.media-change-validator :deep(.el-form-item) {
    margin-bottom: 12px;
}

.media-change-validator :deep(.el-form-item:last-child) {
    margin-bottom: 0;
}

@media (max-width: 860px) {
    .media-change-validator__grid {
        grid-template-columns: 1fr;
    }
}
</style>
