<template>
  <div class="auth-container">
    <div class="auth-form">
      <h2>登录您的账户</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required>
        </div>
        <button type="submit" class="btn-submit">登录</button>
      </form>
      <p v-if="message" class="message" :class="{ 'error': isError }">{{ message }}</p>
      <div class="switch-link">
        <p>还没有账户? <router-link to="/register">立即注册</router-link></p>
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

const handleLogin = async () => {
  if (!username.value || !password.value) {
    message.value = '用户名和密码不能为空！';
    isError.value = true;
    return;
  }

  message.value = '正在登录中...'; // 优化用户体验，立即给出反馈
  isError.value = false;

  try {
    // --- 这里是修改的关键 ---
    // 将用户名和密码作为请求体发送
    const response = await axios.post('/api/auth/login', {
      username: username.value,
      password: password.value
    });

    // 假设后端成功时返回 "用户登录成功!"
    message.value = response.data + " 即将进入聊天室...";
    isError.value = false;

    setTimeout(() => {
      router.push('/chat');
    }, 1000);

  } catch (error) {
    isError.value = true;
    if (error.response) {
      // 尝试从后端获取更具体的错误信息
      // 401 Unauthorized, 403 Forbidden etc.
      if (error.response.status === 401 || error.response.status === 403) {
        message.value = '登录失败：用户名或密码错误。';
      } else {
        message.value = `登录时发生错误: ${error.response.data || error.response.statusText}`;
      }
    } else {
      // 网络错误等
      message.value = '无法连接到服务器，请检查网络。';
    }
  }
};
</script>

<style scoped>
/* 样式与注册页面完全相同，可以复用 */
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
  box-sizing: border-box;
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
