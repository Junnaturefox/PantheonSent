package net.threetag.pantheonsent.compat.rei.fabric;

import com.google.common.collect.Lists;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.threetag.pantheonsent.block.ArcheologyTableBlock;
import net.threetag.pantheonsent.block.PSBlocks;

import java.util.List;

public class RestorationCategory implements DisplayCategory<RestorationDisplay> {

    @Override
    public CategoryIdentifier getCategoryIdentifier() {
        return PantheonSentREIServerPlugin.RESTORATION;
    }

    @Override
    public Component getTitle() {
        return ArcheologyTableBlock.CONTAINER_TITLE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(PSBlocks.ARCHEOLOGY_TABLE.get());
    }

    @Override
    public List<Widget> setupDisplay(RestorationDisplay display, Rectangle bounds) {
        Point startPoint = new Point(bounds.getCenterX() - 31, bounds.getCenterY() - 13);
        List<Widget> widgets = Lists.newArrayList();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 27, startPoint.y + 4)));
        widgets.add(Widgets.createResultSlotBackground(new Point(startPoint.x + 61, startPoint.y + 5)));
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 4 - 22, startPoint.y + 5)).entries(display.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 4, startPoint.y + 5)).entries(display.getInputEntries().get(1)).markInput());
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 61, startPoint.y + 5)).entries(display.getOutputEntries().get(0)).disableBackground().markOutput());
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 36;
    }
}
