package sc.loot.processor;

import sc.loot.util.Constants;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This class is in charge of updating all rank-related roles for all users.
 */
public class RoleUpdater {

    public RoleUpdater() {}

    /**
     * Count all posts and store each user with their post count
     * @param guild
     */
    public void runRoleUpdate(IGuild guild) {
        System.out.println("---All roles are going to be updated---");
        Map<IUser, Integer> userToPostCount = new HashMap<>();
        for (IUser user : guild.getUsers()) {
            userToPostCount.put(user, 0);
        }
        // Populate the userToPostCount map
        Stream.of(
                guild.getChannelByID(Constants.SC_LOOT_CHANNEL_ID)
                        .getFullMessageHistory()
                        .asArray())
                .filter(message -> message.getChannel().getLongID() == Constants.SC_LOOT_CHANNEL_ID)
                .forEach(message -> incrementUserPostCount(message.getAuthor(), userToPostCount));

        // Update roles for all users
        int i = 0;
        for (Map.Entry<IUser, Integer> entry : userToPostCount.entrySet()) {
            IUser user = entry.getKey();
            int postCount = entry.getValue();
            updateRole(user, postCount, guild);
            if (i % 10 == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            i++;
//            System.out.println(user+ ": " + postCount);
        }

        Map.Entry<IUser, Integer> topContributor = userToPostCount.entrySet()
                .stream()
                .max(Comparator.comparing(e -> e.getValue()))
                .get();
        updateTopContributor(topContributor.getKey(), guild);

        System.out.println("---Roles have finished being updated---");
        guild.getChannelByID(Constants.SC_LOOT_LOG_ID).sendMessage("Roles have finished being " +
                "updated.");
        guild.getChannelByID(Constants.SC_LOOT_LOG_ID).sendMessage(
                "The current top contributor is " + topContributor.getKey().mention()
                        + " with `" + topContributor.getValue()
                        + "` total submissions."
        );
    }

    private void updateTopContributor(IUser user, IGuild guild) {
        // If they already have the top contributor role (meaning they are still the top
        // contributor, then don't do anything. Otherwise, remove the role from the user that has
        // the current top contributor role and then add the new top contributor role to the new
        // user
        if (user.hasRole(guild.getRoleByID(Constants.TOP_CONTRIBUTOR_ROLE))) {
            return;
        } else {
            guild.getUsers()
                    .stream()
                    .filter(u -> u.hasRole(guild.getRoleByID(Constants.TOP_CONTRIBUTOR_ROLE)))
                    .forEach(u -> u.removeRole(guild.getRoleByID(Constants.TOP_CONTRIBUTOR_ROLE)));
            user.addRole(guild.getRoleByID(Constants.TOP_CONTRIBUTOR_ROLE));
        }
    }

    /**
     * Finds the new role that the user has achieved
     * @param user
     * @param postCount
     * @param guild
     */
    private void updateRole(IUser user, int postCount, IGuild guild) {
        if (postCount >= 80) {
            addRole(user, Constants.LEGENDARY_ROLE, guild);
        } else if (postCount >= 55) {
            addRole(user, Constants.ELITE_ROLE, guild);
        } else if (postCount >= 35) {
            addRole(user, Constants.EXALTED_ROLE, guild);
        } else if (postCount >= 20) {
            addRole(user, Constants.VETERAN_ROLE, guild);
        } else if (postCount >= 10) {
            addRole(user, Constants.PRO_ROLE, guild);
        } else if (postCount >= 3) {
            addRole(user, Constants.ACE_ROLE, guild);
        } else if (postCount >= 0) {
            addRole(user, Constants.ROOKIE_ROLE, guild);
        }
    }

    /**
     * Adds the new role to the user if they don't have it yet. Otherwise if they do have it,
     * do nothing as no update is needed.
     * @param user
     * @param roleID
     * @param guild
     */
    private void addRole(IUser user, long roleID, IGuild guild) {
        IRole role = guild.getRoleByID(roleID);
        if (!user.getRolesForGuild(guild).contains(role)) {
            user.addRole(role);
            removeOtherRoles(user, role, guild);
        }
    }

    /**
     * Since the new role has been added in, remove the old existing one(s)
     * @param user
     * @param except
     * @param guild
     */
    private void removeOtherRoles(IUser user, IRole except, IGuild guild) {
        for (int i = 0; i < Constants.RANK_ROLES.length; i++) {
            IRole roleToRemove = guild.getRoleByID(Constants.RANK_ROLES[i]);
            if (roleToRemove.getLongID() == except.getLongID()) {
                continue;
            }
            if (user.hasRole(roleToRemove)) {
                user.removeRole(roleToRemove);
            }
        }
    }

    private void incrementUserPostCount(IUser author, Map<IUser, Integer> testMap) {
        if (testMap.get(author) == null) {
            return;
        }
        testMap.put(author, testMap.get(author) + 1);
    }

}
