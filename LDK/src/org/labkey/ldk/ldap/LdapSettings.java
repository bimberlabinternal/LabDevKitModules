package org.labkey.ldk.ldap;

import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.util.ConfigurationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 1/21/13
 * Time: 11:58 AM
 */
public class LdapSettings
{
    public static final String PROPERTY_CATEGORY = "ldk.ldapConfig";

    public static final String BASE_SEARCH_PROP = "baseSearchString";
    public static final String GROUP_SEARCH_PROP = "groupSearchString";
    public static final String USER_SEARCH_PROP = "userSearchString";
    public static final String GROUP_FILTER_PROP = "groupFilterString";
    public static final String USER_FILTER_PROP = "userFilterString";

    public static final String EMAIL_FIELD_PROP = "emailFieldMapping";
    public static final String DISPLAYNAME_FIELD_PROP = "displayNameFieldMapping";
    public static final String UID_FIELD_PROP = "uidFieldMapping";
    public static final String PHONE_FIELD_PROP = "phoneNumberFieldMapping";
    public static final String FIRSTNAME_FIELD_PROP = "firstNameFieldMapping";
    public static final String LASTNAME_FIELD_PROP = "lastNameFieldMapping";

    public static final String LABKEY_EMAIL_PROP = "labkeyAdminEmail";

    public static final String USER_DELETE_PROP = "userDeleteBehavior";
    public static final String GROUP_DELETE_PROP = "groupDeleteBehavior";
    public static final String USER_INFO_CHANGED_PROP = "userInfoChangedBehavior";
    public static final String USERACCOUNTCONTROL_PROP = "userAccountControlBehavior";

    public static final String MEMBER_SYNC_PROP = "memberSyncMode";

    public static final String ENABLED_PROP = "enabled";
    public static final String FREQUENCY_PROP = "frequency";
    public static final String SYNC_MODE_PROP = "syncMode";

    public static final String ALLOWED_DN_PROP = "allowedDn";
    //public static final String DISALLOWED_DN_PROP = "disallowedDn";

    public static final String DELIM = "<>";

    private Map<String, Object> _settings;

    public LdapSettings()
    {
        _settings = getSettingsMap();
    }

    public static void setLdapSettings(Map<String, String> props) throws ConfigurationException
    {
        //validate
        String email = props.get(LABKEY_EMAIL_PROP);
        if (email != null)
        {
            try
            {
                ValidEmail e = new ValidEmail(email);
                User u = UserManager.getUser(e);
                if (u == null)
                {
                    throw new ConfigurationException("Unable to find user for email: " + email);
                }

                if (!u.isSiteAdmin())
                {
                    throw new ConfigurationException("User is not a site admin: " + u.getEmail());
                }
            }
            catch (ValidEmail.InvalidEmailException e)
            {
                throw new ConfigurationException("Improper email: " + email);
            }
        }

        PropertyManager.PropertyMap writableProps = PropertyManager.getWritableProperties(PROPERTY_CATEGORY, true);
        writableProps.clear();

        writableProps.putAll(props);
        writableProps.save();

        LdapScheduler.get().onSettingsChange();
    }

    public Map<String, Object> getSettingsMap()
    {
        Map<String, Object> ret = new HashMap<>();

        Map<String, String> map = PropertyManager.getProperties(PROPERTY_CATEGORY);
        for (String key : map.keySet())
        {
            if (key.equals(ALLOWED_DN_PROP) && StringUtils.trimToNull(map.get(key)) != null)
            {
                ret.put(key, map.get(key).split(DELIM));
            }
//            else if (key.equals(DISALLOWED_DN_PROP) && StringUtils.trimToNull(map.get(key)) != null)
//            {
//                ret.put(key, map.get(key).split(DELIM));
//            }
            else if (key.equals(FREQUENCY_PROP) && StringUtils.trimToNull(map.get(key)) != null)
            {
                ret.put(key, Integer.parseInt(map.get(key)));
            }
            else if (key.equals(ENABLED_PROP) && StringUtils.trimToNull(map.get(key)) != null)
            {
                ret.put(key, Boolean.parseBoolean(map.get(key)));
            }
            else
            {
                ret.put(key, map.get(key));
            }
        }

        if (!ret.containsKey(EMAIL_FIELD_PROP))
            ret.put(EMAIL_FIELD_PROP, "mail");

        if (!ret.containsKey(DISPLAYNAME_FIELD_PROP))
            ret.put(DISPLAYNAME_FIELD_PROP, "displayName");

        if (!ret.containsKey(LASTNAME_FIELD_PROP))
            ret.put(LASTNAME_FIELD_PROP, "sn");

        if (!ret.containsKey(FIRSTNAME_FIELD_PROP))
            ret.put(FIRSTNAME_FIELD_PROP, "givenName");

        if (!ret.containsKey(PHONE_FIELD_PROP))
            ret.put(PHONE_FIELD_PROP, "telephoneNumber");

        if (!ret.containsKey(UID_FIELD_PROP))
            ret.put(UID_FIELD_PROP, "userPrincipalName");

        return ret;
    }

    public boolean isEnabled()
    {
        return _settings.containsKey(ENABLED_PROP) && (Boolean)_settings.get(ENABLED_PROP);
    }

    public String getUserSearchString()
    {
        return (String)_settings.get(USER_SEARCH_PROP);
    }

    public String getGroupSearchString()
    {
        return (String)_settings.get(GROUP_SEARCH_PROP);
    }

    public String getUserFilterString()
    {
        return (String)_settings.get(USER_FILTER_PROP);
    }

    public String getGroupFilterString()
    {
        return (String)_settings.get(GROUP_FILTER_PROP);
    }

    public MemberSyncMode getMemberSyncMode()
    {
        if (!_settings.containsKey(MEMBER_SYNC_PROP))
            return null;

        return MemberSyncMode.valueOf((String)_settings.get(MEMBER_SYNC_PROP));
    }

    public boolean overwriteUserInfoIfChanged()
    {
        return "true".equals(_settings.get(USER_INFO_CHANGED_PROP));
    }

    public boolean deleteGroupWhenRemovedFromLdap()
    {
        return "delete".equals(_settings.get(GROUP_DELETE_PROP));
    }

    public boolean shouldReadUserAccountControl()
    {
        return "true".equals(_settings.get(USERACCOUNTCONTROL_PROP));
    }

    public boolean deleteUserWhenRemovedFromLdap()
    {
        return "delete".equals(_settings.get(USER_DELETE_PROP));
    }

    public List<String> getGroupWhiteList()
    {
        if (!_settings.containsKey(ALLOWED_DN_PROP))
            return Collections.emptyList();

        return Arrays.asList((String[])_settings.get(ALLOWED_DN_PROP));
    }

    public String getBaseSearchString()
    {
        return (String)_settings.get(BASE_SEARCH_PROP);
    }

    public LdapSyncMode getSyncMode()
    {
        if (!_settings.containsKey(SYNC_MODE_PROP))
            return null;

        return LdapSyncMode.valueOf((String)_settings.get(SYNC_MODE_PROP));
    }

    public enum MemberSyncMode
    {
        mirror(),
        removeDeletedLdapUsers(),
        noAction;

        MemberSyncMode()
        {

        }
    }

    public String getEmailMapping()
    {
        return (String)_settings.get(EMAIL_FIELD_PROP);
    }

    public String getDisplayNameMapping()
    {
        return (String)_settings.get(DISPLAYNAME_FIELD_PROP);
    }

    public String getLastNameMapping()
    {
        return (String)_settings.get(LASTNAME_FIELD_PROP);
    }

    public String getFirstNameMapping()
    {
        return (String)_settings.get(FIRSTNAME_FIELD_PROP);
    }

    public String getPhoneMapping()
    {
        return (String)_settings.get(PHONE_FIELD_PROP);
    }

    public String getUIDMapping()
    {
        return (String)_settings.get(UID_FIELD_PROP);
    }

    public String getLabKeyAdminEmail()
    {
        return (String)_settings.get(LABKEY_EMAIL_PROP);
    }

    public User getLabKeyAdminUser()
    {
        String email = getLabKeyAdminEmail();
        if (email == null)
            return null;

        try
        {
            ValidEmail e = new ValidEmail(email);
            User u = UserManager.getUser(e);
            if (u == null)
            {
                return null;
            }

            return u;
        }
        catch (ValidEmail.InvalidEmailException e)
        {
            //this should get caught upstream
            return null;
        }
    }

    public Integer getFrequency()
    {
        return _settings.get(FREQUENCY_PROP) == null ? null : (Integer)_settings.get(FREQUENCY_PROP);
    }

    /**
     * Provides a brief sanity check of the settings, designed to identify problems if a sync will run.
     * @throws LdapException
     */
    public void validateSettings() throws LdapException
    {
        String email = getLabKeyAdminEmail();
        if (email == null)
        {
            throw new LdapException("LabKey admin email not set");
        }

        User u = getLabKeyAdminUser();
        if (u == null)
        {
            throw new LdapException("Unable to find user for email: " + email);
        }

        if (!u.isSiteAdmin())
        {
            throw new LdapException("User is not a site admin: " + u.getEmail());
        }

        LdapSyncMode mode = getSyncMode();
        if (mode == null)
        {
            throw new LdapException("Sync type not set");
        }

        if (LdapSyncMode.groupWhitelist.equals(mode))
        {
            if (getGroupWhiteList().size() == 0)
            {
                throw new LdapException("Cannot choose to sync based on specific groups unless you provide a list of groups to sync");
            }
        }

        MemberSyncMode memberSyncMode = getMemberSyncMode();
        if (memberSyncMode == null)
        {
            throw new LdapException("Member sync type not set");
        }

        if (isEnabled() && getFrequency() == null)
        {
            throw new LdapException("LDAP sync is enabled, but no scheduling frequency was set");
        }
    }

    public enum LdapSyncMode
    {
        usersOnly(),
        usersAndGroups(),
        groupWhitelist();
        //groupBlacklist();

        LdapSyncMode()
        {

        }
    }
}
