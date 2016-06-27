package org.labkey.ldk.ldap;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.PoolableLdapConnectionFactory;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * User: bimber
 * Date: 1/20/13
 * Time: 9:00 PM
 */
public class LdapConnectionWrapper
{
    private PoolableLdapConnectionFactory _pool = null;
    private LdapConnectionConfig _cfg = null;
    private LdapConnection _connection = null;
    private LdapSettings _settings;
    private static final Logger _log = Logger.getLogger(LdapConnectionWrapper.class);

    public LdapConnectionWrapper() throws LdapException
    {
        try
        {
            _cfg = getConnectionConfig();
            _pool = new PoolableLdapConnectionFactory(_cfg);
            _settings = new LdapSettings();
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }
    }

    public boolean connect() throws LdapException
    {
        _connection = getConnection();

        return true;
    }

    private void ensureConnected() throws LdapException
    {
        if (_connection == null)
            throw new LdapException("Either connect() was not called, or the connection has been nulled");

        if (!_connection.isConnected())
            throw new LdapException("The connection is not connected");

        if (!_connection.isAuthenticated())
            throw new LdapException("The connect has not authenticated");
    }

    public void disconnect() throws LdapException
    {
        if (_connection != null)
        {
            try
            {
                _connection.close();
            }
            catch (IOException e)
            {
                throw new LdapException(e);
            }
        }
    }

    private LdapConnection getConnection() throws LdapException
    {
        try
        {
            LdapConnection connection = _pool.makeObject();
            connection.bind();

            if (!connection.isConnected())
                throw new LdapException("The connection is not connected");

            if (!connection.isAuthenticated())
                throw new LdapException("The connect has not authenticated");

            return connection;
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }
    }

    public List<LdapEntry> getGroupMembers(String dn) throws LdapException
    {
        ensureConnected();

        try
        {
            String userFilter = _settings.getUserFilterString() == null ? "" : _settings.getUserFilterString();
            String filter = "(&(objectclass=user)(memberOf=" + dn + ")" + userFilter + ")";

            EntryCursor cursor = _connection.search(getUserSearchString(), filter, SearchScope.SUBTREE, "*");
            List<LdapEntry> users = new ArrayList<>();
            while (cursor.next())
            {
                Entry entry = cursor.get();
                users.add(new LdapEntry(entry, _settings));
            }

            return users;
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }
    }

    public LdapEntry getGroup(String dn) throws LdapException
    {
        String groupFilter = _settings.getGroupFilterString();
        String filter = "(objectclass=*)";
        if (groupFilter != null)
        {
            filter = "(&" + filter + groupFilter + ")";
        }
        return getEntry(dn, filter);
    }

    private LdapEntry getEntry(String dn, String filter) throws LdapException
    {
        ensureConnected();

        try
        {
            EntryCursor cursor = _connection.search(dn, filter, SearchScope.OBJECT, "*");
            while (cursor.next())
            {
                Entry entry = cursor.get();
                if (entry != null)
                    return new LdapEntry(entry, _settings);
            }

            return null;
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }
    }

    /**
     * List all groups, based on the baseSearch and groupSearch strings, which are defined in the LDAP config
     */
    public List<LdapEntry> listAllGroups() throws LdapException
    {
        ensureConnected();
        return getChildren(new Dn(getGroupSearchString()), OBJECT_CLASS.group);
    }

    /**
     * List all users, based on the baseSearch and userSearch strings, which are defined in the LDAP config
     */
    public List<LdapEntry> listAllUsers() throws LdapException
    {
        ensureConnected();
        return getChildren(new Dn(getUserSearchString()), OBJECT_CLASS.user);
    }

    private List<LdapEntry> getChildren(Dn dn, OBJECT_CLASS objectClass) throws LdapException
    {
        return getChildren(dn, objectClass, new HashSet<String>());
    }

    private List<LdapEntry> getChildren(Dn dn, OBJECT_CLASS objectClass, HashSet<String> encountered) throws LdapException
    {
        List<LdapEntry> entries = new ArrayList<>();

        try
        {
            EntryCursor cursor = _connection.search(dn, "(objectclass=*)", SearchScope.ONELEVEL, "*");
            while (cursor.next())
            {
                Entry entry = cursor.get();

                if (entry.hasObjectClass(objectClass.name()))
                {
                    entries.add(new LdapEntry(entry, _settings));
                }

                //TODO: remove this eventually?
                String key = entry.getDn().getName();
                if (encountered.contains(key))
                {
                    _log.info("continue: " + key);
                    continue;
                }
                encountered.add(entry.getDn().getName());

                //TODO: is there a better way to determine whether we need to recurse?
                if (entry.hasObjectClass(OBJECT_CLASS.organizationalUnit.name()))
                {
                    entries.addAll(getChildren(entry.getDn(), objectClass, encountered));
                }
            }
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }

        return entries;
    }

    public String getProviderName()
    {
        return _cfg.getLdapHost();
    }

    private LdapConnectionConfig getConnectionConfig() throws LdapException
    {
        try
        {
            InitialContext ctx = new InitialContext();
            Context envCtx = (Context)ctx.lookup("java:comp/env");
            LdapConnectionConfig cfg = (LdapConnectionConfig)envCtx.lookup("ldap/ConfigFactory");

            return cfg;
        }
        catch (Exception e)
        {
            throw new LdapException(e);
        }
    }

    private String getGroupSearchString()
    {
        StringBuilder sb = new StringBuilder();
        String delim = "";
        String search = _settings.getGroupSearchString();

        if (search != null)
        {
            sb.append(search);
            delim = ",";
        }

        String base = _settings.getBaseSearchString();
        if (base != null)
        {
            sb.append(delim).append(base);
        }

        return sb.toString();
    }

    private String getUserSearchString()
    {
        StringBuilder sb = new StringBuilder();
        String delim = "";

        String search = _settings.getUserSearchString();
        if (search != null)
        {
            sb.append(search);
            delim = ",";
        }

        String base = _settings.getBaseSearchString();
        if (base != null)
        {
            sb.append(delim).append(base);
        }

        return sb.toString();
    }

    public static enum OBJECT_CLASS
    {
        group(),
        user(),
        organizationalUnit();

        OBJECT_CLASS()
        {

        }
    }
}
