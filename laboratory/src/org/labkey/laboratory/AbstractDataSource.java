package org.labkey.laboratory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: bimber
 * Date: 1/18/13
 * Time: 7:46 AM
 */
abstract public class AbstractDataSource
{
    private final String _containerId;
    private final String _schemaName;
    private final String _queryName;
    private final String _label;
    protected static final String DELIM = "<>";
    protected static final Logger _log = LogManager.getLogger(AbstractDataSource.class);
    private final Map<String, TableInfo> _cachedTables = new HashMap<>();
    
    public AbstractDataSource(String label, @Nullable String containerId, String schemaName, String queryName)
    {
        _label = label;
        _containerId = containerId;
        _schemaName = schemaName;
        _queryName = queryName;
    }

    public String getContainerId()
    {
        return _containerId;
    }

    public String getSchemaName()
    {
        return _schemaName;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    protected QueryDefinition getQueryDef(Container c, User u)
    {
        Container target = getContainer();
        if (target == null)
            target = c;

        if (!target.hasPermission(u, ReadPermission.class))
            return null;

        UserSchema us = QueryService.get().getUserSchema(u, target, _schemaName);
        if (us == null)
            return null;

        QueryDefinition qd = us.getQueryDefForTable(_queryName);

        return qd;
    }

    public TableInfo getTableInfo(Container c, User u)
    {
        String key = getTargetContainer(c).getId() + "||" + u.getUserId();
        if (_cachedTables.containsKey(key))
        {
            return _cachedTables.get(key);
        }

        QueryDefinition qd = getQueryDef(c, u);
        if (qd == null)
            return null;

        List<QueryException> errors = new ArrayList<>();
        TableInfo ti = qd.getTable(errors,  true);
        _cachedTables.put(key, ti);

        return ti;
    }

    @Nullable
    public Container getContainer()
    {
        return _containerId == null ? null : ContainerManager.getForId(_containerId);
    }

    public Container getTargetContainer(Container c)
    {
        if (_containerId == null)
            return c;

        Container target = ContainerManager.getForId(_containerId);
        if (target == null)
        {
            _log.error("Invalid saved container for data source: " + getLabel() + ".  containerId was: " + _containerId);
            return c;
        }

        return target;
    }

    public String getLabel()
    {
        return _label == null ? _queryName : _label;
    }

    public JSONObject toJSON(Container c, User u, boolean includeTotals){
        JSONObject obj = new JSONObject();

        Container target = getContainer();
        if (target == null)
            target = c;

        obj.put("container", getContainerId() == null ? "" : target.getName());
        obj.put("containerPath", getContainerId() == null ? "" : target.getPath());
        obj.put("containerId", getContainerId() == null ? "" : target.getId());

        obj.put("schemaName", getSchemaName());
        obj.put("queryName", getQueryName());
        obj.put("label", getLabel());

        obj.put("canRead", target.hasPermission(u, ReadPermission.class));

        Container current = c.isWorkbookOrTab() ? c.getParent() : c;
        obj.put("fromCurrentContainer", getContainerId() == null || current.equals(target));

        if (includeTotals && target.hasPermission(u, ReadPermission.class))
        {
            TableInfo ti = getTableInfo(target, u);
            if (ti == null)
            {
                return new JSONObject();
            }

            //TODO: figure out how to handle substitutions
            TableSelector ts = new TableSelector(ti);
            try
            {
                long count = ts.getRowCount();
                obj.put("total", count);
            }
            catch (QueryService.NamedParameterNotProvided | DataIntegrityViolationException e)
            {
                _log.error(e.getMessage());
                obj.put("total", 0);
            }

            obj.put("browseUrl", QueryService.get().urlFor(u, target, QueryAction.executeQuery, getSchemaName(), getQueryName()));
        }

        return obj;
    }

    abstract public String getPropertyManagerKey();

    protected String getForKey(String input)
    {
        return input == null ? "" : input;
    }
}
