<template>
  <div class="auth-container">
    <div class="auth-form">
      <h2>创建您的账户</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required>
        </div>
        <button type="submit" class="btn-submit">注册</button>
      </form>
      <p v-if="message" class="message" :class="{ 'error': isError }">{{ message }}</p>
      <div class="switch-link">
        <p>已有账户? <router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const username = ref('');
const password = ref('');
const message = ref('');
const isError = ref(false);
const router = useRouter();

const handleRegister = async () => {
  if (!username.value || !password.value) {
    message.value = '用户名和密码不能为空！';
    isError.value = true;
    return;
  }

  try {
    // 使用我们配置的代理，请求会自动转发到 http://localhost:8080/api/auth/register
    const response = await axios.post('/api/auth/register', {
      username: username.value,
      password: password.value
    });

    message.value = response.data + ' 即将跳转到登录页面...';
    isError.value = false;

    // 注册成功后，延时2秒跳转到登录页
    setTimeout(() => {
      router.push('/login');
    }, 2000);

  } catch (error) {
    // 处理后端返回的错误信息
    isError.value = true;
    if (error.response && error.response.data) {
      message.value = error.response.data;
    } else {
      message.value = '注册失败，请检查网络或联系管理员。';
    }
  }
};
</script>

<style scoped>
/* 在下方添加通用样式 */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
}
.auth-form {
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  text-align: center;
}
h2 {
  margin-bottom: 24px;
  color: #333;
}
.form-group {
  margin-bottom: 20px;
  text-align: left;
}
label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: bold;
}
input {
  width: 100%;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ddd;
  box-sizing: border-box; /* 保证padding不会撑大宽度 */
}
.btn-submit {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 4px;
  background-color: #007bff;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}
.btn-submit:hover {
  background-color: #0056b3;
}
.message {
  margin-top: 20px;
  padding: 10px;
  border-radius: 4px;
  color: #155724;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
}
.message.error {
  color: #721c24;
  background-color: #f8d7da;
  border-color: #f5c6cb;
}
.switch-link {
  margin-top: 20px;
}
</style>
