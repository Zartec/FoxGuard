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

package net.foxdenstudio.sponge.foxguard.plugin.command.link;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Fox on 4/19/2016.
 */
public class LinkageParser {

    private static final String REGEX = "[>()]|([\"'])(?:\\\\.|[^\\\\>(),])*?\\1|(?:\\\\.|[^\"'\\s>(),])+";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private World currentWorld;

    public static Set<LinkEntry> parseLinkageExpression(String expression, CommandSource source) throws CommandException {
        return new LinkageParser(source).parse(expression, source);
    }

    public static List<String> getSuggestions(String expressionString, CommandSource source) {
        String[] parts = expressionString.split(" +", -1);
        String endPart = parts[parts.length - 1];
        Matcher matcher = PATTERN.matcher(endPart);
        boolean found = false;
        while(matcher.find()){found = true;}
        if(found) {
            String token = matcher.group();

        } else {

        }

        World world;
        if (source instanceof Player) world = ((Player) source).getWorld();
        return ImmutableList.of();
    }

    private LinkageParser(CommandSource source) {
        if (source instanceof Player) {
            currentWorld = ((Player) source).getWorld();
        }
    }

    private Set<LinkEntry> parse(String expressionString, CommandSource source) throws CommandException {
        String[] parts = expressionString.split(";");
        Set<LinkEntry> set = new HashSet<>();
        for (String part : parts) {
            if (!checkParentheses(part)) {
                throw new CommandException(Text.of("You must close all parentheses!"));
            }
            IExpression expression = new Expression(part, source, Stage.START);
            set.addAll(expression.getLinks());
        }
        return ImmutableSet.copyOf(set);
    }

    private static boolean checkParentheses(String expression) {
        Pattern leftPattern = Pattern.compile("\\(");
        Matcher leftMatcher = leftPattern.matcher(expression);
        int leftCount = 0;
        while (leftMatcher.find()) {
            leftCount++;
        }
        Pattern rightPattern = Pattern.compile("\\)");
        Matcher rightMatcher = rightPattern.matcher(expression);
        int rightCount = 0;
        while (rightMatcher.find()) {
            rightCount++;
        }
        return leftCount == rightCount;
    }

    public class Expression implements IExpression {

        private List<Set<IExpression>> contents = new ArrayList<>();
        private Stage stage;

        public Expression(String expressionString, CommandSource source, Stage stage) {
            this.stage = stage;
            Matcher matcher = PATTERN.matcher(expressionString);
            int parentheses = 0;
            int startIndex = 0;
            while (matcher.find()) {
                if (matcher.group().equals("(")) parentheses++;
                else if (matcher.group().equals(")")) parentheses--;
                else if (matcher.group().equals(">") && parentheses == 0) {
                    contents.add(parseSegment(expressionString.substring(startIndex, matcher.start()), source));
                    startIndex = matcher.end();
                    this.stage = Stage.REST;
                }
            }
            contents.add(parseSegment(expressionString.substring(startIndex, expressionString.length()), source));
        }

        private Set<IExpression> parseSegment(String segmentString, CommandSource source) {
            Set<IExpression> set = new HashSet<>();
            Set<IFGObject> stubObjects = new HashSet<>();
            Matcher matcher = PATTERN.matcher(segmentString);
            while (matcher.find()) {
                if (matcher.group().equals("(")) {
                    int startIndex = matcher.end();
                    int parentheses = 1;
                    while (parentheses != 0 && matcher.find()) {
                        if (matcher.group().equals("(")) parentheses++;
                        else if (matcher.group().equals(")")) parentheses--;
                    }
                    set.add(new Expression(segmentString.substring(startIndex, matcher.start()), source, stage));
                } else {
                    String token = matcher.group();
                    if (!token.startsWith("-")) {
                        if (token.startsWith("%")) {
                            Optional<World> worldOptional = Sponge.getServer().getWorld(token.substring(1));
                            if (worldOptional.isPresent()) currentWorld = worldOptional.get();
                        } else if (token.startsWith("$")) {
                            String name = token.substring(1);
                            if (Aliases.isIn(Aliases.REGIONS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedRegions(source))));
                            } else if (Aliases.isIn(Aliases.HANDLERS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedHandlers(source))));
                            } else if (Aliases.isIn(Aliases.CONTROLLERS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedControllers(source))));
                            }
                        } else {
                            if (token.startsWith("^")) {
                                IController controller = FGManager.getInstance().getController(token.substring(1));
                                if (controller != null) stubObjects.add(controller);
                            } else if (stage == Stage.START) {
                                IRegion region = FGManager.getInstance().getRegionFromWorld(currentWorld, token);
                                if (region != null) stubObjects.add(region);
                            } else {
                                IHandler handler = FGManager.getInstance().gethandler(token);
                                if (handler != null) stubObjects.add(handler);
                            }
                        }
                    }
                }
            }
            if (stubObjects.size() > 0) set.add(new ExpressionStub(stubObjects));
            return ImmutableSet.copyOf(set);
        }

        @Override
        public Set<IFGObject> getValue() {
            if (contents.size() > 0) {
                Set<IFGObject> set = new HashSet<>();
                for (IExpression expression : contents.get(0)) {
                    set.addAll(expression.getValue());
                }
                return set;
            } else return ImmutableSet.of();
        }

        @Override
        public Set<LinkEntry> getLinks() {
            if (contents.size() > 0) {
                Set<LinkEntry> set = new HashSet<>();
                if (contents.size() > 1) {
                    for (Set<IExpression> eSet : contents) {
                        for (IExpression ex : eSet) {
                            set.addAll(ex.getLinks());
                        }
                    }
                    Set<IExpression> from, to = contents.get(0);
                    for (int i = 1; i < contents.size(); i++) {
                        from = to;
                        to = contents.get(i);
                        for (IExpression fromEx : from) {
                            for (IExpression toEx : to) {
                                fromEx.getValue().stream()
                                        .filter(fromObj -> fromObj instanceof ILinkable)
                                        .forEach(fromObj -> set.addAll(toEx.getValue().stream()
                                                .filter(toObj -> toObj instanceof IHandler)
                                                .map(toObj -> new LinkEntry((ILinkable) fromObj, (IHandler) toObj))
                                                .collect(Collectors.toList())));
                            }
                        }
                    }
                    return set;
                } else {
                    for (IExpression e : contents.get(0)) {
                        set.addAll(e.getLinks());
                    }
                    return set;
                }
            } else return ImmutableSet.of();
        }

    }

    /**
     * Created by Fox on 4/30/2016.
     */
    public class ExpressionStub implements IExpression {

        Set<IFGObject> set;

        public ExpressionStub(Set<IFGObject> set) {
            this.set = set;
        }

        public Set<IFGObject> getValue() {
            return set;
        }

        @Override
        public Set<LinkEntry> getLinks() {
            return ImmutableSet.of();
        }

    }

    private enum Stage {
        START, REST
    }
}
