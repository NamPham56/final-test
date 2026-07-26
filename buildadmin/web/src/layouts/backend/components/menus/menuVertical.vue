<template>
    <el-scrollbar ref="layoutMenuScrollbarRef" class="vertical-menus-scrollbar"
        ><el-menu
            router
            class="layouts-menu-vertical"
            :default-active="activeMenu"
            :collapse="config.layout.menuCollapse"
            :collapse-transition="false"
            ><el-sub-menu index="lms"
                ><template #title
                    ><el-icon><Reading /></el-icon><span>{{ $t('lms.app') }}</span></template
                ><el-menu-item index="/admin/students"
                    ><el-icon><User /></el-icon><span>{{ $t('lms.students') }}</span></el-menu-item
                ><el-menu-item index="/admin/courses"
                    ><el-icon><Collection /></el-icon><span>{{ $t('lms.courses') }}</span></el-menu-item
                ><el-menu-item index="/admin/lessons"
                    ><el-icon><Notebook /></el-icon><span>{{ $t('lms.lessons') }}</span></el-menu-item
                ><el-menu-item index="/admin/enrollments"
                    ><el-icon><Tickets /></el-icon><span>{{ $t('lms.enrollments') }}</span></el-menu-item
                ></el-sub-menu
            ></el-menu
        ></el-scrollbar
    >
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Reading, User, Collection, Notebook, Tickets } from '@element-plus/icons-vue'
import { useConfig } from '/@/stores/config'
import { layoutMenuScrollbarRef } from '/@/stores/refs'
const config = useConfig(),
    route = useRoute()
const activeMenu = computed(() => {
    const section = route.path.split('/')[2]
    return section ? `/admin/${section}` : route.path
})
const verticalMenusScrollbarHeight = computed(
    () => `calc(100% - ${(config.layout.menuShowTopBar ? 50 : 0) + (config.layout.menuCollapse ? 100 : 50)}px)`
)
</script>
<style scoped lang="scss">
.vertical-menus-scrollbar {
    height: v-bind(verticalMenusScrollbarHeight);
    background-color: v-bind('config.getColorVal("menuBackground")');
}
.layouts-menu-vertical {
    border: 0;
    --el-menu-bg-color: v-bind('config.getColorVal("menuBackground")');
    --el-menu-text-color: v-bind('config.getColorVal("menuColor")');
    --el-menu-active-color: v-bind('config.getColorVal("menuActiveColor")');
    --el-menu-hover-bg-color: v-bind('config.getColorVal("menuHoverBackground")');
    --el-menu-active-bg-color: v-bind('config.getColorVal("menuActiveBackground")');
}
</style>
