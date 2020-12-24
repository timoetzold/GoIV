package com.kamron.pogoiv.devMethods.gameMasterParser;

import com.google.gson.Gson;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.Form;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.FormSettings;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.Data;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.PogoJson;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.Pokemon;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.Stats;
import com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct.Template;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationDatabaseUpdater {

    static private final String integerArrayFormat = " <item>%d</item>";
    static private final String stringArrayFormat = " <item>%s</item>";
    static private final String commentFormat = " <!-- %s -->\n";

    public static void main(String[] args) {

        List<Data> data = getDataFromGamemasterJson();

        HashMap<Integer, FormSettings> formsByPokedex = new HashMap<>(); // Stores form data index by NatDex number
        ArrayList<FormSettings> pokemonWithMultipleForms = new ArrayList<>(); // stores form data for pokemon with
        // multiple forms
        HashMap<String, HashMap<String, Pokemon>> pokemonFormsByName = new HashMap<>(); // stores pokemon data by
        // species and form id
        HashMap<String, Integer> dexNumberLookup = new HashMap<>(); // species name -> dex #

        for (Data datum : data) {
            Pokemon poke = datum.getPokemon();
            if (poke != null) {
                poke.setTemplateId(datum.getTemplateId()); // Inject template ID directly into pokemon object

                // Add this pokemon to the map for it's species, indexed by the form name
                pokemonFormsByName.putIfAbsent(poke.getUniqueId(), new HashMap<>());
                pokemonFormsByName.get(poke.getUniqueId()).put(poke.getForm(), poke);
            }

            if (datum.getFormSettings() != null) {
                FormSettings form = new SpecificFormSettings(datum.getFormSettings());

                // Extract the NatDex number for the pokemon from the template ID
                int dexNumber = Integer.parseInt(datum.getTemplateId().substring(7, 11));
                formsByPokedex.put(dexNumber, form);
                dexNumberLookup.put(form.getName(), dexNumber);

                if (form.getForms() != null && form.getForms().size() > 1) {
                    // Multiple forms; handle with special care
                    pokemonWithMultipleForms.add(form);
                }
            }
        }

        dexNumberLookup.put(null, 0); // Sneaky addition to dodge an if in printIntegers

        printIntegersXml(formsByPokedex, pokemonWithMultipleForms, pokemonFormsByName, dexNumberLookup);
        printFormsXml(pokemonWithMultipleForms, pokemonFormsByName);
        printTypeDifferencesSuggestions(pokemonWithMultipleForms, pokemonFormsByName, formsByPokedex, dexNumberLookup);
    }

    private static void printTypeDifferencesSuggestions(ArrayList<FormSettings> pokemonWithMultipleForms,
                                                        HashMap<String, HashMap<String, Pokemon>> pokemonFormsByName,
                                                        HashMap<Integer, FormSettings> formsByPokedex,
                                                        HashMap<String, Integer> dexNumberLookup) {
        System.out.println("Here's type difference suggestions to allow GoIV to differentiate between pokemon forms:\n");

        for (FormSettings formSetting : pokemonWithMultipleForms) {
            HashMap<String, Pokemon> formHash = pokemonFormsByName.get(formSetting.getName());

            HashMap<String, Integer> typeCounter = new HashMap<>();
            for (Pokemon pokemon : formHash.values()) {
                String type1 = pokemon.getType1();
                String type2 = pokemon.getType2();
                int count1 = typeCounter.containsKey(type1) ? typeCounter.get(type1) : 0;
                int count2 = typeCounter.containsKey(type2) ? typeCounter.get(type2) : 0;
                typeCounter.put(type1, count1 + 1);
                typeCounter.put(type2, count2 + 1);
            }

            final boolean[] hasUnique = {false};
            typeCounter.forEach((s, integer) -> {
                if (integer == 1) {
                    System.out.println(s + " is unique for " + formSetting.getName() + " #" + dexNumberLookup.get(formSetting.getName()));
                    hasUnique[0] = true;
                }
            });
            if (!hasUnique[0]) {
                System.out.println(formSetting.getName() + " has no unique typing. :((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((");
            }

        }
    }

    /**
     * Reads the V2_GAME_MASTER.json located in the project root directory, and returns a List<ItemTemplate> containing
     * all the data contained in the Json.
     *
     * @return a List of all data contained in the Json
     */
    private static List<Data> getDataFromGamemasterJson() {
        URL url = null;
        try {
            url = new URL("https://raw.githubusercontent.com/pokemongo-dev-contrib/pokemongo-game-master/master"
                    + "/versions/latest/V2_GAME_MASTER.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PogoJson json = new Gson().fromJson(reader, PogoJson.class);

        // in V2, json has an extra layer of nesting: { "template": [{"data": {"templateId": "foo", ...} }] }
        return json.getTemplates().stream().map(Template::getData).collect(Collectors.toList());
    }

    /**
     * Prints out the contents that should be in Integers.xml
     *  @param formsByPokedex - Form data stored by NatDex number
     * @param pokemonWithMultipleForms - Form data for pokemon with multiple forms
     * @param pokemonFormsByName - Pokemon data by species and form ID
     * @param dexNumberLookup
     */
    private static void printIntegersXml(HashMap<Integer, FormSettings> formsByPokedex,
                                         ArrayList<FormSettings> pokemonWithMultipleForms,
                                         HashMap<String, HashMap<String, Pokemon>> pokemonFormsByName,
                                         HashMap<String, Integer> dexNumberLookup) {
        // Full file
        StringBuilder integersXmlBuilder = new StringBuilder();

        // Attack value of default form for each pokedex entry (no forms considered)
        StringBuilder attackBuilder = new StringBuilder();
        Formatter attackFormatter = new Formatter(attackBuilder);

        // Defense value of default form for each pokedex entry (no forms considered)
        StringBuilder defenseBuilder = new StringBuilder();
        Formatter defenseFormatter = new Formatter(defenseBuilder);

        // Stamina value of default form for each pokedex entry (no forms considered)
        StringBuilder staminaBuilder = new StringBuilder();
        Formatter staminaFormatter = new Formatter(staminaBuilder);

        // Parent evolution information for each pokedex entry (no forms considered)
        StringBuilder devolutionNumberBuilder = new StringBuilder();
        Formatter devolutionNumberFormatter = new Formatter(devolutionNumberBuilder);

        // Candy evolution cost of default form for each pokedex entry (no forms considered)
        StringBuilder evolutionCandyCostBuilder = new StringBuilder();
        Formatter evolutionCandyCostFormatter = new Formatter(evolutionCandyCostBuilder);

        // Candy name for each pokedex entry (no forms considered)
        StringBuilder candyNamesBuilder = new StringBuilder();
        Formatter candyNamesFormatter = new Formatter(candyNamesBuilder);

        StringBuilder formsCountIndexBuilder = new StringBuilder();
        Formatter formsCountIndexFormatter = new Formatter(formsCountIndexBuilder);

        // Seed initial text
        integersXmlBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
        attackBuilder.append("<integer-array name=\"attack\">\n");
        defenseBuilder.append("<integer-array name=\"defense\">\n");
        staminaBuilder.append("<integer-array name=\"stamina\">\n");
        devolutionNumberBuilder.append("<integer-array name=\"devolutionNumber\">\n");
        candyNamesBuilder.append("<integer-array name=\"candyNames\">\n");
        formsCountIndexBuilder.append("<integer-array name=\"formsCountIndex\">\n");

        int maxPokedex = Collections.max(formsByPokedex.keySet());
        for (int i = 1; i <= maxPokedex; i++) {
            if (formsByPokedex.containsKey(i)) {
                FormSettings form = formsByPokedex.get(i);
                HashMap<String, Pokemon> formHash = pokemonFormsByName.get(form.getName());
                if (formHash == null) continue; // Some pokemon have form data in the game, but not pokemon data??
                Pokemon poke = formHash.get(null);
                String pokemonName = titleCase(form.getName());
                Stats stats = poke.getStats();

                attackFormatter.format(integerArrayFormat, stats.getBaseAttack()).format(commentFormat, pokemonName);
                defenseFormatter.format(integerArrayFormat, stats.getBaseDefense()).format(commentFormat, pokemonName);
                staminaFormatter.format(integerArrayFormat, stats.getBaseStamina()).format(commentFormat, pokemonName);

                // Devolution Number
                devolutionNumberFormatter.format(integerArrayFormat, dexNumberLookup.get(poke.getParentId()) - 1);
                devolutionNumberFormatter.format(commentFormat, pokemonName);

                // Evolution Candy Cost
                Integer evolveCandy = null;
                if (poke.getEvolutionBranches() != null) {
                    evolveCandy = poke.getEvolutionBranches().get(0).getCandyCost();
                }
                if (evolveCandy == null) {
                    evolveCandy = poke.getCandyToEvolve();
                }
                if (evolveCandy == null) {
                    evolveCandy = -1;
                }
                evolutionCandyCostFormatter.format(integerArrayFormat, evolveCandy).format(commentFormat, pokemonName);

                // Candy Names
                candyNamesFormatter.format(integerArrayFormat, dexNumberLookup.get(poke.getFamilyId().substring(7)));
                candyNamesFormatter.format(commentFormat, pokemonName);

                // Forms Count Index
                formsCountIndexFormatter.format(integerArrayFormat, pokemonWithMultipleForms.indexOf(form));
                formsCountIndexFormatter.format(commentFormat, pokemonName);
            }
        }

        // Add the line to close all the xml arrays
        attackBuilder.append("</integer-array>\n");
        defenseBuilder.append("</integer-array>\n");
        staminaBuilder.append("</integer-array>\n");
        devolutionNumberBuilder.append("</integer-array>\n");
        evolutionCandyCostBuilder.append("</integer-array>\n");
        candyNamesBuilder.append("</integer-array>\n");
        formsCountIndexBuilder.append("</integer-array>\n");

        // Add all the xml arrays to the main XML
        integersXmlBuilder.append(attackBuilder.toString());
        integersXmlBuilder.append(defenseBuilder.toString());
        integersXmlBuilder.append(staminaBuilder.toString());
        integersXmlBuilder.append(devolutionNumberBuilder.toString());
        integersXmlBuilder.append(evolutionCandyCostBuilder.toString());
        integersXmlBuilder.append(candyNamesBuilder.toString());
        integersXmlBuilder.append(formsCountIndexBuilder.toString());

        // Finishing touches
        integersXmlBuilder.append("</resources>\n");

        //System.out.println(stringBuilder);
        writeFile("integers.xml", integersXmlBuilder.toString());
    }

    /**
     * Prints out all the contents that should be in forms.xml
     *
     * @param pokemonWithMultipleForms - Form data for pokemon with multiple forms
     * @param pokemonFormsByName - Pokemon data by species and form ID
     */
    private static void printFormsXml(ArrayList<FormSettings> pokemonWithMultipleForms, HashMap<String, HashMap<String, Pokemon>> pokemonFormsByName) {
        // Full file
        StringBuilder formsXmlBuilder = new StringBuilder();

        // The number of forms for each multi-form pokemon
        StringBuilder formsCountBuilder = new StringBuilder();
        Formatter formsCountFormatter = new Formatter(formsCountBuilder);

        // The form name of each form for each multi-form pokemon
        StringBuilder formNamesBuilder = new StringBuilder();
        Formatter formNamesFormatter = new Formatter(formNamesBuilder);

        // The attack value of each form for each multi-form pokemon
        StringBuilder formAttackBuilder = new StringBuilder();
        Formatter formAttackFormatter = new Formatter(formAttackBuilder);

        // The defense value of each form for each multi-form pokemon
        StringBuilder formDefenseBuilder = new StringBuilder();
        Formatter formDefenseFormatter = new Formatter(formDefenseBuilder);

        // The stamina value of each form for each multi-form pokemon
        StringBuilder formStaminaBuilder = new StringBuilder();
        Formatter formStaminaFormatter = new Formatter(formStaminaBuilder);

        // Seed initial text
        formsXmlBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
        formsCountBuilder.append("<integer-array name=\"formsCount\">\n");
        formNamesBuilder.append("<string-array name=\"formNames\">\n");
        formAttackBuilder.append("<integer-array name=\"formAttack\">\n");
        formDefenseBuilder.append("<integer-array name=\"formDefense\">\n");
        formStaminaBuilder.append("<integer-array name=\"formStamina\">\n");

        for (FormSettings form : pokemonWithMultipleForms) {
            String pokemonName = titleCase(form.getName());
            HashMap<String, Pokemon> formHash = pokemonFormsByName.get(form.getName());

            formsCountFormatter.format(integerArrayFormat, form.getForms().size()).format(commentFormat, pokemonName);

            formNamesFormatter.format(commentFormat, pokemonName);
            formAttackFormatter.format(commentFormat, pokemonName);
            formAttackFormatter.format(commentFormat, pokemonName);
            formAttackFormatter.format(commentFormat, pokemonName);
            for (Form subform : form.getForms()) {
                Pokemon poke = formHash.get(formHash.containsKey(subform.getForm())? subform.getForm() : null);
                Stats stats = poke.getStats();
                String formName = formName(subform.getForm(), form.getName());

                // Form Names
                formNamesFormatter.format(stringArrayFormat, formName);

                // Form Stats
                formAttackFormatter.format(integerArrayFormat, unnull(stats.getBaseAttack(), -1));
                formAttackFormatter.format(commentFormat, formName);
                formDefenseFormatter.format(integerArrayFormat, unnull(stats.getBaseDefense(), -1));
                formDefenseFormatter.format(commentFormat, formName);
                formStaminaFormatter.format(integerArrayFormat, unnull(stats.getBaseStamina(), -1));
                formStaminaFormatter.format(commentFormat, formName);
            }
        }

        // Add the line to close all the xml arrays
        formsCountBuilder.append("</integer-array>\n");
        formNamesBuilder.append("</string-array>\n");
        formAttackBuilder.append("</integer-array>\n");
        formDefenseBuilder.append("</integer-array>\n");
        formStaminaBuilder.append("</integer-array>\n");

        // Add all the xml arrays to the main XML
        formsXmlBuilder.append(formsCountBuilder.toString());
        formsXmlBuilder.append(formNamesBuilder.toString());
        formsXmlBuilder.append(formAttackBuilder.toString());
        formsXmlBuilder.append(formDefenseBuilder.toString());
        formsXmlBuilder.append(formStaminaBuilder.toString());

        // finishing touches
        formsXmlBuilder.append("</resources>\n");

        //System.out.println(stringBuilder);
        writeFile("forms.xml", formsXmlBuilder.toString());
    }

    /**
     * Converts a string like 'RAICHU_ALOLAN' to 'Alolan Form', or 'ARCEUS_DARK' to 'Dark Form'.
     *
     * @param form - Form name
     * @param pokemon - Pokemon Species name
     * @return a human readable description of the form
     */
    private static String formName(String form, String pokemon) {
        form = form.replaceFirst("MEWTWO_A", "MEWTWO_Armored"); // For some reason Mewtwo is weird
        String simpleName = form.substring(pokemon.length() + 1); // Skip past pokemon name and underscore
        return titleCase(simpleName) + " Form";
    }

    /**
     * Guarantees a non-null value
     * @param nullish - value to check
     * @param fallback - backup in case the value is null
     * @param <T> - Value's Type
     * @return a non-null value
     */
    private static <T> T unnull(T nullish, T fallback) {
        return (nullish == null)? fallback : nullish;
    }

    /**
     * Converts text such as "THIS IS annoyingly WEIRd CapitaLIZATION" to "This Is Annoyingly Weird Capitalization" - capitalizing each word.
     *
     * @param str - annoying text
     * @return aesthetic text
     */
    private static String titleCase(String str) {
        return Arrays.stream(str.split("[ _]")).map(word -> Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase()).collect(Collectors.joining(" "));
    }

    private static void writeFile(String fileName, String content) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), "utf-8"))) {
            writer.write(content);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
