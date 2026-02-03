# 用户设置 API 文档

## 基础信息

- **Base URL**: `/user/setting`
- **认证方式**: 需要登录（通过 SaToken 自动获取当前用户）
- **Content-Type**: `application/json`

---

## 接口列表

### 1. 获取用户设置

获取当前登录用户的设置信息。

**请求**

```http
GET /user/setting
```

**响应**

- **成功 (200)**

```json
{
  "theme": "dark",
  "language": "zh-CN",
  "notifications": {
    "email": true,
    "push": false
  },
  "privacy": {
    "showEmail": false,
    "showPhone": false
  }
}
```

如果用户没有设置，返回空对象：

```json
{}
```

---

### 2. 保存/更新用户设置

保存或更新当前登录用户的设置信息。如果设置已存在则更新，不存在则创建。

**请求**

```http
PUT /user/setting
Content-Type: application/json
```

**请求体**

```json
{
  "theme": "dark",
  "language": "zh-CN",
  "notifications": {
    "email": true,
    "push": false
  },
  "privacy": {
    "showEmail": false,
    "showPhone": false
  }
}
```

> **说明**: 请求体可以是任意 JSON 对象，字段完全由前端自定义。

**响应**

- **成功 (200)**

```json
{
  "code": 200,
  "message": "success"
}
```

- **失败 (500)**

```json
{
  "code": 500,
  "message": "Internal Server Error"
}
```

---

### 3. 删除用户设置

删除当前登录用户的所有设置信息。

**请求**

```http
DELETE /user/setting
```

**响应**

- **成功 (200)**

```json
{
  "code": 200,
  "message": "success"
}
```

- **失败 (500)**

```json
{
  "code": 500,
  "message": "Internal Server Error"
}
```

---

## 使用示例

### JavaScript (Fetch API)

```javascript
// 1. 获取设置
async function getUserSetting() {
  const response = await fetch('/user/setting', {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer YOUR_TOKEN'
    }
  });
  const data = await response.json();
  console.log(data);
}

// 2. 保存设置
async function saveSetting() {
  const settings = {
    theme: 'dark',
    language: 'zh-CN',
    notifications: {
      email: true,
      push: false
    }
  };
  
  const response = await fetch('/user/setting', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer YOUR_TOKEN'
    },
    body: JSON.stringify(settings)
  });
  const result = await response.json();
  console.log(result);
}

// 3. 删除设置
async function deleteSetting() {
  const response = await fetch('/user/setting', {
    method: 'DELETE',
    headers: {
      'Authorization': 'Bearer YOUR_TOKEN'
    }
  });
  const result = await response.json();
  console.log(result);
}
```

### Axios

```javascript
import axios from 'axios';

// 1. 获取设置
const getSetting = async () => {
  const { data } = await axios.get('/user/setting');
  return data;
};

// 2. 保存设置
const saveSetting = async (settings) => {
  const { data } = await axios.put('/user/setting', settings);
  return data;
};

// 3. 删除设置
const deleteSetting = async () => {
  const { data } = await axios.delete('/user/setting');
  return data;
};
```



## 常见使用场景

### 场景 1: 主题设置

```json
{
  "theme": "dark",
  "fontSize": 14,
  "fontFamily": "Arial"
}
```

### 场景 2: 通知偏好

```json
{
  "notifications": {
    "comment": true,
    "like": true,
    "follow": false,
    "system": true
  }
}
```

### 场景 3: 隐私设置

```json
{
  "privacy": {
    "showEmail": false,
    "showPhone": false,
    "allowSearch": true,
    "showOnlineStatus": false
  }
}
```

### 场景 4: 编辑器配置

```json
{
  "editor": {
    "autoSave": true,
    "autoSaveInterval": 30,
    "spellCheck": true,
    "wordWrap": true,
    "lineNumbers": true
  }
}
```
