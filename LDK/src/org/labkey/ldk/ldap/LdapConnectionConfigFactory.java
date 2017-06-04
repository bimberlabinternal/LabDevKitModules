package org.labkey.ldk.ldap;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.util.Enumeration;
import java.util.Hashtable;

public class LdapConnectionConfigFactory implements ObjectFactory
{
    public final static String HOST = "host";
    public final static String PORT = "port";
    public final static String PRINCIPAL = "principal";
    public final static String CREDENTIALS = "credentials";
    public final static String USE_SSL = "useSSL";
    public final static String SSL_PROTOCOL = "sslProtocol";

    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?,?> env) throws Exception
    {
        if (!(obj instanceof Reference))
        {
            return null;
        }

        Reference ref = (Reference)obj;
        LdapConnectionConfig cfg = new LdapConnectionConfig();

        Enumeration addrs = ref.getAll();
        while (addrs.hasMoreElements())
        {
            RefAddr addr = (RefAddr)addrs.nextElement();

            if (addr instanceof StringRefAddr)
            {
                if (addr.getType().equals(HOST))
                {
                    cfg.setLdapHost((String)addr.getContent());
                }
                else if (addr.getType().equals(PORT))
                {
                    Integer port = Integer.parseInt((String)addr.getContent());
                    cfg.setLdapPort(port);
                }
                else if (addr.getType().equals(CREDENTIALS))
                {
                    cfg.setCredentials((String)addr.getContent());
                }
                else if (addr.getType().equals(PRINCIPAL))
                {
                    cfg.setName((String)addr.getContent());
                }
                else if (addr.getType().equals(USE_SSL))
                {
                    Boolean useSSL = Boolean.parseBoolean((String)addr.getContent());
                    cfg.setUseSsl(useSSL);
                }
                else if (addr.getType().equals(SSL_PROTOCOL))
                {
                    cfg.setCredentials((String)addr.getContent());
                }
            }
        }

        if (cfg.getLdapHost() == null)
            throw new LdapException("No value for " + HOST + " was provided");

        //NOTE: should we also throw if missing user or credentials?   perhaps your server allows anonymous lookups?
        if (cfg.isUseSsl() && cfg.getSslProtocol() == null)
            cfg.setSslProtocol(LdapConnectionConfig.DEFAULT_SSL_PROTOCOL);

        if (cfg.getLdapPort() == 0)
            cfg.setLdapPort(cfg.isUseSsl() ? LdapConnectionConfig.DEFAULT_LDAPS_PORT : LdapConnectionConfig.DEFAULT_LDAP_PORT);

        return cfg;
    }
}
