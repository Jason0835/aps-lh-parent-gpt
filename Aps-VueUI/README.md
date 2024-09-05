## 开发
本项目使用 pnpm 进行包管理

```bash

# 安装依赖
pnpm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
# 启动服务
pnpm dev
```
## 发布

```bash
# 构建测试环境
pnpm build:stage

# 构建生产环境
pnpm build:prod
```
/test 在 vue.config.js 的 publicPath 中可以修改 

部署到 nginx 上
```
  # 前端静态资源路径 /test 可以看需求替换
  location /test {
    add_header Access-Control-Allow-Origin *;
    gzip on; #开启或关闭gzip on off
    gzip_min_length 100k; #gzip压缩最小文件大小，超出进行压缩（自行调节）
    gzip_buffers 4 16k; #buffer 不用修改
    gzip_comp_level 8; #压缩级别:1-10，数字越大压缩的越好，时间也越长
    gzip_types text/plain application/x-javascript text/css application/xml text/javascript application/x-httpd-php image/jpeg image/gif image/png; # 压缩文件类型
    gzip_static on;
    alias /Users/yongqiu/dev/codes/tlt-work/telecom-ui/dist/;
    index index.html;
    try_files $uri $uri/ @router-tlt;
  }
  location @router-tlt {
    rewrite ^.*$ /index.html last;
  }
  # api 请求 ，根路径 /mps 在 .env.production 中修改
  location /mps/ {
    proxy_pass http://192.168.30.159/;
  }
```