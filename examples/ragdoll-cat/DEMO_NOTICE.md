# ⚠️ 這是 Demo Repo — 不會影響實際產品

這個 `ragdoll-cat/` 是 **demo / sandbox 用的副本**，純粹用來示範與大改實驗。
**這裡的任何改動都不會影響到實際產品或上游正式 repo。**

## 為什麼安全

| 項目 | 說明 |
|------|------|
| **獨立於上游** | 原本的 `.git` 已被移除。這份副本與上游正式 repo（`github.com/Viewsonic-EDU/ragdoll-cat`）**沒有任何 git 連結**，不會、也無法 push 回去。 |
| **納入 demo helper repo** | 本資料夾現在由外層的 `jay-battle-product-helper` repo 追蹤，僅供 demo 紀錄使用，與產品出版流程無關。 |
| **可任意大改** | 為了 demo 可以放心重構、改寫、刪改任何程式碼，不影響正式版本。 |

## 來源

- 上游正式 repo：`github.com/Viewsonic-EDU/ragdoll-cat`（**唯一的真實來源 / source of truth**）
- 本副本：上游某時間點的快照，移除 `.git` 後作為 demo 使用。
- 若需要最新的正式程式碼，請回到上游 repo，**不要**把這份 demo 副本當成正式版本。

## 敏感資訊處理

進版控時已排除所有機敏檔（由外層 `.gitignore` 與本資料夾 `.gitignore` 共同把關）：

- Keystore：`*.jks`、`*.keystore`（含 `cs_debug.keystore`）、`keystore.properties`
- 設定：`local.properties`、`google-services.json`
- 個人憑證 symlink：`.env`、`.mcp.json`（指向個人 credential 路徑）
- `cs_release.keystore.gpg` 為 GPG 加密檔，刻意保留（安全）

## 請勿

- ❌ 不要把這份 demo 副本的改動回推到上游正式 repo。
- ❌ 不要把這裡當成最新正式程式碼的依據。
- ❌ 不要在這裡提交任何未加密的金鑰或憑證。
