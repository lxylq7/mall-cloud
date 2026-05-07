<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getRememberStatus } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456',
  remember: getRememberStatus(),
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const formRef = ref()

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    const redirect = route.query.redirect || '/dashboard'
    router.push(String(redirect))
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <template #header>
        <div class="login-title">mall-cloud-admin</div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
        </el-form-item>
        <el-button :loading="loading" type="primary" style="width: 100%" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="login-tip">演示账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f0f5ff 0%, #ecfeff 100%);
}

.login-card {
  width: 420px;
  border-radius: 10px;
}

.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
}

.login-tip {
  margin-top: 12px;
  text-align: center;
  color: #6b7280;
  font-size: 12px;
}
</style>
