package org.labkey.ldk.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.MemTracker;
import org.labkey.api.util.PageFlowUtil;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: bimber
 * Date: 6/27/13
 * Time: 5:40 PM
 */
public class LookupValidationHelper
{
    private Container _container;
    private final User _user;
    private final TableInfo _table;
    private static final Logger _log = LogManager.getLogger(LookupValidationHelper.class);

    private final Map<String, UserSchema> _userSchemaMap = new HashMap<String, UserSchema>();
    private final Map<String, Map<String, String>> _allowableValueMap = new HashMap<String, Map<String, String>>();

    private LookupValidationHelper(String containerId, int userId, String schemaName, String queryName)
    {
        _container = ContainerManager.getForId(containerId);
        if (_container == null)
            throw new IllegalArgumentException("Unknown container: " + containerId);

        _container = _container.isWorkbook() ? _container.getParent() : _container;

        _user = UserManager.getUser(userId);
        if (_user == null)
            throw new IllegalArgumentException("Unknown user: " + userId);

        UserSchema us = getUserSchema(schemaName);
        if (us == null)
            throw new IllegalArgumentException("Unknown schema: " + schemaName);

        _table = us.getTable(queryName);
        if (_table == null)
            throw new IllegalArgumentException("Unknown table: " + schemaName + "." + queryName);

        MemTracker.getInstance().put(this);
    }

    public static LookupValidationHelper create(String containerId, int userId, String schemaName, String queryName)
    {
        return new LookupValidationHelper(containerId, userId, schemaName, queryName);
    }

    private UserSchema getUserSchema(String name)
    {
        if (_userSchemaMap.containsKey(name))
            return _userSchemaMap.get(name);

        UserSchema us = QueryService.get().getUserSchema(_user, _container, name);
        _userSchemaMap.put(name, us);

        return us;
    }

    /**
     * Provides a case-normalized lookup of the passed value.  Will return null is the passed value is not found in allowable values
     */
    public Object getLookupValue(String value, String fieldName)
    {
        value = StringUtils.trimToNull(value);
        if (value == null)
            return value;

        ColumnInfo ci = _table.getColumn(FieldKey.fromString(fieldName));
        if (ci == null)
            throw new IllegalArgumentException("Unknown column: " + fieldName);

        Map<String, String> allowable = getAllowableValues(ci);
        if (allowable == null)
            return null;

        return allowable.get(value);
    }

    public String validateRequiredField(String fieldName, Object value)
    {
        ColumnInfo ci = _table.getColumn(FieldKey.fromString(fieldName));
        if (ci == null)
            return null;

        //NOTE: some required fields are auto-completed, like LSID or rowIds, so skip them here
        if (!ci.isNullable() && ci.isUserEditable() && (value == null || StringUtils.isEmpty(String.valueOf(value))))
            return "The field: " + ci.getLabel() + " is required";

        return null;
    }

    private Map<String, String> getAllowableValues(ColumnInfo ci)
    {
        String name = ci.getName();
        if (_allowableValueMap.containsKey(name))
            return _allowableValueMap.get(name);

        TableInfo fkTableInfo = ci.getFkTableInfo();
        if (fkTableInfo == null)
            return null;

        ColumnInfo targetCol = fkTableInfo.getColumn(ci.getFk().getLookupColumnName());
        if (targetCol == null)
            return null;

        try
        {
            TableSelector ts = new TableSelector(fkTableInfo, PageFlowUtil.set(targetCol), null, null);
            String[] vals = ts.getArray(String.class);
            if (vals != null)
            {
                Map<String, String> map = new CaseInsensitiveHashMap<>();
                for (String val : vals)
                {
                    map.put(val, val);
                }
                _allowableValueMap.put(name, map);
            }
            else
                _allowableValueMap.put(name, null);

            return _allowableValueMap.get(name);
        }
        catch (DeadlockLoserDataAccessException e)
        {
            _log.error("DeadlockLoserException in LookupValidationHelper for table: " + _table.getPublicSchemaName() + "." +  _table.getPublicName(), e);
            throw e;
        }
    }

    public void cascadeUpdate(String targetSchema, String targetTable, String targetField, Object newVal, Object oldVal) throws Exception
    {
        UserSchema us = QueryService.get().getUserSchema(_user, _container, targetSchema);
        if (us == null)
            throw new IllegalArgumentException("Unknown schema: " + targetSchema);

        TableInfo ti = us.getTable(targetTable);
        if (ti == null)
            throw new IllegalArgumentException("Unknown table: " + targetTable);

        if (ti.getColumn(targetField) == null)
            throw new IllegalArgumentException("Unknown column: " + targetField + " in table: " + targetTable);

        if (ti.getPkColumnNames().size() != 1)
            throw new IllegalArgumentException("Cascade update only supported for columns with single PKs.  Problem table was : " + targetTable);

        Set<String> pkCols = new HashSet<>(ti.getPkColumnNames());

        TableSelector ts = new TableSelector(ti, pkCols, new SimpleFilter(FieldKey.fromString(targetField), oldVal), null);
        Collection<Map<String, Object>> pks = ts.getMapCollection();

        List<Map<String, Object>> toUpdate = new ArrayList<>();
        List<Map<String, Object>> oldPKs = new ArrayList<>();
        for (Map<String, Object> oldPk : pks)
        {
            CaseInsensitiveHashMap<Object> map = new CaseInsensitiveHashMap<>();
            map.put(targetField, newVal);
            map.putAll(oldPk);
            toUpdate.add(map);

            oldPKs.add(oldPk);
        }

        QueryUpdateService qus = ti.getUpdateService();
        qus.updateRows(_user, _container, toUpdate, oldPKs, null, new HashMap<String, Object>());
    }

    public boolean verifyNotUsed(String targetSchema, String targetTable, String targetField, Object val) throws SQLException
    {
        return verifyNotUsed(targetSchema, targetTable, targetField, val, null);
    }

    public boolean verifyNotUsed(String targetSchema, String targetTable, String targetField, Object val, @Nullable String containerPath) throws SQLException
    {
        Container c = _container;
        if (containerPath != null)
        {
            c = ContainerManager.getForPath(containerPath);
            if (c == null)
            {
                throw new IllegalArgumentException("Unknown container: " + containerPath);
            }
        }

        UserSchema us = QueryService.get().getUserSchema(_user, c, targetSchema);
        if (us == null)
            throw new IllegalArgumentException("Unknown schema: " + targetSchema);

        TableInfo ti = us.getTable(targetTable);
        if (ti == null)
            throw new IllegalArgumentException("Unknown table: " + targetTable);

        if (ti.getColumn(targetField) == null)
            throw new IllegalArgumentException("Unknown column: " + targetField + " in table: " + targetTable);

        TableSelector ts = new TableSelector(ti, new SimpleFilter(FieldKey.fromString(targetField), val), null);

        return ts.exists();
    }
}
