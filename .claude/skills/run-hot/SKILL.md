---
name: run-hot
description: >-
  啟動 Designer Shell 並開啟 Compose Hot Reload（開發 shell 自身 UI 用，存檔即熱抽換）。
  當使用者說「run-hot」「/run-hot」「hot reload 啟動 designer shell」
  「熱重載開發 designer shell」「跑 hot run」「開發 shell UI」時使用。
  一般啟動(去控制別的 app)請用 /run。
---

# 啟動 Designer Shell（Compose Hot Reload）

用 **Compose Hot Reload** 啟動根目錄的 **Designer Shell** —— 給「開發 shell 自身 Compose UI」用,
存檔後變更會即時熱抽換進執行中的視窗(`./gradlew hotRun --auto`)。

> 平常用 shell 去檢視 / 控制別的 app,請改用 **/run**(`./gradlew run`),不需要這個。

## 步驟

1. 用**背景方式**執行啟動腳本(它是會阻塞的桌面 app,視窗會一直開著直到關閉):

   ```bash
   scripts/run-designer-shell-hot.sh
   ```

   從 repo 根目錄(`.../jay-battle-product-helper`)執行,並設 `run_in_background: true`。

2. 輪詢該背景任務的輸出:
   - 看到 `> Task :hotRun`(視窗開啟)即代表啟動成功。
   - **第一次**執行會先用 foojay toolchain 佈建 JetBrains Runtime(JBR),可能下載數百 MB、需要較久,屬正常現象 —— 輪詢時請耐心等待,別過早判定失敗。
   - 若顯示 `BUILD FAILED` / exception,則回報錯誤。

3. 啟動後,直接編輯 `designer-shell/` 的 Compose 程式碼並存檔即可熱抽換;`--auto` 會自動偵測變更。
   (改 `@Composable` 函式體沒問題;改類別結構 / 非 Compose 狀態 / `main()` 仍需重啟。)
