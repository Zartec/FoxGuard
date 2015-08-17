package tk.elektrofuchse.fox.foxguard.flags;

import org.spongepowered.api.entity.player.Player;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.flags.util.PassiveFlags;

import java.util.StringJoiner;

/**
 * Created by Fox on 8/17/2015.
 */
public interface IFlagSet {

    FlagState hasPermission(Player player, ActiveFlags flag);

    FlagState isEnabled(PassiveFlags flag);

    int getPriority();

    void setPriority(int priority);

    String getName();

    void setName(String name);
}