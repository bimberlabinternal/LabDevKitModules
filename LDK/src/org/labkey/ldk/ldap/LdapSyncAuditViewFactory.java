package org.labkey.ldk.ldap;

import org.labkey.api.audit.AuditLogEvent;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.SimpleAuditViewFactory;
import org.labkey.api.audit.query.AuditLogQueryView;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserIdForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 1/21/13
 * Time: 10:06 PM
 */
public class LdapSyncAuditViewFactory extends SimpleAuditViewFactory
{
    private static final LdapSyncAuditViewFactory _instance = new LdapSyncAuditViewFactory();

    public static LdapSyncAuditViewFactory getInstance()
    {
        return _instance;
    }

    private LdapSyncAuditViewFactory()
    {

    }

    public String getEventType()
    {
        return LdapSyncRunner.AUDIT_EVENT_TYPE;
    }

    public String getName()
    {
        return "LDAP Sync Events";
    }

    @Override
    public String getDescription()
    {
        return "Contains the history LDAP sync events and summary of changes made";
    }

    public QueryView createDefaultQueryView(ViewContext context)
    {
        return createHistoryView(context, null);
    }

    public static void addAuditEntry(User user, int usersAdded, int usersRemoved, int usersInactivated, int usersModified, int groupsAdded, int groupsRemoved, int membershipsAdded, int membershipsRemoved)
    {
        AuditLogEvent event = new AuditLogEvent();
        event.setContainerId(ContainerManager.getRoot().getId());
        event.setEventType(LdapSyncRunner.AUDIT_EVENT_TYPE);
        event.setCreatedBy(user);

        String comment = String.format("LDAP Sync Summary: users added: %s, users removed: %s, users inactivated: %s, users modified: %s, groups added: %s, groups removed: %s, memberships added: %s, memberships removed: %s", usersAdded, usersRemoved, usersInactivated, usersModified, groupsAdded, groupsRemoved, membershipsAdded, membershipsRemoved);
        event.setComment(comment);
        event.setIntKey1(usersAdded + groupsAdded);
        event.setIntKey2(usersRemoved + groupsRemoved);
        event.setIntKey3(membershipsAdded + membershipsRemoved);
        event.setCreated(new Date());
        AuditLogService.get().addEvent(event);
    }

    public AuditLogQueryView createHistoryView(ViewContext context, SimpleFilter extraFilter)
    {
        SimpleFilter filter = new SimpleFilter();

        if (null != extraFilter)
            filter.addAllClauses(extraFilter);

        AuditLogQueryView view = AuditLogService.get().createQueryView(context, filter, getEventType());
        view.setSort(new Sort("-Date"));

        return view;
    }

    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> columns = new ArrayList<>();

        columns.add(FieldKey.fromParts("Date"));
        columns.add(FieldKey.fromParts("Comment"));
        columns.add(FieldKey.fromParts("IntKey1"));
        columns.add(FieldKey.fromParts("IntKey2"));
        columns.add(FieldKey.fromParts("IntKey3"));

        return columns;
    }

    public void setupTable(FilteredTable table, UserSchema schema)
    {
        super.setupTable(table, schema);
        ColumnInfo col = table.getColumn("IntKey1");
        if (col != null)
        {
            col.setLabel("Total Users/Groups Added");
        }

        ColumnInfo col2 = table.getColumn("IntKey2");
        if (col2 != null)
        {
            col2.setLabel("Total Users/Groups Removed");
        }

        ColumnInfo col3 = table.getColumn("IntKey3");
        if (col3 != null)
        {
            col3.setLabel("Total Group Members Added or Removed");
        }
    }
}
