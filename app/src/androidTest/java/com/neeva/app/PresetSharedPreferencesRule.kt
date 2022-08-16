package com.neeva.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Forces the Neeva app to skip First Run or NeevaScope tooltip when it starts. */
class PresetSharedPreferencesRule(
    val skipFirstRun: Boolean = false,
    val skipNeevaScopeTooltip: Boolean = false
) : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val sharedPreferencesModel = SharedPreferencesModel(context)

                if (skipFirstRun) {
                    FirstRunModel.setFirstRunDone(sharedPreferencesModel)
                }

                if (skipNeevaScopeTooltip) {
                    SharedPrefFolder.App.ShowTryNeevaScopeTooltip.set(
                        sharedPreferencesModel,
                        false
                    )
                }

                base?.evaluate()
            }
        }
    }
}
