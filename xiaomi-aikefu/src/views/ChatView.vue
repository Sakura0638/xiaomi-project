<template>
  <!-- 单一的根元素 -->
  <div class="page-container">
    <!-- 历史记录侧边栏 -->
    <div class="history-panel">
      <div class="history-header">
        <span>对话历史</span>
        <button @click="startNewConversation" class="new-chat-btn" title="开始新的对话">
          新建对话
        </button>
      </div>
      <div class="history-list">
        <div
            v-for="item in conversationHistory"
            :key="item.id"
            class="history-item"
            :class="{ 'active': item.conversationId === currentConversationId }"
        >
          <!-- 将内容包裹在一个div中，方便flex布局 -->
          <div class="history-content" @click="loadHistory(item)">
            <p class="history-question">{{ item.question }}</p>
            <span class="history-time">{{ formatTime(item.timestamp) }}</span>
          </div>
          <!-- 新增的删除按钮 -->
          <button @click.stop="deleteConversation(item.conversationId)" class="delete-btn" title="删除对话">
            × <!-- 这是一个简单的 "x" 叉号 -->
          </button>
        </div>
        <p v-if="!conversationHistory.length" class="no-history">暂无历史记录</p>
      </div>
    </div>

    <!-- 聊天主区域 -->
    <div class="chat-main-area">
      <div class="header">AI 客服</div>
      <div class="message-list" ref="messageList">
        <!-- 消息列表渲染 -->
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
        <!-- 输入表单 -->
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

// 响应式数据
const conversationHistory = ref([]);
const messages = ref([]);
const newMessage = ref('');
const isLoading = ref(false);
const messageList = ref(null);
const currentConversationId = ref(null);

// --- 新增方法：删除对话 ---
const deleteConversation = async (conversationId) => {
  // 友好地向用户确认
  if (!confirm("您确定要删除这条对话记录吗？此操作不可恢复。")) {
    return;
  }

  try {
    // 调用后端的DELETE API
    await axios.delete(`/api/history/${conversationId}`);

    // 从前端的历史记录列表中移除被删除的项，实现实时更新
    conversationHistory.value = conversationHistory.value.filter(
        item => item.conversationId !== conversationId
    );

    // 如果删除的是当前正在查看的对话，则新建一个对话
    if (currentConversationId.value === conversationId) {
      startNewConversation();
    }

  } catch (error) {
    console.error("删除对话失败:", error);
    // 可以在这里给用户一个错误提示
    alert(`删除失败：${error.response?.data || '请稍后再试'}`);
  }
};

// --- 新增方法：开始新对话 ---
const startNewConversation = () => {
  messages.value = [
    { sender: 'assistant', text: '您好！有什么新问题吗？' }
  ];
  currentConversationId.value = null; // 清空会话ID，这很关键
};

// 获取历史记录
const fetchHistory = async () => {
  try {
    const response = await axios.get('/api/history');
    conversationHistory.value = response.data;
  } catch (error) {
    console.error("获取历史记录失败:", error);
  }
};

const loadHistory = async (historyItem) => {
  try {
    // 之前这里的逻辑是错误的，现在改为调用后端获取完整对话
    const response = await axios.get(`/api/history/${historyItem.conversationId}`);
    const fullConversation = response.data;

    messages.value = [];
    fullConversation.forEach(item => {
      messages.value.push({ sender: 'user', text: item.question });
      messages.value.push({ sender: 'assistant', text: item.answer });
    });

    // 点击加载历史对话时，也将会话ID设置为当前会话ID
    currentConversationId.value = historyItem.conversationId;

    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error("加载历史对话失败:", error);
  }
};

// 格式化时间显示
const formatTime = (timestamp) => {
  if (!timestamp) return '';

  const date = new Date(timestamp);
  return `
    ${(date.getMonth() + 1).toString().padStart(2, '0')}-
    ${date.getDate().toString().padStart(2, '0')}
    ${date.getHours().toString().padStart(2, '0')}:
    ${date.getMinutes().toString().padStart(2, '0')}
  `.replace(/\s+/g, '');
};

// 发送消息
const sendMessage = async () => {
  const userMessageText = newMessage.value.trim();
  if (!userMessageText || isLoading.value) return;

  // 添加用户消息
  messages.value.push({
    sender: 'user',
    text: userMessageText
  });
  newMessage.value = '';
  isLoading.value = true;

  await nextTick();
  scrollToBottom();

  try {
    // 发送请求获取AI回复
    const response = await axios.post('/api/chat/ask', {
      question: userMessageText,
      conversationId: currentConversationId.value
    });

    const chatResponse = response.data;

    // 将AI的回复添加到列表
    messages.value.push({
      sender: 'assistant',
      text: chatResponse.answer
    });

    // !!! 关键：更新前端的会话ID !!!
    currentConversationId.value = chatResponse.conversationId;
  } catch (error) {
    let errorText = '发生未知错误，请稍后再试。';

    if (error.response) {
      errorText = `请求失败：${
          error.response.data || error.response.statusText
      }`;
    } else if (error.request) {
      errorText = '无法连接到服务器，请检查您的网络。';
    }

    messages.value.push({
      sender: 'assistant',
      text: errorText
    });
  } finally {
    isLoading.value = false;
    await nextTick();
    scrollToBottom();
    fetchHistory();
  }
};

// 滚动到底部
const scrollToBottom = () => {
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight;
  }
};

onMounted(() => {
  // 初始时不加载欢迎语，而是直接调用新建对话
  startNewConversation();
  fetchHistory();
});
</script>

<style scoped>
.page-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5;
  overflow: hidden;
}

.history-item {
  /* 使用flex布局来放置内容和删除按钮 */
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 10px 12px 16px; /* 调整内边距 */
  /* ... 其他 .history-item 样式 ... */
}

.history-content {
  flex-grow: 1; /* 让内容区填充大部分空间 */
  cursor: pointer;
  overflow: hidden; /* 防止内容过长影响布局 */
}
.history-content:hover .history-question {
  color: #007bff; /* 悬浮时标题变色，提升可点击感 */
}

.history-question {
  /* ... */
  transition: color 0.2s;
}

/* 新增的删除按钮样式 */
.delete-btn {
  background: none;
  border: none;
  color: #aaa;
  font-size: 22px;
  font-weight: bold;
  cursor: pointer;
  padding: 0 10px;
  border-radius: 50%;
  line-height: 1;
  opacity: 0; /* 默认隐藏 */
  transition: opacity 0.2s, color 0.2s;
  flex-shrink: 0; /* 防止被压缩 */
}

/* 当鼠标悬浮在整个历史记录项上时，显示删除按钮 */
.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #f44336; /* 悬浮时变红色 */
}

/* 当前激活项的样式 */
.history-item.active {
  background-color: #e9f5ff;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* ... */
}
.new-chat-btn {
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  padding: 5px 10px;
  cursor: pointer;
  font-size: 14px;
}
.new-chat-btn:hover {
  background-color: #0056b3;
}

/* 历史记录侧边栏样式 */
.history-panel {
  width: 280px;
  flex-shrink: 0;
  background-color: #ffffff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.history-header {
  padding: 16px;
  font-weight: bold;
  font-size: 18px;
  text-align: center;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
  color: #333;
}

.history-list {
  flex-grow: 1;
  overflow-y: auto;
}

.history-list::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.history-item {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #e9ecef;
}

.history-question {
  font-weight: 500;
  color: #333;
  margin: 0 0 5px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-time {
  font-size: 12px;
  color: #888;
}

.no-history {
  text-align: center;
  color: #999;
  margin-top: 20px;
  font-size: 14px;
}

/* 聊天主区域样式 */
.chat-main-area {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  background-color: #f7f7f7;
}

.header {
  padding: 16px 24px;
  background-color: #ffffff;
  font-weight: bold;
  font-size: 18px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.message-list {
  flex-grow: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

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
  max-width: 70%;
}

.message-item.user {
  align-self: flex-end;
  flex-direction: row-reverse;
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

.text {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.message-item.user .text {
  background-color: #007bff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .text {
  background-color: #ffffff;
  color: #333;
  border-bottom-left-radius: 4px;
  border: 1px solid #e9ecef;
}

/* 输入区域样式 */
.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
  background-color: #ffffff;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.input-form {
  display: flex;
}

.input-form input {
  flex-grow: 1;
  padding: 12px 18px;
  border: 1px solid #ccc;
  border-radius: 24px;
  margin-right: 10px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.2s;
}

.input-form input:focus {
  border-color: #007bff;
}

.input-form button {
  padding: 0 24px;
  border: none;
  background-color: #007bff;
  color: white;
  border-radius: 24px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
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

.loading span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1.0);
  }
}
</style>
