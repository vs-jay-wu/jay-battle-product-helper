package com.viewsonic.classswift.data.clientapp

sealed class ClientAppInfo(val packageName: String, val bindIntentAction: String, val launchIntentAction: String) {
    object NotAllowed : ClientAppInfo("", "", "")
    object AppMyViewBoard : ClientAppInfo(
        "com.viewsonic.droid",
        "com.viewsonic.classswift.service.BIND_FROM_MYVIEWBOARD",
        "com.viewsonic.classswift.service.LAUNCH_FROM_MYVIEWBOARD"
    )

    companion object {
        private fun allowList(): List<ClientAppInfo> = listOfNotNull(AppMyViewBoard)

        fun findClientByPackageNames(names: List<String>): ClientAppInfo {
            val packageNames = names.map { it.trim() }
            return allowList().firstOrNull { client ->
                packageNames.contains(client.packageName)
            } ?: NotAllowed
        }

        fun findClientByLaunchAction(action: String): ClientAppInfo {
            val intentAction = action.trim()
            if (intentAction.isEmpty()) return NotAllowed
            return allowList().firstOrNull { client ->
                client.launchIntentAction == intentAction
            } ?: NotAllowed
        }

        fun findClientByBindAction(action: String): ClientAppInfo {
            val intentAction = action.trim()
            if (intentAction.isEmpty()) return NotAllowed
            return allowList().firstOrNull { client ->
                client.bindIntentAction == intentAction
            } ?: NotAllowed
        }
    }
}
