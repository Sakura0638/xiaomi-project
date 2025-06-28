<!-- src/views/ChatView.vue -->
<template>
  <div class="chat-container">
    <div class="chat-window">
      <div class="header">AI 客服</div>
      <div class="message-list" ref="messageList">
        <!-- 循环渲染消息列表 -->
        <div v-for="(message, index) in messages" :key="index" class="message-item" :class="message.sender">
          <div class="avatar" :class="message.sender">
            {{ message.sender === 'user' ? '我' : 'AI' }}
          </div>
          <div class="message-content">
            <div class="text">{{ message.text }}</div>
          </div>
        </div>
        <!-- 加载提示 -->
        <div v-if="isLoading" class="message-item assistant">
          <div class="avatar assistant">AI</div>
          <div class="message-content">
            <div class="text loading">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
      <div class="chat-input-area">
        <form @submit.prevent="sendMessage" class="input-form">
          <input
              type="text"
              v-model="newMessage"
              placeholder="请输入您的问题..."
              :disabled="isLoading"
              autocomplete="off"
          />
          <button type="submit" :disabled="isLoading || !newMessage.trim()">
            发送
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import axios from 'axios';

// 消息列表，存储所有对话
const messages = ref([]);
// 用户输入的新消息
const newMessage = ref('');
// 是否正在等待AI回复的加载状态
const isLoading = ref(false);
// 对消息列表DOM元素的引用，用于控制滚动
const messageList = ref(null);

// 发送消息的方法
const sendMessage = async () => {
  const userMessageText = newMessage.value.trim();
  if (!userMessageText || isLoading.value) return;

  // 1. 将用户发送的消息添加到列表
  messages.value.push({ sender: 'user', text: userMessageText });
  newMessage.value = ''; // 清空输入框
  isLoading.value = true;

  // 确保DOM更新后，滚动到底部
  await nextTick();
  scrollToBottom();

  try {
    // 2. 调用后端API
    // 注意：这里的axios请求会自动带上登录时服务器设置的cookie (JSESSIONID)
    const response = await axios.post('/api/chat/ask', {
      question: userMessageText,
    });

    // 3. 将AI的回复添加到列表
    messages.value.push({ sender: 'assistant', text: response.data });

  } catch (error) {
    // 4. 处理错误情况
    let errorText = '发生未知错误，请稍后再试。';
    if (error.response) {
      // 后端返回了具体的错误信息
      errorText = `请求失败：${error.response.data || error.response.statusText}`;
    } else if (error.request) {
      // 请求已发出，但没有收到响应（例如网络问题或超时）
      errorText = '无法连接到服务器，请检查您的网络。';
    }
    messages.value.push({ sender: 'assistant', text: errorText });
  } finally {
    isLoading.value = false;
    // 确保DOM更新后，再次滚动到底部
    await nextTick();
    scrollToBottom();
  }
};

// 滚动到消息列表底部的辅助函数
const scrollToBottom = () => {
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight;
  }
};

// 组件加载时，添加一条欢迎消息
onMounted(() => {
  messages.value.push({ sender: 'assistant', text: '您好！我是您的专属AI客服，有什么可以帮助您的吗？' });
});

</script>

<style scoped>
.chat-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.chat-window {
  width: 100%;
  max-width: 600px;
  height: 80vh;
  max-height: 800px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  padding: 16px;
  background-color: #007bff;
  color: white;
  font-weight: bold;
  font-size: 18px;
  text-align: center;
}

.message-list {
  flex-grow: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px; /* 消息之间的间距 */
}

/* 滚动条美化 */
.message-list::-webkit-scrollbar {
  width: 6px;
}
.message-list::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.message-item {
  display: flex;
  align-items: flex-end;
  max-width: 80%;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.avatar.user {
  background-color: #007bff;
  order: 2; /* 用户头像在右边 */
  margin-left: 10px;
}
.avatar.assistant {
  background-color: #6c757d;
  margin-right: 10px;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-item.user {
  align-self: flex-end; /* 用户消息靠右 */
  flex-direction: row-reverse; /* 内容和头像反向 */
}

.text {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.5;
  white-space: pre-wrap; /* 保留换行符 */
}

.message-item.user .text {
  background-color: #007bff;
  color: white;
  border-bottom-right-radius: 4px; /* 右下角变尖 */
}

.message-item.assistant .text {
  background-color: #e9ecef;
  color: #333;
  border-bottom-left-radius: 4px; /* 左下角变尖 */
}

.chat-input-area {
  padding: 15px;
  border-top: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.input-form {
  display: flex;
}

.input-form input {
  flex-grow: 1;
  padding: 12px;
  border: 1px solid #ccc;
  border-radius: 20px;
  margin-right: 10px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
}
.input-form input:focus {
  border-color: #007bff;
}
.input-form input:disabled {
  background-color: #f1f1f1;
}

.input-form button {
  padding: 10px 20px;
  border: none;
  background-color: #007bff;
  color: white;
  border-radius: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.input-form button:hover:not(:disabled) {
  background-color: #0056b3;
}

.input-form button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

/* 加载动画 */
.loading span {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #888;
  margin: 0 2px;
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading span:nth-child(1) { animation-delay: -0.32s; }
.loading span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

</style>
