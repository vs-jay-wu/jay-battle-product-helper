---
name: run
description: >-
  啟動 standalone Designer Shell（通用控制面板 + 專案 picker，含 inspector）。
  當使用者說「run」「run designer-shell」「/run」「啟動 designer shell」
  「開 designer shell」「跑 designer shell」時使用。
---

# 啟動 Designer Shell

啟動根目錄的 **Designer Shell** —— 通用控制面板 + 專案 picker（核心模組 `designer-shell/`）。
**不要**啟動 `examples/ragdoll-cat/designer-shell`（那是 Compose target host,不是 picker）。

啟動後它會掃描 `examples/` 底下有 `.designer-shell.json` 的範例 app,列在 RepoPicker
(目前是 flutter_shop + ragdoll-cat)。

## 步驟

1. 用**背景方式**執行啟動腳本(它是會阻塞的桌面 app,視窗會一直開著直到關閉):

   ```bash
   scripts/run-designer-shell.sh
   ```

   從 repo 根目錄(`.../jay-battle-product-helper`)執行,並設 `run_in_background: true`。

2. 輪詢該背景任務的輸出,看到 `> Task :run` / `discovered=[...]` 即代表 picker 視窗已開,
   接著回報掃到了哪些專案。若改顯示 `BUILD FAILED` / exception,則回報錯誤。
