/*  Copyright (C) 2022-2024 Cédric Bellegarde, Davis Mosenkovs, José Rebelo,
    roolx, ssilverr, thirschbuechler

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.util.language;

import static id.icapps.savera.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_LANGUAGES;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.icapps.savera.GBApplication;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.util.Prefs;
import id.icapps.savera.util.language.impl.ArabicTransliterator;
import id.icapps.savera.util.language.impl.ArmenianTransliterator;
import id.icapps.savera.util.language.impl.BengaliTransliterator;
import id.icapps.savera.util.language.impl.CommonSymbolsTransliterator;
import id.icapps.savera.util.language.impl.CroatianTransliterator;
import id.icapps.savera.util.language.impl.CzechTransliterator;
import id.icapps.savera.util.language.impl.EstonianTransliterator;
import id.icapps.savera.util.language.impl.ExtendedAsciiTransliterator;
import id.icapps.savera.util.language.impl.FlattenToAsciiTransliterator;
import id.icapps.savera.util.language.impl.FrenchTransliterator;
import id.icapps.savera.util.language.impl.GeorgianTransliterator;
import id.icapps.savera.util.language.impl.GermanTransliterator;
import id.icapps.savera.util.language.impl.GreekTransliterator;
import id.icapps.savera.util.language.impl.HebrewTransliterator;
import id.icapps.savera.util.language.impl.HungarianTransliterator;
import id.icapps.savera.util.language.impl.IcelandicTransliterator;
import id.icapps.savera.util.language.impl.KoreanTransliterator;
import id.icapps.savera.util.language.impl.LatvianTransliterator;
import id.icapps.savera.util.language.impl.LithuanianTransliterator;
import id.icapps.savera.util.language.impl.PersianTransliterator;
import id.icapps.savera.util.language.impl.PolishTransliterator;
import id.icapps.savera.util.language.impl.RussianTransliterator;
import id.icapps.savera.util.language.impl.ScandinavianTransliterator;
import id.icapps.savera.util.language.impl.SerbianTransliterator;
import id.icapps.savera.util.language.impl.TurkishTransliterator;
import id.icapps.savera.util.language.impl.UkranianTransliterator;

public class LanguageUtils {
    private static final Logger LOG = LoggerFactory.getLogger(LanguageUtils.class);

    private static final Map<String, Transliterator> TRANSLITERATORS_MAP = new HashMap<String, Transliterator>() {{
        put("arabic", new ArabicTransliterator());
        put("bengali", new BengaliTransliterator());
        put("common_symbols", new CommonSymbolsTransliterator());
        put("croatian", new CroatianTransliterator());
        put("czech", new CzechTransliterator());
        put("estonian", new EstonianTransliterator());
        put("extended_ascii", new ExtendedAsciiTransliterator());
        put("french", new FrenchTransliterator());
        put("georgian", new GeorgianTransliterator());
        put("german", new GermanTransliterator());
        put("greek", new GreekTransliterator());
        put("hebrew", new HebrewTransliterator());
        put("hungarian", new HungarianTransliterator());
        put("icelandic", new IcelandicTransliterator());
        put("korean", new KoreanTransliterator());
        put("latvian", new LatvianTransliterator());
        put("lithuanian", new LithuanianTransliterator());
        put("persian", new PersianTransliterator());
        put("polish", new PolishTransliterator());
        put("russian", new RussianTransliterator());
        put("scandinavian", new ScandinavianTransliterator());
        put("serbian", new SerbianTransliterator());
        put("turkish", new TurkishTransliterator());
        put("ukranian", new UkranianTransliterator());
        put("armenian", new ArmenianTransliterator());
    }};

    /**
     * Get a {@link Transliterator} for a specific language.
     *
     * @param language the language
     * @return the transliterator, if it exists
     * @throws IllegalArgumentException if there is no transliterator for the provided language
     */
    public static Transliterator getTransliterator(final String language) {
        if (!TRANSLITERATORS_MAP.containsKey(language)) {
            throw new IllegalArgumentException(String.format("Transliterator for %s not found", language));
        }

        return TRANSLITERATORS_MAP.get(language);
    }

    /**
     * Get the configured transliterator for the provided {@link GBDevice}, if any.
     *
     * @param device the device
     * @return the configured transliterator, null if not configured
     */
    @Nullable
    public static Transliterator getTransliterator(final GBDevice device) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final String transliterateLanguagesPref = devicePrefs.getString(PREF_TRANSLITERATION_LANGUAGES, "");

        if (transliterateLanguagesPref.isEmpty()) {
            return null;
        }

        final List<String> languages = Arrays.asList(transliterateLanguagesPref.split(","));
        final List<Transliterator> transliterators = new ArrayList<>(languages.size());

        for (String language : languages) {
            if (!TRANSLITERATORS_MAP.containsKey(language)) {
                LOG.warn("Transliterator for {} not found", language);
                continue;
            }

            transliterators.add(TRANSLITERATORS_MAP.get(language));
        }

        if (!coordinator.supportsUnicodeEmojis()) {
            // For now, assume that if the device does not support unicode emoji, it also doesn't
            // support utf, so flatten to ASCII. This allows for devices that support unicode
            // characters to still use transliterators for languages not supported by the device,
            // and still get emoji
            // TODO: Maybe this should be configurable, or at least separate from the emoji setting
            transliterators.add(new FlattenToAsciiTransliterator());
        }

        return new MultiTransliterator(transliterators);
    }
}
