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
// import { useRouter } from 'vue-router'; // 登录成功后可以跳转到主页

const username = ref('');
const password = ref('');
const message = ref('');
const isError = ref(false);
// const router = useRouter();

const handleLogin = async () => {
  if (!username.value || !password.value) {
    message.value = '用户名和密码不能为空！';
    isError.value = true;
    return;
  }

  try {
    const response = await axios.post('/api/auth/login', {
      username: username.value,
      password: password.value
    });

    message.value = response.data;
    isError.value = false;

    // 登录成功后，浏览器会自动保存JSESSIONID cookie。
    // 在这里你可以跳转到应用的主页
    // router.push('/dashboard'); // 假设主页路由是/dashboard

  } catch (error) {
    isError.value = true;
    // Spring Security 默认的认证失败是401 Unauthorized，没有具体的body
    message.value = '登录失败：用户名或密码错误。';
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
