# jay-battle-product-helper

Demo / 紀錄用的 helper repo。`ragdoll-cat/` 是 demo sandbox（見 `ragdoll-cat/DEMO_NOTICE.md`）。

## 工作習慣：完成後自動 commit + push

完成一項工作後，**若不需要使用者額外確認**，直接 `git commit` 並 `git push`，不用再問。

**「不需要確認」指：** 改動明確、已驗證可行、沒有破壞性或模稜兩可的決策。符合時就：
1. 寫清楚的 commit message（結尾照 harness 規定加 `Co-Authored-By`）。
2. commit 後 push 到當前分支所追蹤的 upstream（本 repo 即 `origin/main`）。

**護欄（以下情況仍要先問）：**
- 破壞性或難回復的操作（force-push、改寫歷史、刪遠端分支、`git reset --hard` 等）→ **永遠先確認**，且不要 `--force` push。
- 改動本身就需要使用者決策、或結果模稜兩可、或牽涉 repo 以外的對外動作 → 先確認。
- 只 push 當前分支到它既有的 upstream；不主動建立 / 切換遠端分支。
- 機敏檔由 `.gitignore` 把關；push 前確認沒有未加密金鑰 / 憑證被加入。

此授權對本 repo 持續有效，直到使用者另行指示。
