<template>
  <div class="page-container">
    <!-- 折叠/展开控制按钮 -->
    <button class="collapse-btn" @click="isCollapsed = !isCollapsed" :title="isCollapsed ? '展开历史记录' : '收起历史记录'">
      {{ isCollapsed ? '⮞' : '⮜' }}
    </button>
    <div class="history-panel" v-show="!isCollapsed">
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
          <div class="history-content" @click="loadHistory(item)">
            <p class="history-question">{{ item.question }}</p>
            <span class="history-time">{{ formatTime(item.timestamp) }}</span>
          </div>
          <button @click.stop="deleteConversation(item.conversationId)" class="delete-btn" title="删除对话">
            ×
          </button>
        </div>
        <p v-if="!conversationHistory.length" class="no-history">暂无历史记录</p>
      </div>
    </div>

    <div class="chat-main-area" :class="{ 'full-width': isCollapsed }">
      <div class="model-selector-area">
        <label for="model-select">选择模型: </label>
        <select v-model="selectedModel" id="model-select">
          <option value="deepseek">DeepSeek</option>
          <option value="dashscope">通义千问</option>
        </select>
      </div>
      <div class="header">AI 客服</div>
      <div class="message-list" ref="messageList">
        <div v-for="(message, index) in messages" :key="index" :class="['message-item', message.sender]">
          <!-- 用户头像图片 -->
          <div class="avatar user" v-if="message.sender === 'user'">
            <img src="/touxaing.jpg" alt="用户头像" class="avatar-img" />
          </div>
          <!-- AI 图片头像 -->
          <div class="avatar assistant" v-else>
            <img src="/background.jpg" alt="AI头像" class="avatar-img" />
          </div>
          <div class="message-content">
            <div class="text" v-html="message.text"></div>
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
          <button type="submit" :disabled="isLoading || !newMessage.trim()">发送</button>
        </form>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, nextTick, onMounted } from 'vue';
import MarkdownIt from 'markdown-it';
import axios from 'axios';

const conversationHistory = ref([]);
const messages = ref([]);
const newMessage = ref('');
const isLoading = ref(false);
const messageList = ref(null);
const currentConversationId = ref(null);
const selectedModel = ref('deepseek');
const isCollapsed = ref(false);

const md = new MarkdownIt({
  breaks: true,         // 支持换行
  linkify: true,        // 自动链接
  html: false           // 禁止原始 HTML（可提高安全）
});

const sendMessage = async () => {
  const userMessageText = newMessage.value.trim();
  if (!userMessageText || isLoading.value) return;

  isLoading.value = true;
  messages.value.push({ sender: 'user', text: userMessageText });
  messages.value.push({ sender: 'assistant', text: '',raw:'' });

  const question = newMessage.value;
  newMessage.value = '';

  await nextTick();
  scrollToBottom();

  // 修改URL，带上选择的模型参数
  let url = `/api/chat/stream-ask?question=${encodeURIComponent(question)}&model=${selectedModel.value}`;
  if (currentConversationId.value) {
    url += `&conversationId=${encodeURIComponent(currentConversationId.value)}`;
  }

  const eventSource = new EventSource(url);

  eventSource.onopen = function() {
    console.log("Connection to server opened.");
  };

  eventSource.onmessage = function (event) {
    const lastMessageIndex = messages.value.length - 1;
    const lastMessage = messages.value[lastMessageIndex];

    if (lastMessage && lastMessage.sender === 'assistant') {
      // 原始 Markdown 拼接
      const updatedRawText = (lastMessage.raw || '') + event.data;

      // 渲染为 HTML
      const html = md.render(updatedRawText);

      // 更新消息（包括原始 raw 字符串，以便后续继续拼接）
      messages.value[lastMessageIndex] = {
        ...lastMessage,
        raw: updatedRawText,      // ⬅️ 保留原始 Markdown
        text: html                // ⬅️ 渲染成 HTML
      };

      nextTick(() => {
        scrollToBottom();
      });
    }
  };

  eventSource.onerror = function (err) {
    console.error("EventSource failed:", err);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.sender === 'assistant' && lastMessage.text === '') {
      lastMessage.text = '流式连接出错，请重试。';
    }
    eventSource.close();
    isLoading.value = false;
    fetchHistory();
  };
};

const startNewConversation = () => {
  messages.value = [{ sender: 'assistant', text: '您好！有什么新问题吗？' }];
  currentConversationId.value = null;
  isLoading.value = false;
};

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
    const response = await axios.get(`/api/history/${historyItem.conversationId}`);
    const fullConversation = response.data;

    messages.value = [];

    // ✅ 使用 markdown-it 渲染历史内容
    fullConversation.forEach(item => {
      messages.value.push({
        sender: 'user',
        text: md.render(item.question),
        raw: item.question
      });
      messages.value.push({
        sender: 'assistant',
        text: md.render(item.answer),
        raw: item.answer
      });
    });

    currentConversationId.value = historyItem.conversationId;

    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error("加载历史对话失败:", error);
  }
};

const deleteConversation = async (conversationId) => {
  if (!confirm("您确定要删除这条对话记录吗？此操作不可恢复。")) {
    return;
  }

  try {
    await axios.delete(`/api/history/${conversationId}`);
    conversationHistory.value = conversationHistory.value.filter(
        item => item.conversationId !== conversationId
    );

    if (currentConversationId.value === conversationId) {
      startNewConversation();
    }
  } catch (error) {
    console.error("删除对话失败:", error);
    alert(`删除失败：${error.response?.data || '请稍后再试'}`);
  }
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${month}-${day} ${hours}:${minutes}`;
};


const scrollToBottom = () => {
  if (messageList.value) {
    messageList.value.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' });
  }
};

onMounted(() => {
  startNewConversation();
  fetchHistory();
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

:root {
  --background-color: #1e1e2f;
  --font-family: 'Noto Sans SC', sans-serif;
  --primary-color: #5e5ce6;
  --primary-hover-color: #4845d3;
  --text-color-light: #f5f5f7;
  --text-color-dark: #1d1d1f;
  --text-color-muted: rgba(255, 255, 255, 0.55);
  --border-color: rgba(255, 255, 255, 0.15);
  --bubble-user-bg: var(--primary-color);
  --bubble-ai-bg: rgba(255, 255, 255, 0.1);
  --panel-bg: rgba(28, 28, 30, 0.5);
  --panel-bg-light: rgba(255, 255, 255, 0.08);
}

.page-container {
  font-family: var(--font-family);
  height: 100vh;
  width: 100vw;
  background-image: url('/background.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  padding: 20px;
  box-sizing: border-box;
  color: var(--text-color-light);
}

.history-panel,
.chat-main-area {
  height: 100%;
  background: var(--panel-bg);
  border: 1px solid var(--border-color);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  transition: all 0.3s ease;
  border-radius: 24px;
}

.history-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  margin-right: 20px;
}

.chat-main-area {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.history-header,
.header {
  padding: 20px;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.new-chat-btn {
  background: none;
  border: none;
  color: var(--text-color-light);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  font-weight: 500;
  background-color: var(--primary-color);
  transition: all 0.2s;
}
.new-chat-btn:hover {
  background-color: var(--primary-hover-color);
  transform: scale(1.05);
}

.history-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px;
  margin: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}
.history-item.active {
  background-color: var(--primary-color);
}
.history-content {
  flex-grow: 1;
  cursor: pointer;
  overflow: hidden;
}
.history-question {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 0 4px 0;
}
.history-time {
  font-size: 12px;
  color: var(--text-color-muted);
}
.delete-btn {
  font-size: 20px;
  opacity: 0;
  color: var(--text-color-muted);
  transition: all 0.2s;
}
.history-item:hover .delete-btn {
  opacity: 0.8;
}
.delete-btn:hover {
  color: #ff6b6b;
  transform: scale(1.1);
}

.message-list {
  flex-grow: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 85%;
}
.message-item.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.message-item.assistant {
  align-self: flex-start;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}
.avatar.user {
  background-color: var(--primary-color);
}
.avatar.assistant {
  background-color: #6c757d;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.text {
  padding: 10px 16px;
  border-radius: 18px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
  animation: pop-in 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes pop-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.message-item.user .text {
  background-color: var(--bubble-user-bg);
  color: #000000;
  border-bottom-right-radius: 6px;
}
.message-item.assistant .text {
  background-color: var(--bubble-ai-bg);
  color: #000000;
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background-color: transparent;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.input-form {
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: var(--panel-bg-light);
  border-radius: 24px;
  padding: 6px;
}

.input-form input {
  flex-grow: 1;
  padding: 10px 18px;
  border: none;
  font-size: 16px;
  outline: none;
  background-color: transparent;
  color: var(--text-color-light);
}
.input-form input::placeholder {
  color: var(--text-color-muted);
}
.input-form button {
  padding: 0 24px;
  border: none;
  background-color: var(--primary-color);
  color: white;
  border-radius: 24px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.input-form button:hover:not(:disabled) {
  background-color: var(--primary-hover-color);
  transform: scale(1.05);
}
.input-form button:disabled {
  background-color: #555;
  cursor: not-allowed;
}
.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.model-selector-area {
  padding: 12px 24px;
  border-top: 1px solid var(--border-color);
  text-align: right;
  font-size: 14px;
  flex-shrink: 0;
  background: var(--background-color);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.model-selector-area select {
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-color-light);
  border: 1px solid var(--border-color);
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  outline: none;
}

.model-selector-area select:hover {
  background: rgba(255, 255, 255, 0.15);
  cursor: pointer;
}

.model-selector-area select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(100, 150, 255, 0.3);
}
/* 折叠按钮 */
.collapse-btn {
  position: absolute;
  left: 10px;
  top: 10px;
  z-index: 10;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.2s;
}
.collapse-btn:hover {
  transform: scale(1.1);
}

/* 折叠时让主区域撑满 */
.chat-main-area.full-width {
  width: 100%;
}
.history-panel {
  transition: width 0.3s ease, opacity 0.3s ease;
}
.chat-main-area {
  transition: width 0.3s ease;
}


</style>
