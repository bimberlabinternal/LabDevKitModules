package org.labkey.ldk;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.FileSqlScriptProvider;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlScriptManager;
import org.labkey.api.data.SqlScriptRunner;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.util.ExceptionUtil;

import java.sql.Connection;

/**
 * User: bimber
 * Date: 7/27/13
 * Time: 7:13 AM
 */
public class LDKUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(LDKUpgradeCode.class);

    /** called at 12.33-12.34 */
    @SuppressWarnings({"UnusedDeclaration"})
    public void installNaturalize(final ModuleContext context)
    {
        try
        {
            SqlDialect dialect = LDKSchema.getInstance().getSqlDialect();

            // Run the install script only if dialect supports GROUP_CONCAT
            // this is a crude proxy, since they have the same base requirements
            if (dialect.isSqlServer() && dialect.supportsGroupConcat())
            {
                try
                {
                    // Attempt to use the function. If this succeeds, we'll skip the install step.
                    SqlExecutor executor = new SqlExecutor(LDKSchema.getInstance().getSchema());
                    executor.setLogLevel(Level.OFF);  // We expect this to fail in most cases... shut off data layer logging
                    executor.execute("SELECT x.G, ldk.Naturalize('Foo') FROM (SELECT 1 AS G) x GROUP BY G");
                    return;
                }
                catch (Exception e)
                {
                    //
                }

                DbSchema schema = LDKSchema.getInstance().getSchema();
                FileSqlScriptProvider provider = new FileSqlScriptProvider(ModuleLoader.getInstance().getModule(LDKModule.class));
                SqlScriptRunner.SqlScript script = new FileSqlScriptProvider.FileSqlScript(provider, schema, "naturalize_install.sql", "LDK");

                try (Connection conn = schema.getScope().getUnpooledConnection())
                {
                    SqlScriptManager.get(provider, schema).runScript(context.getUpgradeUser(), script, context, conn);
                }
            }
        }
        catch (Throwable t)
        {
            // The install script can fail for a variety of reasons, e.g., the database user lacks sufficient
            // permissions. If the automatic install fails then log and display the exception to admins, but continue
            // upgrading. Not having NATURALIZE is not a disaster; admin can install the function manually later.

            // Wrap the exception to provide an explanation to the admin
            Exception wrap = new Exception("Failure installing NATURALIZE function. This function is required for optimal operation of this server. Contact LabKey if you need assistance installing this function.", t);
            ExceptionUtil.logExceptionToMothership(null, wrap);
            ModuleLoader.getInstance().addModuleFailure(LDKModule.NAME, wrap);
        }
    }

}
