package com.tonywww.slashblade_sendims.compat.adastra;

import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.recipes.machines.AlloyingRecipe;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/** An immutable work order; inventory is only changed after every result has a destination. */
public final class EtrionicFurnaceBatch {
    private final EtrionicBlastFurnaceBlockEntity.Mode mode;
    private final ItemStack[] inputs;
    private final int[] consumed;
    private final List<ItemStack> results;
    private final List<Recipe<?>> recipes;
    private final int duration;
    private final long energyPerTick;
    private final CompoundTag fingerprint;

    private EtrionicFurnaceBatch(EtrionicBlastFurnaceBlockEntity furnace, int[] consumed,
                               List<ItemStack> results, List<Recipe<?>> recipes, int duration, long energyPerTick) {
        this.mode = furnace.mode();
        this.inputs = copyInputs(furnace);
        this.consumed = consumed;
        this.results = results.stream().map(ItemStack::copy).toList();
        this.recipes = List.copyOf(recipes);
        this.duration = Math.max(1, duration);
        this.energyPerTick = Math.max(0, energyPerTick);
        this.fingerprint = createFingerprint();
    }

    public static ItemStack[] copyInputs(EtrionicBlastFurnaceBlockEntity furnace) {
        ItemStack[] inputs = new ItemStack[4];
        for (int i = 0; i < 4; i++) inputs[i] = furnace.getItem(i + 1).copy();
        return inputs;
    }

    public static EtrionicFurnaceBatch find(EtrionicBlastFurnaceBlockEntity furnace) {
        RecipeManager manager = furnace.getLevel().getRecipeManager();
        if (furnace.mode() == EtrionicBlastFurnaceBlockEntity.Mode.BLASTING) {
            int[] consumed = new int[4];
            List<ItemStack> results = new ArrayList<>();
            List<Recipe<?>> recipes = new ArrayList<>();
            int duration = 1;
            for (int i = 0; i < 4; i++) {
                ItemStack input = furnace.getItem(i + 1);
                if (input.isEmpty()) continue;
                SimpleContainer single = new SimpleContainer(input.copy());
                BlastingRecipe recipe = manager.getRecipeFor(RecipeType.BLASTING, single, furnace.getLevel()).orElse(null);
                if (recipe == null) continue;
                ItemStack result = recipe.assemble(single, furnace.getLevel().registryAccess());
                if (result.isEmpty()) continue;
                consumed[i] = 1;
                results.add(result);
                recipes.add(recipe);
                // The native UI has one progress bar. A batch must satisfy its slowest recipe.
                duration = Math.max(duration, recipe.getCookingTime());
            }
            if (results.isEmpty()) return null;
            long perItem = Math.max(0, MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem);
            long cost = perItem > Long.MAX_VALUE / results.size() ? Long.MAX_VALUE : perItem * results.size();
            return new EtrionicFurnaceBatch(furnace, consumed, results, recipes, duration, cost);
        }
        ItemStack[] inputs = copyInputs(furnace);
        for (AlloyingRecipe recipe : manager.getAllRecipesFor(ModRecipeTypes.ALLOYING.get())) {
            if (recipe.result().isEmpty()) continue;
            int[] consumed = allocateIngredients(inputs, recipe.ingredients());
            if (consumed != null) {
                return new EtrionicFurnaceBatch(furnace, consumed, List.of(recipe.result()),
                        List.of(recipe), recipe.cookingTime(), recipe.energy());
            }
        }
        return null;
    }

    /** Capacitated matching, including duplicate ingredients and overlapping tags/alternatives. */
    private static int[] allocateIngredients(ItemStack[] inputs, List<Ingredient> ingredients) {
        int available = Arrays.stream(inputs).mapToInt(stack -> Math.min(stack.getCount(), ingredients.size())).sum();
        if (ingredients.isEmpty() || ingredients.size() > available) return null;
        boolean[][] matches = new boolean[ingredients.size()][4];
        for (int i = 0; i < ingredients.size(); i++) {
            for (int slot = 0; slot < 4; slot++) {
                matches[i][slot] = !inputs[slot].isEmpty() && ingredients.get(i).test(inputs[slot]);
            }
        }
        int[][] assigned = new int[4][ingredients.size()];
        int[] used = new int[4];
        for (int i = 0; i < ingredients.size(); i++) {
            if (!assign(i, inputs, matches, assigned, used, new boolean[4])) return null;
        }
        return used;
    }

    private static boolean assign(int ingredient, ItemStack[] inputs, boolean[][] matches,
                                  int[][] assigned, int[] used, boolean[] visited) {
        for (int slot = 0; slot < 4; slot++) {
            if (!matches[ingredient][slot] || visited[slot]) continue;
            visited[slot] = true;
            if (used[slot] < inputs[slot].getCount()) {
                assigned[slot][used[slot]++] = ingredient;
                return true;
            }
            for (int index = 0; index < used[slot]; index++) {
                if (assign(assigned[slot][index], inputs, matches, assigned, used, visited)) {
                    assigned[slot][index] = ingredient;
                    return true;
                }
            }
        }
        return false;
    }

    public int duration() { return duration; }
    public long energyPerTick() { return energyPerTick; }
    public CompoundTag fingerprint() { return fingerprint.copy(); }

    public boolean recipesCurrent(RecipeManager manager) {
        return recipes.stream().allMatch(recipe -> manager.byKey(recipe.getId()).orElse(null) == recipe);
    }

    public boolean sameWork(EtrionicFurnaceBatch other) {
        if (other == null || !fingerprint.equals(other.fingerprint) || recipes.size() != other.recipes.size()) return false;
        for (int i = 0; i < recipes.size(); i++) if (recipes.get(i) != other.recipes.get(i)) return false;
        return true;
    }

    private CompoundTag createFingerprint() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        tag.putInt("Duration", duration);
        tag.putLong("Energy", energyPerTick);
        tag.putIntArray("Consumed", consumed);
        for (int i = 0; i < 4; i++) {
            if (consumed[i] > 0) tag.put("Input" + i, inputs[i].copyWithCount(1).save(new CompoundTag()));
        }
        ListTag outputs = new ListTag();
        for (ItemStack result : results) outputs.add(result.save(new CompoundTag()));
        tag.put("Results", outputs);
        for (int i = 0; i < recipes.size(); i++) tag.putString("Recipe" + i, recipes.get(i).getId().toString());
        return tag;
    }

    private boolean inputsValid(EtrionicBlastFurnaceBlockEntity furnace) {
        if (furnace.mode() != mode || !recipesCurrent(furnace.getLevel().getRecipeManager())) return false;
        for (int i = 0; i < 4; i++) {
            if (consumed[i] == 0) continue;
            ItemStack current = furnace.getItem(i + 1);
            if (current.getCount() < consumed[i] || !ItemStack.isSameItemSameTags(current, inputs[i])) return false;
        }
        return true;
    }

    /** Simulate the combined results, splitting stacks and preserving item NBT and stack limits. */
    private ItemStack[] planOutputs(EtrionicBlastFurnaceBlockEntity furnace) {
        ItemStack[] output = new ItemStack[4];
        for (int i = 0; i < 4; i++) output[i] = furnace.getItem(i + 5).copy();
        for (ItemStack result : results) {
            int remaining = result.getCount();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < 4 && remaining > 0; slot++) {
                    ItemStack existing = output[slot];
                    if (pass == 0 ? existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, result) : !existing.isEmpty()) continue;
                    int limit = Math.min(furnace.getMaxStackSize(), result.getMaxStackSize());
                    if (!existing.isEmpty()) limit = Math.min(limit, existing.getMaxStackSize());
                    int inserted = Math.min(remaining, Math.max(0, limit - existing.getCount()));
                    if (inserted == 0) continue;
                    output[slot] = existing.isEmpty() ? result.copyWithCount(inserted) : existing.copyWithCount(existing.getCount() + inserted);
                    remaining -= inserted;
                }
            }
            if (remaining != 0) return null;
        }
        return output;
    }

    public boolean canCommit(EtrionicBlastFurnaceBlockEntity furnace) {
        return inputsValid(furnace) && planOutputs(furnace) != null;
    }

    public boolean commit(EtrionicBlastFurnaceBlockEntity furnace) {
        if (!inputsValid(furnace)) return false;
        ItemStack[] output = planOutputs(furnace);
        if (output == null) return false;
        // No setItem()/update() callbacks between slots: one server-thread transaction.
        for (int i = 0; i < 4; i++) {
            if (consumed[i] > 0) {
                ItemStack remainder = furnace.getItem(i + 1).copy();
                remainder.shrink(consumed[i]);
                furnace.items().set(i + 1, remainder);
            }
        }
        for (int i = 0; i < 4; i++) furnace.items().set(i + 5, output[i]);
        furnace.setChanged();
        return true;
    }
}
