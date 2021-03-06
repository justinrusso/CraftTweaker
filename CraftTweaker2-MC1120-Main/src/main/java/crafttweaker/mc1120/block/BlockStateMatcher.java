package crafttweaker.mc1120.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import java.util.*;
import java.util.stream.Collectors;

public class BlockStateMatcher implements IBlockStateMatcher {

    private final crafttweaker.api.block.IBlockState blockState;
    private final Map<String, List<String>> allowedProperties;

    public BlockStateMatcher(crafttweaker.api.block.IBlockState blockState, Map<String, List<String>> allowedProperties) {
        this.blockState = blockState;
        this.allowedProperties = ImmutableMap.copyOf(allowedProperties);
    }

    public BlockStateMatcher(crafttweaker.api.block.IBlockState blockState) {
        this(blockState, ImmutableMap.of());
    }

    public static IBlockStateMatcher create(crafttweaker.api.block.IBlockState... blockStates) {
        if (blockStates == null) {
            throw new IllegalArgumentException("Cannot create matcher for null blockstate");
        }
        for (Object o : blockStates) {
            if (o == null) {
                throw new IllegalArgumentException("Cannot create matcher for null blockstate");
            }
        }
        if (blockStates.length == 1) {
            return new BlockStateMatcher(blockStates[0]);
        } else {
            return new BlockStateMatcherOr(Arrays.copyOf(blockStates, blockStates.length));
        }
    }

    @Override
    public boolean matches(crafttweaker.api.block.IBlockState stateToMatch) {
        if (stateToMatch == null) {
            return false;
        }
        if (((IBlockState) this.blockState.getInternal()).getBlock().equals(((IBlockState) stateToMatch.getInternal()).getBlock())) {
            for (Map.Entry<IProperty<?>, Comparable<?>> entry : ((IBlockState) stateToMatch.getInternal()).getProperties().entrySet()) {
                List<String> allowed = allowedProperties.get(entry.getKey().getName());
                if (allowed != null && !(allowed.contains(entry.getValue().toString()) || allowed.contains("*"))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public IBlockStateMatcher or(IBlockStateMatcher matcher) {
        return new BlockStateMatcherOr(this, matcher);
    }

    @Override
    public Collection<crafttweaker.api.block.IBlockState> getMatchingBlockStates() {
        IBlockState state = ((IBlockState) blockState.getInternal());
        return state.getBlock().getBlockState().getValidStates()
                .stream()
                .map(MCBlockState::new)
                .filter(this::matches)
                .collect(Collectors.toList());
    }

    @Override
    public IBlockStateMatcher allowValuesForProperty(String name, String... values) {
        CraftTweakerAPI.logWarning("IBlockStateMatcher#allowValuesForProperty is deprecated. Please use IBlockStateMatcher#withMatchedValuesForProperty instead.");
        return withMatchedValuesForProperty(name, values);
    }

    @Override
    public IBlockStateMatcher withMatchedValuesForProperty(String name, String... values) {
        Map<String, List<String>> newProps = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allowedProperties.entrySet()) {
            newProps.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        List<String> newValues = ImmutableList.copyOf(values);
        if (newValues.contains("*") && newValues.size() > 1) {
            newProps.put(name, ImmutableList.of("*"));
        } else {
            newProps.put(name, newValues);
        }

        return new BlockStateMatcher(blockState, newProps);
    }

    @Override
    public List<String> getMatchedValuesForProperty(String name) {
        if (allowedProperties.containsKey(name)) {
            return ImmutableList.copyOf(allowedProperties.get(name));
        }
        return ImmutableList.of("*");
    }

    @Override
    public Map<String, List<String>> getMatchedProperties() {
        Map<String, List<String>> props = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allowedProperties.entrySet()) {
            props.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        return ImmutableMap.copyOf(props);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Matcher (");
        IBlockState state = (IBlockState) blockState.getInternal();
        builder.append(state.getBlock().getRegistryName());
        if (!allowedProperties.isEmpty()) {
            builder.append(":");
            StringJoiner joiner = new StringJoiner(",");
            for (Map.Entry<String, List<String>> entry : allowedProperties.entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }
            builder.append(joiner);
        }
        return builder.append(")").toString();
    }

    @Override
    public boolean isCompound() {
        return false;
    }
}
