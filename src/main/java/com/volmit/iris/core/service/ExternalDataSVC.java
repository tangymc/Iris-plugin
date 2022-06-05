/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2022 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.volmit.iris.core.service;

import com.volmit.iris.Iris;
import com.volmit.iris.core.link.ExternalDataProvider;
import com.volmit.iris.core.link.ItemAdderDataProvider;
import com.volmit.iris.core.link.OraxenDataProvider;
import com.volmit.iris.util.collection.KList;
import com.volmit.iris.util.plugin.IrisService;
import lombok.Data;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.MissingResourceException;
import java.util.Optional;

@Data
public class ExternalDataSVC implements IrisService {

    private KList<ExternalDataProvider> providers = new KList<>();

    @Override
    public void onEnable() {
        addProvider(new OraxenDataProvider(), new ItemAdderDataProvider());
    }

    @Override
    public void onDisable() { }

    public void addProvider(ExternalDataProvider... provider) {
        for(ExternalDataProvider p : provider) {
            if(p.getPlugin() != null) {
                providers.add(p);
                p.init();
            }
        }
    }

    public Optional<BlockData> getBlockData(NamespacedKey key) {
        Optional<ExternalDataProvider> provider = providers.stream().filter(p -> p.isPresent() && p.isValidProvider(key)).findFirst();
        if(provider.isEmpty())
            return Optional.empty();
        try {
            return Optional.of(provider.get().getBlockData(key));
        } catch(MissingResourceException e) {
            Iris.error(e.getMessage() + " - [" + e.getClassName() + ":" + e.getKey() + "]");
            return Optional.empty();
        }
    }

    public Optional<ItemStack> getItemStack(NamespacedKey key) {
        Optional<ExternalDataProvider> provider = providers.stream().filter(p -> p.isPresent() && p.isValidProvider(key)).findFirst();
        if(provider.isEmpty())
            return Optional.empty();
        try {
            return Optional.of(provider.get().getItemStack(key));
        } catch(MissingResourceException e) {
            Iris.error(e.getMessage() + " - [" + e.getClassName() + ":" + e.getKey() + "]");
            return Optional.empty();
        }
    }

    public NamespacedKey[] getAllIdentifiers() {
        KList<NamespacedKey> names = new KList<>();
        providers.stream().filter(ExternalDataProvider::isPresent).forEach(p -> names.add(p.getBlockTypes()));
        return names.toArray(new NamespacedKey[0]);
    }
}