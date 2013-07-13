package com.hwlcn.security.realm;

import com.hwlcn.security.authc.*;
import com.hwlcn.security.authz.AuthorizationInfo;
import com.hwlcn.security.authz.SimpleRole;
import com.hwlcn.security.subject.PrincipalCollection;
import com.hwlcn.security.util.CollectionUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SimpleAccountRealm extends AuthorizingRealm {


    protected final Map<String, SimpleAccount> users;
    protected final Map<String, SimpleRole> roles;
    protected final ReadWriteLock USERS_LOCK;
    protected final ReadWriteLock ROLES_LOCK;

    public SimpleAccountRealm() {
        this.users = new LinkedHashMap<String, SimpleAccount>();
        this.roles = new LinkedHashMap<String, SimpleRole>();
        USERS_LOCK = new ReentrantReadWriteLock();
        ROLES_LOCK = new ReentrantReadWriteLock();
        setCachingEnabled(false);
    }

    public SimpleAccountRealm(String name) {
        this();
        setName(name);
    }

    protected SimpleAccount getUser(String username) {
        USERS_LOCK.readLock().lock();
        try {
            return this.users.get(username);
        } finally {
            USERS_LOCK.readLock().unlock();
        }
    }

    public boolean accountExists(String username) {
        return getUser(username) != null;
    }

    public void addAccount(String username, String password) {
        addAccount(username, password, (String[]) null);
    }

    public void addAccount(String username, String password, String... roles) {
        Set<String> roleNames = CollectionUtils.asSet(roles);
        SimpleAccount account = new SimpleAccount(username, password, getName(), roleNames, null);
        add(account);
    }

    protected String getUsername(SimpleAccount account) {
        return getUsername(account.getPrincipals());
    }

    protected String getUsername(PrincipalCollection principals) {
        return getAvailablePrincipal(principals).toString();
    }

    protected void add(SimpleAccount account) {
        String username = getUsername(account);
        USERS_LOCK.writeLock().lock();
        try {
            this.users.put(username, account);
        } finally {
            USERS_LOCK.writeLock().unlock();
        }
    }

    protected SimpleRole getRole(String rolename) {
        ROLES_LOCK.readLock().lock();
        try {
            return roles.get(rolename);
        } finally {
            ROLES_LOCK.readLock().unlock();
        }
    }

    public boolean roleExists(String name) {
        return getRole(name) != null;
    }

    public void addRole(String name) {
        add(new SimpleRole(name));
    }

    protected void add(SimpleRole role) {
        ROLES_LOCK.writeLock().lock();
        try {
            roles.put(role.getName(), role);
        } finally {
            ROLES_LOCK.writeLock().unlock();
        }
    }

    protected static Set<String> toSet(String delimited, String delimiter) {
        if (delimited == null || delimited.trim().equals("")) {
            return null;
        }

        Set<String> values = new HashSet<String>();
        String[] rolenamesArray = delimited.split(delimiter);
        for (String s : rolenamesArray) {
            String trimmed = s.trim();
            if (trimmed.length() > 0) {
                values.add(trimmed);
            }
        }

        return values;
    }

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        SimpleAccount account = getUser(upToken.getUsername());

        if (account != null) {

            if (account.isLocked()) {
                throw new LockedAccountException("Account [" + account + "] is locked.");
            }
            if (account.isCredentialsExpired()) {
                String msg = "The credentials for account [" + account + "] are expired";
                throw new ExpiredCredentialsException(msg);
            }

        }

        return account;
    }

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = getUsername(principals);
        USERS_LOCK.readLock().lock();
        try {
            return this.users.get(username);
        } finally {
            USERS_LOCK.readLock().unlock();
        }
    }
}