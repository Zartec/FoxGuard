package tk.elektrofuchse.fox.foxguard.commands;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandAdd implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};
    String[] positionsAliases = {"positions", "position", "points", "point", "locations", "location", "pos", "loc", "locs", "p"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ");
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (FGHelper.contains(regionsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FoxGuardManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new ArgumentParseException(Texts.of("No Regions with this name!"), args[1 + flag], 1 + flag);
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.contains(region))
                    throw new ArgumentParseException(Texts.of("Region is already in your state buffer!"), args[1 + flag], 1 + flag);
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.add(region);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added Region to your state buffer!"));
                return CommandResult.success();
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                IFlagSet flagSet = FoxGuardManager.getInstance().getFlagSet(args[1]);
                if (flagSet == null)
                    throw new ArgumentParseException(Texts.of("No FlagSets with this name!"), args[1], 1);
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.contains(flagSet))
                    throw new ArgumentParseException(Texts.of("FlagSet is already in your state buffer!"), args[1], 1);
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.add(flagSet);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added FlagSet to your state buffer!"));
                return CommandResult.success();
            } else if (FGHelper.contains(positionsAliases, args[0])) {
                int x, y, z;
                Vector3i pPos = player.getLocation().getBlockPosition();
                if (args.length == 1) {
                    x = pPos.getX();
                    y = pPos.getY();
                    z = pPos.getZ();
                } else if (args.length > 1 && args.length < 4) {
                    throw new CommandException(Texts.of("Not enough arguments!"));
                } else if (args.length == 4) {
                    try {
                        x = FGHelper.parseCoordinate(pPos.getX(), args[1]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[1] + "\"!"), e, args[1], 1);
                    }
                    try {
                        y = FGHelper.parseCoordinate(pPos.getY(), args[2]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[2] + "\"!"), e, args[2], 2);
                    }
                    try {
                        z = FGHelper.parseCoordinate(pPos.getZ(), args[3]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[3] + "\"!"), e, args[3], 3);
                    }
                } else {
                    throw new CommandException(Texts.of("Too many arguments!"));
                }
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).positions.add(new Vector3i(x, y, z));
                player.sendMessage(Texts.of(TextColors.GREEN, "Successfully added position (" + x + ", " + y + ", " + z + ") to your state buffer!"));
                return CommandResult.success();
            } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
        } else {

        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        if (source instanceof Player)
            return Texts.of("detail (region [w:<worldname>] | flagset) <name>");
        else return Texts.of("detail (region <worldname> | flagset) <name>");
    }
}