package org.labkey.laboratory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.SimpleModuleContainerListener;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.HttpView;
import org.labkey.laboratory.query.LaboratoryWorkbooksTable;
import org.labkey.laboratory.query.WorkbookModel;

import java.beans.PropertyChangeEvent;

/**
 * User: bimber
 * Date: 1/31/13
 * Time: 7:19 PM
 */
public class LaboratoryContainerListener extends SimpleModuleContainerListener
{
    Logger _log = LogManager.getLogger(LaboratoryContainerListener.class);

    public LaboratoryContainerListener(Module owner)
    {
        super(owner);
    }

    @Override
    public void containerCreated(Container c, User user)
    {
        super.containerCreated(c, user);

        if (c.isWorkbook())
        {
            if (c.getParent().getActiveModules().contains(ModuleLoader.getInstance().getModule(LaboratoryModule.class)))
            {
                try
                {
                    LaboratoryManager.get().initLaboratoryWorkbook(c, user);
                }
                catch (Exception e)
                {
                    _log.error("Unable to update laboratory workbooks table", e);
                }
            }
        }

        //attempt to populate default values on load
        if (user != null && !c.isWorkbook() && c.getActiveModules().contains(ModuleLoader.getInstance().getModule(LaboratoryModule.class)))
        {
            try
            {
                LaboratoryManager.get().populateDefaultData(user, c, null);
            }
            catch (IllegalArgumentException e)
            {
                _log.warn("Unable to populate default values for laboratory module", e);
            }
        }
    }

    @Override
    public void containerMoved(Container c, Container oldParent, User u)
    {
        super.containerMoved(c, oldParent, u);

        if (c.isWorkbook())
        {
            //determine if we have a record of this workbook, and delete if true:
            TableInfo ti = LaboratorySchema.getInstance().getTable(LaboratorySchema.TABLE_WORKBOOKS);
            TableSelector ts = new TableSelector(ti, new SimpleFilter(FieldKey.fromString(LaboratoryWorkbooksTable.WORKBOOK_COL), c.getId()), null);
            if (ts.exists())
            {
                Table.delete(ti, c.getId());
            }

            //then re-register
            if (c.getActiveModules().contains(ModuleLoader.getInstance().getModule(LaboratoryModule.class)))
            {
                try
                {
                    LaboratoryManager.get().initLaboratoryWorkbook(c, u);
                }
                catch (Exception e)
                {
                    _log.error("Unable to init laboratory workbook", e);
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);

        if (evt.getPropertyName().equals(ContainerManager.Property.Name.name()))
        {
            updateWorkbookTableOnNameChange(evt);
        }
        else if (evt.getPropertyName().equals(ContainerManager.Property.Modules.name()))
        {
            possiblyInitializeOnActiveModuleChange(evt);
        }
    }

    @Override
    protected void purgeTable(UserSchema userSchema, TableInfo table, Container c)
    {
        if (table.getName().equalsIgnoreCase(LaboratorySchema.TABLE_WORKBOOKS))
        {
            SQLFragment sql = new SQLFragment("DELETE FROM laboratory.workbooks WHERE " +
                    (c.isWorkbook() ? LaboratoryWorkbooksTable.WORKBOOK_COL : LaboratoryWorkbooksTable.PARENT_COL) +
                    " = ?", c.getId());
            new SqlExecutor(table.getSchema()).execute(sql);
        }
        else
        {
            super.purgeTable(userSchema, table, c);
        }
    }

    /**
     * The container name field stores the workbookId as a string. If that name changes (and this should no longer be permitted
     * for the most part in LK, we need to update this table
     */
    private void updateWorkbookTableOnNameChange(PropertyChangeEvent evt)
    {
        if (!(evt instanceof ContainerManager.ContainerPropertyChangeEvent))
        {
            return;
        }

        ContainerManager.ContainerPropertyChangeEvent ce = (ContainerManager.ContainerPropertyChangeEvent) evt;
        if (!ce.container.isWorkbook())
        {
            return;
        }

        TableInfo ti = LaboratorySchema.getInstance().getTable(LaboratorySchema.TABLE_WORKBOOKS);
        TableSelector ts = new TableSelector(ti, new SimpleFilter(FieldKey.fromString(LaboratoryWorkbooksTable.WORKBOOK_COL), ce.container.getId()), null);
        if (ts.exists())
        {
            try
            {
                Integer workbookId = Integer.parseInt(ce.container.getName());
                WorkbookModel w = ts.getObject(ce.container.getId(), WorkbookModel.class);
                w.setWorkbookId(workbookId);
                Table.update(ce.user, ti, w, ce.container.getId());
            }
            catch (NumberFormatException e)
            {
                _log.error("Non-numeric workbook name: " + ce.container.getName() + " for: " + ce.container.getEntityId());
            }
        }
    }

    /**
     * The intent of this is to initialize the laboratory folder if the set of active modules
     * changes to include Laboratory.  This should only occur on the parent folder, not individual workbooks.
     */
    private void possiblyInitializeOnActiveModuleChange(PropertyChangeEvent evt)
    {
        if (!(evt instanceof ContainerManager.ContainerPropertyChangeEvent))
        {
            return;
        }

        ContainerManager.ContainerPropertyChangeEvent ce = (ContainerManager.ContainerPropertyChangeEvent)evt;

        //Only make these changes from the parent container for performance reasons
        if (ce.container.isWorkbook())
        {
            return;
        }

        User u = ce.user;
        if (u == null && HttpView.hasCurrentView())
        {
            u = HttpView.currentView().getViewContext().getUser();
        }

        if (u == null || !ce.container.hasPermission(u, InsertPermission.class))
        {
            return;
        }

        if (ce.container.getActiveModules().contains(ModuleLoader.getInstance().getModule(LaboratoryModule.class)))
        {
            try
            {
                LaboratoryManager.get().recursivelyInitWorkbooksForContainer(u, ce.container);
            }
            catch (Exception e)
            {
                _log.error("Unable to update laboratory workbooks table", e);
            }

            try
            {
                LaboratoryManager.get().populateDefaultData(u, ce.container, null);
            }
            catch (Exception e)
            {
                _log.error("Unable to populate defaults in laboratory module tables", e);
            }
        }
    }
}
