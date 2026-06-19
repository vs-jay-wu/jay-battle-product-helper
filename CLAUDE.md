# jay-battle-product-helper

Demo / 紀錄用的 helper repo。`ragdoll-cat/` 是 demo sandbox（見 `ragdoll-cat/DEMO_NOTICE.md`）。

## Git 規則：可自動 commit，但**禁止主動 push**

> ⚠️ 最重要：**agent 一律不得自己 `git push`。** 只有在使用者**當下明確要求**(例如說「push」「push it」「推上去」「push 上去」)時才可以 push。即使改動已 commit、已驗證、看起來完全沒問題,也**不要主動 push** —— 使用者常在實驗 / 測試階段,自動 push 會造成困擾。這條優先於任何「完成就收尾」的直覺。

完成一項工作後,**若不需要使用者額外確認**(改動明確、已驗證可行、無破壞性或模稜兩可的決策),可以直接 `git commit`(commit message 結尾照 harness 規定加 `Co-Authored-By`),不用先問。**但 commit 後停在本地,不要 push。**

**護欄(以下情況連 commit 都要先問):**
- 破壞性或難回復的操作(force-push、改寫歷史、刪遠端分支、`git reset --hard` 等)→ **永遠先確認**。
- 改動本身需要使用者決策、或結果模稜兩可、或牽涉 repo 以外的對外動作 → 先確認。
- 機敏檔由 `.gitignore` 把關;commit 前確認沒有未加密金鑰 / 憑證被加入。

此規則對本 repo(含 `ragdoll-cat/`)持續有效,直到使用者另行指示。
