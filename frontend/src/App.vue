<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import EmployeeShell from '@/layouts/EmployeeShell.vue'
import AdminShell from '@/layouts/AdminShell.vue'

const route = useRoute()

const layout = computed(() => {
  if (!route.meta.requiresAuth) return null
  return route.meta.requiresAdmin || route.meta.requiresSuperAdmin ? AdminShell : EmployeeShell
})
</script>

<template>
  <component :is="layout" v-if="layout" />
  <RouterView v-else />
</template>

<style>
body {
  margin: 0;
  background: #f5f7fa;
  color: #1f2329;
}
</style>
