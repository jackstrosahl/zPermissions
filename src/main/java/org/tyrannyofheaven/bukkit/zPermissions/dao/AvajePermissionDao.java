package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import com.avaje.ebean.EbeanServer;

public class AvajePermissionDao implements PermissionDao {

    private final EbeanServer ebean;
    
    public AvajePermissionDao(EbeanServer ebean) {
        this.ebean = ebean;
    }

    private EbeanServer getEbeanServer() {
        return ebean;
    }

    @Override
    public Boolean getPermission(String name, boolean group, String permission) {
        getEbeanServer().beginTransaction();
        try {
            Entry entry = getEbeanServer().find(Entry.class).where()
                .eq("entity.name", name)
                .eq("entity.group", group)
                .eq("permission", permission)
                .findUnique();
            if (entry != null)
                return entry.isValue();
            return null;
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void setPermission(String name, boolean group, String permission, boolean value) {
        getEbeanServer().beginTransaction();
        try {
            PermissionEntity owner = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", name)
                .eq("group", false)
                .findUnique();
            if (owner == null) {
                owner = new PermissionEntity();
                owner.setName(name);
                owner.setGroup(false);
            }

            Entry found = null;
            for (Entry entry : owner.getPermissions()) {
                if (entry.getPermission().equals(permission)) {
                    found = entry;
                    break;
                }
            }
            
            if (found == null) {
                found = new Entry();
                found.setEntity(owner);
                found.setPermission(permission);

                owner.getPermissions().add(found);
            }
            
            found.setValue(value);

            getEbeanServer().save(owner);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void unsetPermission(String name, boolean group, String permission) {
        getEbeanServer().beginTransaction();
        try {
            Entry entry = getEbeanServer().find(Entry.class).where()
                .eq("entity.name", name)
                .eq("entity.group", group)
                .eq("permission", permission)
                .findUnique();
            if (entry != null) {
                getEbeanServer().delete(entry);
                getEbeanServer().commitTransaction();
            }
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void addMember(String groupName, String member) {
        getEbeanServer().beginTransaction();
        try {
            PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", groupName)
                .eq("group", true)
                .findUnique();

            Membership membership = null;
            if (group == null) {
                group = new PermissionEntity();
                group.setName(groupName);
                group.setGroup(true);
                getEbeanServer().save(group);
            }
            else {
                membership = getEbeanServer().find(Membership.class).where()
                    .eq("member", member)
                    .eq("group", group)
                    .findUnique();
            }
            
            if (membership == null) {
                membership = new Membership();
                membership.setMember(member);
                membership.setGroup(group);
                getEbeanServer().save(membership);

                group.getMemberships().add(membership);
                getEbeanServer().commitTransaction();
            }
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void removeMember(String groupName, String member) {
        getEbeanServer().beginTransaction();
        try {
            PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", groupName)
                .eq("group", true)
                .findUnique();
            
            if (group != null) {
                Membership membership = getEbeanServer().find(Membership.class).where()
                    .eq("member", member)
                    .eq("group", group)
                    .findUnique();
                
                if (membership != null) {
                    getEbeanServer().delete(membership);
                    getEbeanServer().commitTransaction();
                }
            }
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public Set<PermissionEntity> getGroups(String member) {
        getEbeanServer().beginTransaction();
        try {
            List<Membership> memberships = getEbeanServer().find(Membership.class).where()
                .eq("member", member)
                .findList();
            
            Set<PermissionEntity> groups = new HashSet<PermissionEntity>();
            for (Membership membership : memberships) {
                groups.add(membership.getGroup());
            }
            return groups;
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

}
