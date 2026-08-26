<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const links = computed(() => {
  const base = [
    { to: '/chat', title: 'AI 질의', desc: '궁금한 사내 규정을 물어보세요' },
    { to: '/chat/history', title: '질의 이력', desc: '지난 대화를 다시 확인하세요' },
    { to: '/mypage', title: '내 정보', desc: '프로필 확인 및 비밀번호 변경' },
  ]
  if (auth.user?.role === 'ADMIN') {
    base.push(
      { to: '/admin/members', title: '회원 관리', desc: '임직원 계정 상태 관리', admin: true },
      { to: '/admin/signup-requests', title: '가입 승인', desc: '예외 가입 신청 검토', admin: true },
    )
  }
  return base
})
</script>

<template>
  <div class="home">
    <el-card shadow="never" class="welcome">
      <h1 class="greeting">안녕하세요, {{ auth.user?.email }}님</h1>
      <p class="role">
        {{ auth.user?.role === 'ADMIN' ? '관리자' : '임직원' }} · 회사 ID {{ auth.user?.companyId }}
      </p>
    </el-card>

    <div class="grid">
      <RouterLink v-for="link in links" :key="link.to" :to="link.to" class="tile-link">
        <el-card shadow="hover" class="tile" :class="{ 'admin-tile': link.admin }">
          <h3>{{ link.title }}</h3>
          <p>{{ link.desc }}</p>
        </el-card>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.welcome {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
}
.welcome :deep(.el-card__body) {
  padding: 28px;
}
.greeting {
  margin: 0;
  font-size: 22px;
}
.role {
  margin: 8px 0 0;
  font-size: 13px;
  opacity: 0.85;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.tile-link {
  text-decoration: none;
  color: inherit;
}
.tile h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #303133;
}
.tile p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.admin-tile h3 {
  color: #b8860b;
}
</style>
