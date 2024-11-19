package de.skyh.skyhaven.config;

import de.skyh.skyhaven.Skyhaven;
import de.skyh.skyhaven.config.gui.SkyhConfigPreview;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiSlider;

import java.util.*;

/**
 * A config category, contains elements and logic for one specific config category
 */
public class SkyhConfigCategory {
    private final String displayName;
    private final String configName;
    private String menuDisplayName;
    private final List<SubCategory> subCategories;

    public SkyhConfigCategory(String displayName, String configName) {
        this.displayName = displayName;
        this.configName = configName;
        this.menuDisplayName = displayName;
        subCategories = new ArrayList<>();
    }

    public void setMenuDisplayName(String menuDisplayName) {
        this.menuDisplayName = menuDisplayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getMenuDisplayName() {
        return menuDisplayName;
    }

    public List<SubCategory> getSubCategories() {
        return subCategories;
    }

    public SubCategory addSubCategory(String subCatName) {
        SubCategory subCategory = new SubCategory(subCatName);
        subCategories.add(subCategory);
        return subCategory;
    }


    public static class SubCategory {
        private final String displayName;
        private final List<String> explanations = new ArrayList<>();
        private final List<Property> configEntries = new ArrayList<>();
        private final Map<String, GuiSliderExtra> guiSliderExtras = new HashMap<>();
        /**
         * Index 0: preview for sub category
         * Index >0: preview for one specific property
         */
        private final Map<Integer, SkyhConfigPreview> previews = new HashMap<>();

        public SubCategory(String subCatName) {
            this.displayName = subCatName;
        }

        public Property addConfigEntry(Property configEntry, SkyhConfigPreview configEntryPreview) {
            Property property = addConfigEntry(configEntry);
            addPropertyPreview(property, configEntryPreview);
            return property;
        }

        public Property addConfigEntry(Property property, String prefix, String suffix, GuiSlider.ISlider onChangeSliderValue) {
            guiSliderExtras.put(property.getLanguageKey(), new GuiSliderExtra(prefix, suffix, onChangeSliderValue));
            return addConfigEntry(property);
        }

        public Property addConfigEntry(Property configEntry) {
            configEntry.setLanguageKey(Skyhaven.MODID + ".config." + configEntry.getName());
            configEntries.add(configEntry);
            return configEntry;
        }

        public void addPropertyPreview(Property property, SkyhConfigPreview preview) {
            int propId = configEntries.indexOf(property);
            if (propId > -1) {
                this.previews.put(propId + 1, preview);
            }
        }

        public void addExplanations(String... explanations) {
            if (this.explanations.isEmpty()) {
                // first line is only used for the explanations tooltips, not for in-text:
                this.explanations.add(EnumChatFormatting.DARK_GREEN + "❢ " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + displayName);
            }
            this.explanations.addAll(Arrays.asList(explanations));
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getExplanations() {
            return explanations;
        }

        public List<Property> getConfigEntries() {
            return configEntries;
        }

        public GuiSliderExtra getGuiSliderExtra(String propertyKey) {
            return guiSliderExtras.get(propertyKey);
        }

        public SkyhConfigPreview getPreview(Property property) {
            int propId = configEntries.indexOf(property);
            if (propId > -1) {
                return this.previews.get(propId + 1);
            }
            return null;
        }

        public Map<Integer, SkyhConfigPreview> getPreviews() {
            return previews;
        }

        public static class GuiSliderExtra {
            private final String prefix;
            private final String suffix;
            private final GuiSlider.ISlider onChangeSliderValue;

            public GuiSliderExtra(String prefix, String suffix, GuiSlider.ISlider onChangeSliderValue) {
                this.prefix = prefix;
                this.suffix = suffix;
                this.onChangeSliderValue = onChangeSliderValue;
            }

            public String getPrefix() {
                return prefix != null ? prefix : "";
            }

            public String getSuffix() {
                return suffix != null ? suffix : "";
            }

            public GuiSlider.ISlider getOnChangeSliderValue() {
                return onChangeSliderValue;
            }
        }
    }
}
