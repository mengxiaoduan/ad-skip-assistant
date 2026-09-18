# ⏭️ 广告跳过助手 (AdSkip Assistant)

一个安卓小工具：当任何应用弹出带「跳过」按钮的开屏广告时，自动帮你点掉，**点开就是主界面**。

**下载安装**：https://mengxiaoduan.github.io/ad-skip-assistant/ （页面内有 APK 下载与安装教程）

## 工作原理

基于 Android 无障碍服务（`AccessibilityService`）：

- 监听全局窗口变化（`TYPE_WINDOW_STATE_CHANGED` / `TYPE_WINDOW_CONTENT_CHANGED`）
- 节流 + 防抖扫描当前窗口节点树
- 匹配规则：文本或 contentDescription 含「跳过 / skip」（长度 ≤ 12）或 viewId 含 `skip`，且节点尺寸不超过半屏宽、四分之一屏高（**防误触**：全屏的"跳过"引导页不会被碰）
- 命中后向上寻找 4 层以内可点击节点执行 `ACTION_CLICK`，并做统计 + Toast 提示

内置「模拟广告自测」页面（右上角跳过按钮 + 倒计时），安装后无需真实广告即可验证效果。

## 特性

- ✅ 自动点击开屏广告「跳过」按钮
- ✅ 一键暂停/恢复（右上角开关，实时生效）
- ✅ 跳过次数统计
- ✅ 不申请网络权限，不收集任何数据
- ✅ 误触保护：只点"跳过"，绝不乱点

## 安装要求

- Android 7.0+（API 24）
- 下载 APK 直接安装（允许未知来源），并在系统「无障碍」设置中开启服务

## 构建

推送即自动构建：GitHub Actions 会产出 Debug APK 并发布到 Releases。
手动构建：`gradle -p android assembleDebug`

## 免责声明

本工具仅替代用户本人点击广告界面自身提供的「跳过」按钮，不修改、不绕过、不破坏任何应用的正常功能，仅供个人设备自用与学习交流；请遵守您设备上各应用的用户协议及当地法律法规。

## 许可

MIT
