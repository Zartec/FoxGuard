/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxguard.plugin.command;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandDetail implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(ALL_ALIASES, key) && !map.containsKey("all")) {
            map.put("all", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            IRegion region = FGManager.getInstance().getRegion(parse.args[1]);
            if (region == null) {
                String worldName = parse.flagmap.get("world");
                World world = null;
                if (source instanceof Player) world = ((Player) source).getWorld();
                if (!worldName.isEmpty()) {
                    Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                    if (optWorld.isPresent()) {
                        world = optWorld.get();
                    } else {
                        if (world == null)
                            throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
                    }
                }
                if (world == null) throw new CommandException(Text.of("Must specify a world!"));
                region = FGManager.getInstance().getWorldRegion(world, parse.args[1]);
            }
            if (region == null)
                throw new CommandException(Text.of("No region exists with the name \"" + parse.args[1] + "\"!"));
            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
            if (parse.args.length < 3 || parse.args[2].isEmpty() || parse.flagmap.containsKey("all")) {
                builder.append(Text.of(TextColors.GREEN, "------- General -------\n"));
                builder.append(Text.of(TextColors.GOLD, "Name: "), Text.of(TextColors.RESET, region.getName() + "\n"));
                builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, region.getLongTypeName() + "\n"));
                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Enabled: "))
                        .append(Text.of(TextColors.RESET, (region.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (region.isEnabled() ? "disable" : "enable") +
                                " r " + FGUtil.genWorldFlag(region) + region.getName()))
                        .onHover(TextActions.showText(Text.of("Click to " + (region.isEnabled() ? "Disable" : "Enable"))))
                        .build());
                if (region instanceof IWorldRegion)
                    builder.append(Text.of(TextColors.GOLD, "World: "), Text.of(TextColors.RESET, ((IWorldRegion) region).getWorld().getName() + "\n"));
                builder.append(Text.of(TextColors.GREEN, "------- Details -------\n"));
                builder.append(region.details(source, parse.args.length < 3 ? "" : parse.args[2]));
                outboundLinks(builder, region, source);
            } else {
                builder.append(Text.of(TextColors.GREEN, "------- Details for Region \"" + region.getName() + "\"" +
                        (region instanceof IWorldRegion ? (" in World \"" + ((IWorldRegion) region).getWorld().getName() + "\"") : "") +
                        " -------\n"));
                builder.append(region.details(source, parse.args.length < 3 ? "" : parse.args[2]));
            }
            source.sendMessage(builder.build());
            return CommandResult.empty();
        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            IHandler handler = FGManager.getInstance().gethandler(parse.args[1]);
            if (handler == null)
                throw new CommandException(Text.of("No handler with name \"" + parse.args[1] + "\"!"));
            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
            if (parse.args.length <= 2 || parse.args[2].isEmpty() || parse.flagmap.containsKey("all")) {
                builder.append(Text.of(TextColors.GREEN, "------- General -------\n"));
                builder.append(Text.of(TextColors.GOLD, "Name: "), Text.of(TextColors.RESET, handler.getName() + "\n"));
                builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, handler.getLongTypeName() + "\n"));
                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Enabled: "))
                        .append(Text.of(TextColors.RESET, (handler.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (handler.isEnabled() ? "disable" : "enable") + " h " + handler.getName()))
                        .onHover(TextActions.showText(Text.of("Click to " + (handler.isEnabled() ? "Disable" : "Enable"))))
                        .build());
                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Priority: "))
                        .append(Text.of(TextColors.RESET, handler.getPriority() + "\n"))
                        .onClick(TextActions.suggestCommand("/foxguard prio " + handler.getName() + " "))
                        .onHover(TextActions.showText(Text.of("Click to Change Priority")))
                        .build());
                builder.append(Text.of(TextColors.GREEN, "------- Details -------\n"));
                Text objectDetails = handler.details(source, parse.args.length < 3 ? "" : parse.args[2]);
                if (objectDetails == null) objectDetails = Text.of();
                builder.append(objectDetails);
                builder.append(Text.of(TextColors.GREEN, "\n------- Inbound Links -------"));
                List<IController> controllerList = FGManager.getInstance().getControllers().stream()
                        .filter(controller -> controller.getHandlers().contains(handler))
                        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                        .collect(GuavaCollectors.toImmutableList());
                List<IRegion> regionList = FGManager.getInstance().getAllRegions().stream()
                        .filter(region -> region.getHandlers().contains(handler))
                        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                        .collect(GuavaCollectors.toImmutableList());
                if (controllerList.size() == 0 && regionList.size() == 0)
                    builder.append(Text.of(TextStyles.ITALIC, "\nNo inbound links!"));
                controllerList.forEach(controller -> {
                    builder.append(Text.of("\n"));
                    if (source instanceof Player) {
                        List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                        List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                        if (selectedHandlers.contains(controller)) {
                            builder.append(Text.of(TextColors.GRAY, "[h+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s h remove " + controller.getName()),
                                    TextActions.showText(Text.of("Remove from Handler State Buffer")),
                                    "[h-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + controller.getName()),
                                    TextActions.showText(Text.of("Add to Handler State Buffer")),
                                    "[h+]"));
                            builder.append(Text.of(TextColors.GRAY, "[h-]"));
                        }
                        if (selectedControllers.contains(controller)) {
                            builder.append(Text.of(TextColors.GRAY, "[c+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                    TextActions.showText(Text.of("Remove from Controller State Buffer")),
                                    "[c-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                    TextActions.showText(Text.of("Add to Controller State Buffer")),
                                    "[c+]"));
                            builder.append(Text.of(TextColors.GRAY, "[c-]"));
                        }
                        builder.append(Text.of(" "));
                    }
                    builder.append(Text.of(FGUtil.getColorForObject(controller),
                            TextActions.runCommand("/foxguard det c " + controller.getName()),
                            TextActions.showText(Text.of("View details for controller \"" + controller.getName() + "\"")),
                            controller.getShortTypeName() + " : " + controller.getName()));
                });

                regionList.forEach(region -> {
                    builder.append(Text.of("\n"));
                    if (source instanceof Player) {
                        List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                        if (selectedRegions.contains(region)) {
                            builder.append(Text.of(TextColors.GRAY, "[+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s r remove " +
                                            FGUtil.genWorldFlag(region) +
                                            region.getName()),
                                    TextActions.showText(Text.of("Remove from State Buffer")),
                                    "[-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s r add " +
                                            FGUtil.genWorldFlag(region) +
                                            region.getName()),
                                    TextActions.showText(Text.of("Add to State Buffer")),
                                    "[+]"));
                            builder.append(Text.of(TextColors.GRAY, "[-]"));
                        }
                        builder.append(Text.of(" "));
                    }
                    builder.append(Text.of(FGUtil.getColorForObject(region),
                            TextActions.runCommand("/foxguard detail region " + FGUtil.genWorldFlag(region) + region.getName()),
                            TextActions.showText(Text.of("View details for region \"" + region.getName() + "\"")),
                            FGUtil.getRegionName(region, true)
                    ));
                });
                if (handler instanceof IController) {
                    outboundLinks(builder, (IController) handler, source);
                }
            } else {
                builder.append(Text.of(TextColors.GREEN, "------- Details for Handler \"" + handler.getName() + "\" -------\n"));
                builder.append(handler.details(source, parse.args[2]));
            }
            source.sendMessage(builder.build());
            return CommandResult.empty();
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
    }

    private void outboundLinks(Text.Builder builder, ILinkable linkable, CommandSource source) {
        builder.append(Text.of(TextColors.GREEN, "\n------- Outbound Links -------"));
        if (linkable.getHandlers().size() == 0)
            builder.append(Text.of(TextStyles.ITALIC, "\nNo outbound links!"));
        linkable.getHandlers().stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).forEach(handler -> {
            builder.append(Text.of("\n"));
            if (source instanceof Player) {
                List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                if (selectedHandlers.contains(handler)) {
                    builder.append(Text.of(TextColors.GRAY, "[h+]"));
                    builder.append(Text.of(TextColors.RED,
                            TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                            TextActions.showText(Text.of("Remove from Handler State Buffer")),
                            "[h-]"));
                } else {
                    builder.append(Text.of(TextColors.GREEN,
                            TextActions.runCommand("/foxguard s h add " + handler.getName()),
                            TextActions.showText(Text.of("Add to Handler State Buffer")),
                            "[h+]"));
                    builder.append(Text.of(TextColors.GRAY, "[h-]"));
                }
                if (handler instanceof IController) {
                    IController controller = ((IController) handler);
                    if (selectedControllers.contains(controller)) {
                        builder.append(Text.of(TextColors.GRAY, "[c+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from Controller State Buffer")),
                                "[c-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                TextActions.showText(Text.of("Add to Controller State Buffer")),
                                "[c+]"));
                        builder.append(Text.of(TextColors.GRAY, "[c-]"));
                    }
                } else {
                    builder.append(Text.of(TextColors.DARK_GRAY, "[c+][c-]"));
                }
                builder.append(Text.of(" "));
            }
            builder.append(Text.of(FGUtil.getColorForObject(handler),
                    TextActions.runCommand("/foxguard det h " + handler.getName()),
                    TextActions.showText(Text.of("View Details for " + (handler instanceof IController ? "controller" : "handler") + " \"" + handler.getName() + "\"")),
                    handler.getShortTypeName() + " : " + handler.getName()
            ));
        });
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Arrays.asList(FGManager.TYPES).stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 1) {
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flagmap.get("world");
                    World world = null;
                    if (source instanceof Player) world = ((Player) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (world == null) return FGManager.getInstance().getRegions().stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                    return FGManager.getInstance().getAllRegions(world).stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers().stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world").stream()
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {

        return source.hasPermission("foxguard.command.info.detail");
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
        return Text.of("detail <region [--w:<worldname>] | handler> <name> [args...]");
    }
}
