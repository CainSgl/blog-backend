# CDN WebSocket 配置指南

## 问题诊断

你的 WebSocket 连接失败 `wss://cainsgl.cn/api/ws/chat?token=xxx` 很可能是因为 **CDN 不支持或未正确配置 WebSocket**。

---

## 快速诊断

### 测试 1：绕过 CDN 直连

```bash
# 获取源站 IP
nslookup cainsgl.cn

# 修改本地 hosts 文件测试（Windows: C:\Windows\System32\drivers\etc\hosts）
# 添加一行：
your-server-ip  cainsgl.cn

# 然后测试 WebSocket 连接
# 如果能连上，说明是 CDN 的问题
```

### 测试 2：使用 curl 测试 WebSocket 握手

```bash
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  "https://cainsgl.cn/api/ws/chat?token=fdb97b3b-6267-4b0b-b38f-f181cb6ddc2b"
```

**期望结果：**
```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
```

**如果返回 502/504/400，说明 CDN 不支持 WebSocket。**

---

## 解决方案

### 方案一：启用 CDN 的 WebSocket 支持（推荐）

#### Cloudflare

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. 选择域名 `cainsgl.cn`
3. 进入 **Network** 标签
4. 找到 **WebSockets** 选项
5. 开启 **WebSockets**

**注意：** Cloudflare 免费版支持 WebSocket，但有连接时长限制（通常 100 秒）。

**解决连接时长限制：**
```javascript
// 客户端需要实现心跳保活
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
  }
}, 30000); // 每 30 秒发送一次
```

#### 阿里云 CDN

1. 登录 [阿里云控制台](https://cdn.console.aliyun.com/)
2. 进入 **CDN** → **域名管理**
3. 选择域名 `cainsgl.cn`
4. 点击 **配置** → **回源配置**
5. 找到 **WebSocket** 配置
6. 开启 **WebSocket**

#### 腾讯云 CDN

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/cdn)
2. 进入 **域名管理**
3. 选择域名 `cainsgl.cn`
4. 进入 **高级配置**
5. 找到 **WebSocket 配置**
6. 开启 **WebSocket**

#### 七牛云 CDN

1. 登录 [七牛云控制台](https://portal.qiniu.com/)
2. 进入 **CDN** → **域名管理**
3. 选择域名
4. 在 **高级配置** 中开启 **WebSocket**

---

### 方案二：使用独立域名（强烈推荐）

**为 WebSocket 单独配置一个不走 CDN 的域名。**

#### 步骤 1：添加 DNS 记录

在你的 DNS 服务商（如 Cloudflare、阿里云 DNS）添加：

```
类型: A
主机记录: ws
记录值: your-server-ip
TTL: 600
代理状态: 仅 DNS（不走 CDN）
```

**Cloudflare 特别注意：**
- 添加记录时，确保云朵图标是 **灰色**（DNS only）
- 灰色 = 不走 CDN，直连源站
- 橙色 = 走 CDN

#### 步骤 2：修改 K3s Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: websocket-ingress
  namespace: your-namespace
  annotations:
    nginx.ingress.kubernetes.io/websocket-services: "ai-service"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"  # 如果需要 HTTPS
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - ws.cainsgl.cn
      secretName: ws-cainsgl-cn-tls
  rules:
    - host: ws.cainsgl.cn
      http:
        paths:
          - path: /api/ws/chat
            pathType: Prefix
            backend:
              service:
                name: ai-service  # 或者 gateway-service
                port:
                  number: 8080
```

#### 步骤 3：更新客户端连接地址

```javascript
// 修改前
const ws = new WebSocket('wss://cainsgl.cn/api/ws/chat?token=' + token);

// 修改后
const ws = new WebSocket('wss://ws.cainsgl.cn/api/ws/chat?token=' + token);
```

**优点：**
- 不受 CDN 限制
- 连接更稳定
- 延迟更低
- 不消耗 CDN 流量

---

### 方案三：使用 CDN 的 WebSocket 专用加速

某些 CDN 提供商提供专门的 WebSocket 加速服务：

#### 阿里云全站加速 DCDN

1. 开通 [全站加速 DCDN](https://dcdn.console.aliyun.com/)
2. 添加域名 `ws.cainsgl.cn`
3. 配置 WebSocket 加速
4. 回源协议选择 **WebSocket**

#### 腾讯云 ECDN

1. 开通 [ECDN](https://console.cloud.tencent.com/ecdn)
2. 添加域名
3. 开启 WebSocket 支持

---

## K3s 完整配置示例

### 1. Service 配置

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ai-service
  namespace: default
spec:
  selector:
    app: ai
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      protocol: TCP
  type: ClusterIP
```

### 2. Ingress 配置（推荐方案）

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ai-ingress
  namespace: default
  annotations:
    # WebSocket 支持
    nginx.ingress.kubernetes.io/websocket-services: "ai-service"
    # 超时设置（重要！）
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
    # 保持连接
    nginx.ingress.kubernetes.io/proxy-http-version: "1.1"
    nginx.ingress.kubernetes.io/configuration-snippet: |
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
    # HTTPS 证书
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - ws.cainsgl.cn
      secretName: ws-cainsgl-cn-tls
  rules:
    - host: ws.cainsgl.cn
      http:
        paths:
          - path: /api/ws/chat
            pathType: Prefix
            backend:
              service:
                name: ai-service
                port:
                  number: 8080
```

### 3. 如果通过 Gateway 转发

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gateway-ingress
  annotations:
    nginx.ingress.kubernetes.io/websocket-services: "gateway-service"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
spec:
  rules:
    - host: ws.cainsgl.cn
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: gateway-service
                port:
                  number: 8080
```

**Gateway 路由配置（Spring Cloud Gateway）：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ai-websocket
          uri: lb:ws://ai-service  # 注意：使用 ws:// 协议
          predicates:
            - Path=/api/ws/chat
          filters:
            - StripPrefix=0
```

---

## 验证配置

### 1. 检查 Ingress 状态

```bash
kubectl get ingress -n default
kubectl describe ingress ai-ingress -n default
```

### 2. 查看 Nginx Ingress 日志

```bash
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=100 -f
```

### 3. 测试 WebSocket 连接

```bash
# 安装 wscat
npm install -g wscat

# 测试连接
wscat -c "wss://ws.cainsgl.cn/api/ws/chat?token=your-token"

# 如果连接成功，会显示：
# Connected (press CTRL+C to quit)

# 发送心跳测试
> ping
< pong
```

### 4. 浏览器测试

```javascript
const ws = new WebSocket('wss://ws.cainsgl.cn/api/ws/chat?token=your-token');

ws.onopen = () => console.log('✅ 连接成功');
ws.onerror = (e) => console.error('❌ 连接失败', e);
ws.onclose = (e) => console.log('连接关闭', e.code, e.reason);

// 测试心跳
ws.onopen = () => {
  ws.send('ping');
};

ws.onmessage = (e) => {
  console.log('收到消息:', e.data);
};
```

---

## 常见错误及解决

### 错误 1: 502 Bad Gateway

**原因：**
- CDN 不支持 WebSocket
- Nginx Ingress 未正确配置

**解决：**
```bash
# 检查 Ingress 配置
kubectl get ingress ai-ingress -o yaml

# 确保有以下 annotations：
# nginx.ingress.kubernetes.io/websocket-services: "ai-service"
```

### 错误 2: 连接立即断开（1006）

**原因：**
- Token 无效
- 认证失败
- 服务端错误

**解决：**
```bash
# 查看 AI 服务日志
kubectl logs -n default -l app=ai --tail=50

# 检查 token 是否有效
curl -H "Authorization: Bearer your-token" https://cainsgl.cn/api/user/info
```

### 错误 3: 连接超时

**原因：**
- 防火墙阻止
- 端口未开放
- DNS 解析错误

**解决：**
```bash
# 检查 DNS 解析
nslookup ws.cainsgl.cn

# 检查端口连通性
telnet ws.cainsgl.cn 443

# 检查 Service 端点
kubectl get endpoints ai-service -n default
```

### 错误 4: Cloudflare 100 秒断开

**原因：** Cloudflare 免费版有 100 秒连接限制

**解决：**
```javascript
// 实现心跳保活
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
  }
}, 30000); // 每 30 秒
```

或者使用独立域名绕过 Cloudflare。

---

## 推荐配置总结

### 最佳实践

1. **使用独立域名** `ws.cainsgl.cn`
2. **不走 CDN**（DNS only）
3. **配置 HTTPS**（Let's Encrypt）
4. **实现心跳保活**（30 秒间隔）
5. **实现断线重连**（指数退避）

### DNS 配置

```
# 主站（走 CDN）
cainsgl.cn        A    CDN-IP        代理: 开启
www.cainsgl.cn    A    CDN-IP        代理: 开启

# WebSocket（不走 CDN）
ws.cainsgl.cn     A    源站-IP       代理: 关闭
```

### Nginx Ingress 关键配置

```yaml
annotations:
  nginx.ingress.kubernetes.io/websocket-services: "ai-service"
  nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
  nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
```

---

## 运维检查清单

- [ ] DNS 记录已添加（ws.cainsgl.cn）
- [ ] DNS 不走 CDN（仅 DNS 模式）
- [ ] K3s Ingress 已配置 WebSocket 支持
- [ ] HTTPS 证书已配置
- [ ] 防火墙已开放 443 端口
- [ ] 服务端应用正常运行
- [ ] 客户端实现了心跳保活
- [ ] 客户端实现了断线重连
- [ ] 已测试连接成功

---

## 监控建议

### 1. 添加健康检查

```yaml
# Deployment 中添加
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### 2. 监控 WebSocket 连接数

```kotlin
// 在 ChatWebSocketHandler 中添加
@Scheduled(fixedRate = 60000)
fun logConnectionStats() {
    logger.info { "当前 WebSocket 连接数: ${userSessions.size}" }
}
```

### 3. 日志监控

```bash
# 实时查看 WebSocket 日志
kubectl logs -n default -l app=ai -f | grep -i websocket
```

---

## 技术支持

如果按照以上步骤配置后仍然无法连接，请提供：

1. 浏览器控制台错误信息
2. Nginx Ingress 日志
3. AI 服务日志
4. DNS 解析结果
5. curl 测试结果

联系技术团队进行排查。
