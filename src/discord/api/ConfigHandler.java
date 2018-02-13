package discord.api;

import com.Cardinal.OCSL.Obj.Config;
import com.Cardinal.OCSL.Obj.ConfigElement;
import sx.blah.discord.handle.obj.IGuild;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creates hashmaps for the properties of the guild (discord server)
 */
public class ConfigHandler {

    public static Map<Long, Config> guildProperties = new HashMap<>();

    /**
     * Add property to hashmap
     * @param guild
     */
    public static void addGuild(IGuild guild) {
        guildProperties.put(guild.getLongID(), new Config(String.valueOf(guild.getLongID())));
    }

    /**
     * Add values to the property hashmap
     * @param guild
     * @param propertyName
     * @param value
     */
    public static void setProperty(IGuild guild, String propertyName, String value) {
        Optional<Config> config = Optional.of(guildProperties.get(guild.getLongID()));
        if (config.isPresent()) {
            config.get().addElement(new ConfigElement(propertyName, value));
            return;
        } else {
            // guild does not exist
            addGuild(guild);
            setProperty(guild, value, value);
        }
    }

    /**
     * Get the property for the given guild
     * @param guild
     * @param propertyName
     * @return
     */
    public static Object getProperty(IGuild guild, String propertyName) {
        Optional<Config> config = Optional.of(guildProperties.get(guild.getLongID()));
        if (config.isPresent()) {
            if (config.get().doesExist()) {
                ConfigElement element = config.get().getElement(propertyName);
                if (element.getElementType() == 1) { // array value
                    return element.getArrayElement().getValues();
                } else {
                    return element.getValue();
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
