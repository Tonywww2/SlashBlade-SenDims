package com.tonywww.slashblade_sendims.gametest;

import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import earth.terrarium.adastra.common.registry.ModBlocks;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.recipes.machines.AlloyingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises the actual Ad Astra machine with the production Mixin applied by Forge. */
@GameTestHolder("sendims_blasting_test")
@PrefixGameTestTemplate(false)
public final class EtrionicBlastFurnaceGameTests {
    private static final Item[] INPUTS = {Items.RAW_IRON, Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_GOLD};

    private static final class Furnace extends EtrionicBlastFurnaceBlockEntity {
        Furnace(GameTestHelper helper) {
            this(helper, Mode.BLASTING, new ItemStack(Items.RAW_IRON), new ItemStack(Items.RAW_IRON),
                    new ItemStack(Items.RAW_GOLD), new ItemStack(Items.RAW_GOLD));
        }

        Furnace(GameTestHelper helper, Mode mode, ItemStack... inputs) {
            super(BlockPos.ZERO, ModBlocks.ETRIONIC_BLAST_FURNACE.get().defaultBlockState());
            setLevel(helper.getLevel());
            setMode(mode);
            for (int i = 0; i < inputs.length; i++) items().set(i + 1, inputs[i].copy());
            update();
        }

        void tickRecipe() {
            getEnergyStorage().setEnergy(getEnergyStorage().getMaxCapacity());
            recipeTick(getEnergyStorage());
        }

        void finishBatch(GameTestHelper helper) {
            for (int tick = 0; tick < 1000 && outputCount(this) == 0; tick++) tickRecipe();
            helper.assertTrue(outputCount(this) > 0, "Valid replacement recipes must eventually finish");
        }

        void finishSlot(BlastingRecipe recipe, int slot) {
            craft(recipe, slot);
        }

        CompoundTag saveState() {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag);
            return tag;
        }
    }

    private static int count(Furnace furnace, Item item) {
        int result = 0;
        for (int slot = 5; slot <= 8; slot++) {
            ItemStack stack = furnace.getItem(slot);
            if (stack.is(item)) result += stack.getCount();
        }
        return result;
    }

    private static int outputCount(Furnace furnace) {
        int result = 0;
        for (int slot = 5; slot <= 8; slot++) result += furnace.getItem(slot).getCount();
        return result;
    }

    private static BlastingRecipe ironRecipe(GameTestHelper helper) {
        return helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.BLASTING).stream()
                .filter(recipe -> recipe.getIngredients().get(0).test(new ItemStack(Items.RAW_IRON)))
                .findFirst().orElseThrow();
    }

    @GameTest(template = "blast_furnace_test")
    public static void reportedAabbToBbbbSwapCannotProduceA(GameTestHelper helper) {
        Furnace furnace = new Furnace(helper);
        for (int tick = 0; tick < 10; tick++) furnace.tickRecipe();
        furnace.items().set(1, new ItemStack(Items.RAW_GOLD));
        furnace.items().set(2, new ItemStack(Items.RAW_GOLD));
        furnace.finishBatch(helper);
        helper.assertTrue(count(furnace, Items.IRON_INGOT) == 0 && count(furnace, Items.GOLD_INGOT) == 4,
                "aabb -> bbbb must produce 0 A + 4 B; got " + count(furnace, Items.IRON_INGOT)
                        + " A + " + count(furnace, Items.GOLD_INGOT) + " B");
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void replacementInputsRestartAndProduceOnlyMatchingResults(GameTestHelper helper) {
        // Every subset of slots, including the reported aabb -> bbbb (mask 3),
        // both early in the cycle and immediately before its completion tick.
        for (int mask = 1; mask < 16; mask++) {
            for (boolean atCompletion : new boolean[]{false, true}) {
                Furnace furnace = new Furnace(helper);
                int ticks = atCompletion ? furnace.cookTimeTotal() - 1 : 2;
                for (int tick = 0; tick < ticks; tick++) furnace.tickRecipe();
                helper.assertTrue(outputCount(furnace) == 0, "Fixture must still be processing");
                int expectedIron = 0;
                for (int index = 0; index < INPUTS.length; index++) {
                    Item input = INPUTS[index];
                    if ((mask & (1 << index)) != 0) {
                        input = input == Items.RAW_IRON ? Items.RAW_GOLD : Items.RAW_IRON;
                        // Direct mutation also covers automation that does not call setChanged().
                        furnace.items().set(index + 1, new ItemStack(input));
                    }
                    if (input == Items.RAW_IRON) expectedIron++;
                }
                furnace.tickRecipe();
                helper.assertTrue(outputCount(furnace) == 0 && furnace.cookTime() == 1,
                        "Replacing input must restart the shared timer, mask " + mask);
                furnace.finishBatch(helper);
                helper.assertTrue(count(furnace, Items.IRON_INGOT) == expectedIron
                                && count(furnace, Items.GOLD_INGOT) == 4 - expectedIron,
                        "Outputs must match the items actually consumed, mask " + mask);
                for (int slot = 1; slot <= 4; slot++) {
                    helper.assertTrue(furnace.getItem(slot).isEmpty(), "Exactly one item per slot consumed");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void emptyAndUnsmeltableReplacementCannotProduce(GameTestHelper helper) {
        for (int slot = 1; slot <= 4; slot++) {
            for (ItemStack replacement : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK)}) {
                Furnace furnace = new Furnace(helper);
                furnace.tickRecipe();
                furnace.items().set(slot, replacement.copy());
                furnace.finishBatch(helper);
                helper.assertTrue(count(furnace, Items.IRON_INGOT) == (slot <= 2 ? 1 : 2)
                                && count(furnace, Items.GOLD_INGOT) == (slot > 2 ? 1 : 2),
                        "Removed/invalid slot must not create its cached output");
                helper.assertTrue(ItemStack.matches(furnace.getItem(slot), replacement),
                        "Invalid replacement must not be consumed");
                for (int tick = 0; tick < 250; tick++) furnace.tickRecipe();
                helper.assertTrue(outputCount(furnace) == 3, "Empty inputs must never repeat output");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void legacyCraftCannotBypassProgressOrEnergy(GameTestHelper helper) {
        BlastingRecipe recipe = ironRecipe(helper);
        for (ItemStack replacement : new ItemStack[]{ItemStack.EMPTY,
                new ItemStack(Items.RAW_GOLD), new ItemStack(Items.STICK)}) {
            Furnace furnace = new Furnace(helper);
            furnace.items().set(1, replacement.copy());
            furnace.finishSlot(recipe, 1);
            helper.assertTrue(outputCount(furnace) == 0, "Stale recipe must be rejected at craft()");
            helper.assertTrue(ItemStack.matches(furnace.getItem(1), replacement),
                    "Rejected craft must leave replacement untouched");
        }
        Furnace valid = new Furnace(helper);
        valid.finishSlot(null, 1);
        helper.assertTrue(outputCount(valid) == 0 && valid.getItem(1).getCount() == 1, "Null recipe is a no-op");
        valid.finishSlot(recipe, 1);
        helper.assertTrue(outputCount(valid) == 0 && valid.getItem(1).getCount() == 1,
                "Even matching recipes must not bypass the paid batch transaction");
        valid.craftAlloying();
        helper.assertTrue(outputCount(valid) == 0, "Legacy alloy settlement must also be a no-op");
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void unchangedAndRestockedInputsKeepProgress(GameTestHelper helper) {
        Furnace furnace = new Furnace(helper);
        for (int tick = 0; tick < 10; tick++) furnace.tickRecipe();
        furnace.items().set(1, new ItemStack(Items.RAW_IRON, 2));
        furnace.tickRecipe();
        helper.assertTrue(furnace.cookTime() == 11, "Restocking the same ingredient must not reset progress");
        furnace.finishBatch(helper);
        helper.assertTrue(count(furnace, Items.IRON_INGOT) == 2 && count(furnace, Items.GOLD_INGOT) == 2,
                "Normal mixed batch must retain its original results");
        helper.assertTrue(furnace.getItem(1).getCount() == 1, "Restocked surplus must remain");
        helper.succeed();
    }

    private static void withRecipes(GameTestHelper helper, List<Recipe<?>> extra, Runnable body) {
        var manager = helper.getLevel().getRecipeManager();
        List<Recipe<?>> original = new ArrayList<>(manager.getRecipes());
        List<Recipe<?>> combined = new ArrayList<>(original);
        combined.addAll(extra);
        manager.replaceRecipes(combined);
        try { body.run(); }
        finally { manager.replaceRecipes(original); }
    }

    private static BlastingRecipe blasting(String id, Item input, ItemStack result, int duration) {
        return new BlastingRecipe(new ResourceLocation("sendims_blasting_test", id), "",
                CookingBookCategory.MISC, Ingredient.of(input), result, 0, duration);
    }

    private static AlloyingRecipe alloy(String id, List<Ingredient> ingredients, ItemStack result) {
        return new AlloyingRecipe(new ResourceLocation("sendims_blasting_test", id), 7, 20, ingredients, result);
    }

    @GameTest(template = "blast_furnace_test")
    public static void fullBatchEnergyIsRequiredForEveryProgressTick(GameTestHelper helper) {
        Furnace furnace = new Furnace(helper);
        long cost = MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem * 4;
        helper.assertTrue(cost > 0 && cost < furnace.getEnergyStorage().getMaxCapacity(), "Positive test energy cost");
        furnace.getEnergyStorage().setEnergy(cost - 1);
        furnace.recipeTick(furnace.getEnergyStorage());
        helper.assertTrue(furnace.cookTime() == 0 && furnace.getEnergyStorage().getStoredEnergy() == cost - 1,
                "An underfunded batch must neither advance nor partially drain energy");
        int duration = furnace.cookTimeTotal();
        for (int tick = 1; tick <= duration; tick++) {
            furnace.getEnergyStorage().setEnergy(cost);
            furnace.recipeTick(furnace.getEnergyStorage());
            helper.assertTrue(furnace.getEnergyStorage().getStoredEnergy() == 0, "Exactly one batch-tick charge");
            if (tick < duration) {
                helper.assertTrue(furnace.cookTime() == tick && outputCount(furnace) == 0, "No early completion");
                for (int pause = 0; pause < 3; pause++) furnace.recipeTick(furnace.getEnergyStorage());
                helper.assertTrue(furnace.cookTime() == tick, "No-energy pause preserves progress");
            }
        }
        helper.assertTrue(count(furnace, Items.IRON_INGOT) == 2 && count(furnace, Items.GOLD_INGOT) == 2,
                "Exactly one batch finishes after its full paid duration");
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void combinedOutputsMustFitBeforeAnythingIsConsumed(GameTestHelper helper) {
        Furnace furnace = new Furnace(helper);
        furnace.items().set(5, new ItemStack(Items.IRON_INGOT, 63));
        furnace.items().set(6, new ItemStack(Items.GOLD_INGOT, 63));
        furnace.items().set(7, new ItemStack(Items.STONE, 64));
        furnace.items().set(8, new ItemStack(Items.STONE, 64));
        furnace.getEnergyStorage().setEnergy(1000);
        for (int tick = 0; tick < 150; tick++) furnace.recipeTick(furnace.getEnergyStorage());
        helper.assertTrue(furnace.cookTime() == 0 && furnace.getEnergyStorage().getStoredEnergy() == 1000,
                "Individually fitting outputs must not overbook their shared slots");
        for (int slot = 1; slot <= 4; slot++) helper.assertTrue(furnace.getItem(slot).getCount() == 1, "No lost input");
        furnace.getItem(5).setCount(62);
        furnace.getItem(6).setCount(62);
        int duration = furnace.cookTimeTotal();
        for (int tick = 0; tick < duration; tick++) furnace.tickRecipe();
        helper.assertTrue(furnace.getItem(5).getCount() == 64 && furnace.getItem(6).getCount() == 64,
                "The complete batch is inserted exactly once when capacity becomes sufficient");
        for (int slot = 1; slot <= 4; slot++) helper.assertTrue(furnace.getItem(slot).isEmpty(), "All paid inputs consumed");
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void fullOutputsPauseAndResumeWithoutLosingProgress(GameTestHelper helper) {
        Furnace furnace = new Furnace(helper);
        for (int tick = 0; tick < 10; tick++) furnace.tickRecipe();
        for (int slot = 5; slot <= 8; slot++) furnace.items().set(slot, new ItemStack(Items.STONE, 64));
        furnace.getEnergyStorage().setEnergy(1000);
        for (int tick = 0; tick < 150; tick++) furnace.recipeTick(furnace.getEnergyStorage());
        helper.assertTrue(furnace.cookTime() == 10 && furnace.getEnergyStorage().getStoredEnergy() == 1000,
                "A full output inventory must pause without losing progress, input or power");
        for (int slot = 5; slot <= 8; slot++) furnace.items().set(slot, ItemStack.EMPTY);
        furnace.finishBatch(helper);
        helper.assertTrue(outputCount(furnace) == 4, "Opening the outputs resumes the same batch");
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void mixedRecipeTimesUseTheSlowestSlot(GameTestHelper helper) {
        withRecipes(helper, List.of(blasting("fast", Items.STICK, new ItemStack(Items.DIAMOND), 3),
                blasting("slow", Items.FEATHER, new ItemStack(Items.EMERALD), 7)), () -> {
            for (boolean reverse : new boolean[]{false, true}) {
                Item first = reverse ? Items.FEATHER : Items.STICK;
                Item second = reverse ? Items.STICK : Items.FEATHER;
                Furnace furnace = new Furnace(helper, EtrionicBlastFurnaceBlockEntity.Mode.BLASTING,
                        new ItemStack(first), new ItemStack(first), new ItemStack(second), new ItemStack(second));
                helper.assertTrue(furnace.cookTimeTotal() == 7, "Duration must not depend on slot order");
                for (int tick = 0; tick < 6; tick++) furnace.tickRecipe();
                helper.assertTrue(outputCount(furnace) == 0, "Slow recipe cannot borrow a fast recipe's duration");
                furnace.tickRecipe();
                helper.assertTrue(count(furnace, Items.DIAMOND) == 2 && count(furnace, Items.EMERALD) == 2,
                        "Mixed batch completes together at the correct duration");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void multiItemResultsRespectNbtAndStackLimits(GameTestHelper helper) {
        ItemStack result = new ItemStack(Items.ENDER_PEARL, 20);
        result.getOrCreateTag().putString("batch", "expected");
        withRecipes(helper, List.of(blasting("tagged_output", Items.STICK, result, 3)), () -> {
            Furnace furnace = new Furnace(helper, EtrionicBlastFurnaceBlockEntity.Mode.BLASTING, new ItemStack(Items.STICK));
            ItemStack other = result.copyWithCount(15);
            other.getOrCreateTag().putString("batch", "different");
            furnace.items().set(5, other);
            furnace.items().set(6, result.copyWithCount(15));
            for (int tick = 0; tick < 5; tick++) furnace.tickRecipe();
            helper.assertTrue(ItemStack.matches(furnace.getItem(5), other), "Different NBT cannot be merged");
            helper.assertTrue(furnace.getItem(6).getCount() == 16 && furnace.getItem(7).getCount() == 16
                    && furnace.getItem(8).getCount() == 3, "All 20 results split within the 16-item stack limit");
            for (int slot = 6; slot <= 8; slot++) {
                helper.assertTrue(ItemStack.isSameItemSameTags(furnace.getItem(slot), result), "Result NBT retained");
            }
            helper.assertTrue(result.getCount() == 20 && furnace.getItem(1).isEmpty(), "Recipe prototype is never mutated");
        });
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void alloyDuplicatesNeedSeparatePhysicalItems(GameTestHelper helper) {
        withRecipes(helper, List.of(alloy("duplicate", List.of(Ingredient.of(Items.DIAMOND), Ingredient.of(Items.DIAMOND)),
                new ItemStack(Items.GLOWSTONE_DUST))), () -> {
            Furnace furnace = new Furnace(helper, EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING, new ItemStack(Items.DIAMOND));
            furnace.getEnergyStorage().setEnergy(1000);
            furnace.update();
            for (int tick = 0; tick < 20; tick++) furnace.recipeTick(furnace.getEnergyStorage());
            helper.assertTrue(outputCount(furnace) == 0 && furnace.getItem(1).getCount() == 1
                    && furnace.getEnergyStorage().getStoredEnergy() == 1000, "One item cannot satisfy two ingredients");
            furnace.getItem(1).setCount(2);
            furnace.tickRecipe();
            helper.assertTrue(furnace.cookTime() == 1, "Enough items activate the alloy recipe");
            furnace.getItem(1).setCount(1);
            furnace.tickRecipe();
            helper.assertTrue(outputCount(furnace) == 0 && furnace.cookTime() == 0, "Removing a required item invalidates progress");
            furnace.getItem(1).setCount(2);
            furnace.finishBatch(helper);
            helper.assertTrue(count(furnace, Items.GLOWSTONE_DUST) == 1 && furnace.getItem(1).isEmpty(),
                    "Two items from the same stack are consumed exactly once");
        });
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void overlappingAlloyIngredientsUseAValidAssignment(GameTestHelper helper) {
        withRecipes(helper, List.of(alloy("overlap", List.of(Ingredient.of(Items.DIAMOND, Items.EMERALD),
                Ingredient.of(Items.DIAMOND)), new ItemStack(Items.GLOWSTONE_DUST))), () -> {
            Furnace furnace = new Furnace(helper, EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING,
                    new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD));
            furnace.getEnergyStorage().setEnergy(1000);
            furnace.update();
            furnace.finishBatch(helper);
            helper.assertTrue(count(furnace, Items.GLOWSTONE_DUST) == 1 && furnace.getItem(1).isEmpty()
                    && furnace.getItem(2).isEmpty(), "Broad ingredient must leave the diamond for the specific ingredient");
        });
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void modeSwitchAndSaveLoadKeepOnlyValidProgress(GameTestHelper helper) {
        withRecipes(helper, List.of(alloy("persist", List.of(Ingredient.of(Items.DIAMOND), Ingredient.of(Items.EMERALD)),
                new ItemStack(Items.GLOWSTONE_DUST))), () -> {
            Furnace furnace = new Furnace(helper);
            for (int tick = 0; tick < 20; tick++) furnace.tickRecipe();
            furnace.setMode(EtrionicBlastFurnaceBlockEntity.Mode.ALLOYING);
            furnace.clearContent();
            furnace.items().set(1, new ItemStack(Items.DIAMOND));
            furnace.items().set(2, new ItemStack(Items.EMERALD));
            for (int tick = 0; tick < 3; tick++) furnace.tickRecipe();
            helper.assertTrue(furnace.cookTime() == 3 && outputCount(furnace) == 0, "Mode switch cannot inherit blasting progress");
            CompoundTag saved = furnace.saveState();
            Furnace restored = new Furnace(helper);
            restored.load(saved.copy());
            restored.update();
            helper.assertTrue(restored.cookTime() == 3, "Valid work resumes after save/load");
            for (int tick = 0; tick < 4; tick++) restored.tickRecipe();
            helper.assertTrue(count(restored, Items.GLOWSTONE_DUST) == 1, "Resumed alloy finishes at seven paid ticks");
            Furnace changed = new Furnace(helper);
            changed.load(saved.copy());
            changed.getItem(1).shrink(1);
            changed.update();
            helper.assertTrue(changed.cookTime() == 0 && outputCount(changed) == 0, "Invalid restored inputs cannot retain progress");
            CompoundTag oldFormat = saved.copy();
            oldFormat.remove("SenDimsFurnaceBatch");
            Furnace upgraded = new Furnace(helper);
            upgraded.load(oldFormat);
            upgraded.update();
            helper.assertTrue(upgraded.cookTime() == 0 && upgraded.getItem(1).is(Items.DIAMOND),
                    "Old saves discard unverified progress while preserving their inventory");
        });
        helper.succeed();
    }

    @GameTest(template = "blast_furnace_test")
    public static void recipeReloadAndNbtChangesInvalidateOldWork(GameTestHelper helper) {
        BlastingRecipe first = blasting("reload", Items.STICK, new ItemStack(Items.DIAMOND), 7);
        withRecipes(helper, List.of(first), () -> {
            Furnace furnace = new Furnace(helper, EtrionicBlastFurnaceBlockEntity.Mode.BLASTING, new ItemStack(Items.STICK));
            for (int tick = 0; tick < 3; tick++) furnace.tickRecipe();
            furnace.getItem(1).getOrCreateTag().putString("changed", "in place");
            furnace.tickRecipe();
            helper.assertTrue(furnace.cookTime() == 1, "In-place NBT changes invalidate the input snapshot");
            var manager = helper.getLevel().getRecipeManager();
            List<Recipe<?>> reloaded = new ArrayList<>(manager.getRecipes());
            reloaded.removeIf(recipe -> recipe.getId().equals(first.getId()));
            reloaded.add(blasting("reload", Items.STICK, new ItemStack(Items.EMERALD), 9));
            manager.replaceRecipes(reloaded);
            furnace.tickRecipe();
            helper.assertTrue(furnace.cookTime() == 1 && furnace.cookTimeTotal() == 9, "Recipe replacement starts fresh work");
            furnace.finishBatch(helper);
            helper.assertTrue(count(furnace, Items.DIAMOND) == 0 && count(furnace, Items.EMERALD) == 1,
                    "Only the current recipe can produce its result");
        });
        helper.succeed();
    }
}
