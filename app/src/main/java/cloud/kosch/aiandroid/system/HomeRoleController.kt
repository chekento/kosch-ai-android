package cloud.kosch.aiandroid.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

object HomeRoleController {
    fun isDefaultHome(context: Context): Boolean = context
        .getSystemService(RoleManager::class.java)
        .isRoleHeld(RoleManager.ROLE_HOME)

    fun requestIntent(context: Context): Intent? {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
}

