package tk.elektrofuchse.fox.foxguard.commands;

import com.google.common.base.Optional;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 */
public class CommandTest implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        source.sendMessage(Texts.of(arguments));
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
        return Optional.absent();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.absent();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("Testing");
    }
}
