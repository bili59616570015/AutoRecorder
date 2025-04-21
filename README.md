# 自动录屏（AutoRecorder）
检测抖音直播间状态，主播开播后自动打开直播间开启录屏，主播下播后自动上传录屏视频至B站

## OS
Android 10以上

## 思路
参考[Biliup](https://github.com/biliup/biliup)检测抖音直播间状态，使用[ADB库](https://github.com/MuntashirAkon/libadb-android)打开直播间，结束录屏后参考Biliup上传视频致B站

## 使用方法
* 需要连接ADB（有一定难度），连接录制，设置主播，才能开启巡逻。
* 可以设置B站上传模版，如果不设置则仅录屏不上传。录制视频在/Movies/AutoRecorder/AutoRecorder_files/下面，可以自己手动用官方app进行上传。
* 锁屏时无法录屏，可以在进阶设置设置唤醒屏幕（需预设无密码）。
* 菜单>上传可看到上传进度。如果中断了可以按按钮继续上传。
https://github.com/bili59616570015/AutoRecorder/releases/download/1.0/app-release-1.0.apk

## 叠甲
* 只测试了Android10和Android11
* Android15有可能只能检测直播间状态6小时
* 有可能崩溃导致录屏不成功
* 手机卡顿时录屏好像会导致音画不同步
* 好像只能录竖屏
* 长时间使用可能导致设备寿命快速缩短
* 本项目仅供学习交流使用，低调使用比较好，如果官方API改变有可能不能用
* 因使用本项目导致的任何直接或间接损失（包括数据泄露、数据丢失、设备损坏、法律纠纷等），概不负责
* 没什么时间维护，bug不一定改

## 捐赠
[爱发电](https://afdian.com/a/bili59616570015)
