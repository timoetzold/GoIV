package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.FractionPowerUpBinding;
import com.kamron.pogoiv.scanlogic.CPRange;
import com.kamron.pogoiv.scanlogic.Data;
import com.kamron.pogoiv.scanlogic.IVCombination;
import com.kamron.pogoiv.scanlogic.PokeInfoCalculator;
import com.kamron.pogoiv.scanlogic.PokeSpam;
import com.kamron.pogoiv.scanlogic.Pokemon;
import com.kamron.pogoiv.scanlogic.ScanResult;
import com.kamron.pogoiv.scanlogic.UpgradeCost;
import com.kamron.pogoiv.utils.GUIColorFromPokeType;
import com.kamron.pogoiv.utils.ReactiveColorListener;
import com.kamron.pogoiv.utils.fractions.Fraction;
import com.kamron.pogoiv.widgets.PokemonSpinnerAdapter;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class PowerUpFraction extends Fraction implements ReactiveColorListener {

    private final Context context;
    private final Pokefly pokefly;

    private FractionPowerUpBinding binding;

    private PokemonSpinnerAdapter extendedEvolutionSpinnerAdapter;
    private ColorStateList exResLevelDefaultColor;


    public PowerUpFraction(@NonNull Pokefly pokefly) {
        this.context = pokefly;
        this.pokefly = pokefly;
    }

    @Override
    public void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        binding = FractionPowerUpBinding.inflate(inflater, parent, attachToParent);

        exResLevelDefaultColor = binding.exResLevel.getTextColors();

        createExtendedResultLevelSeekbar();
        createExtendedResultEvolutionSpinner();
        adjustSeekbarsThumbs();
        populateAdvancedInformation();

        updateGuiColors();
        GUIColorFromPokeType.getInstance().setListenTo(this);

        binding.ivButton.setOnClickListener(view -> onIV());
        binding.movesetButton.setOnClickListener(view -> onMoveset());
        binding.btnBack.setOnClickListener(view -> onBack());
        binding.btnClose.setOnClickListener(view -> onClose());

        binding.exResultPercentPerfection.setOnClickListener(view -> explainCPPercentageComparedToMaxIV());
        binding.btnDecrementLevelExpanded.setOnClickListener(view -> decrementLevelExpanded());
        binding.btnIncrementLevelExpanded.setOnClickListener(view -> incrementLevelExpanded());
    }



    @Override public void onDestroy() {
        GUIColorFromPokeType.getInstance().removeListener(this);
        binding = null;
    }

    @Override
    public Anchor getAnchor() {
        return Anchor.BOTTOM;
    }

    @Override
    public int getVerticalOffset(@NonNull DisplayMetrics displayMetrics) {
        return 0;
    }

    void onIV() {
        pokefly.navigateToIVResultFraction();
    }

    void onMoveset() {
        pokefly.navigateToMovesetFraction();
    }

    void onBack() {
        pokefly.navigateToPreferredStartFraction();
    }

    void onClose() {
        pokefly.closeInfoDialog();
    }

    /**
     * Sets the growth estimate text boxes to correspond to the
     * pokemon evolution and level set by the user.
     */
    public void populateAdvancedInformation() {
        double selectedLevel = seekbarProgressToLevel(binding.expandedLevelSeekbar.getProgress());
        Pokemon selectedPokemon = initPokemonSpinnerIfNeeded(Pokefly.scanResult.pokemon);

        setEstimateCpTextBox(Pokefly.scanResult, selectedLevel, selectedPokemon);
        setEstimateHPTextBox(Pokefly.scanResult, selectedLevel, selectedPokemon);
        setPokemonPerfectionPercentageText(Pokefly.scanResult, selectedLevel, selectedPokemon);
        setEstimateCostTextboxes(Pokefly.scanResult, selectedLevel, selectedPokemon, Pokefly.scanResult.isLucky);
        binding.exResLevel.setText(String.valueOf(selectedLevel));
        setEstimateLevelTextColor(selectedLevel);

        setAndCalculatePokeSpamText(Pokefly.scanResult);
    }

    /**
     * Initialize the pokemon spinner in the evolution and powerup box in the result window, and return picked pokemon.
     * <p/>
     * The method will populate the spinner with the correct pokemon evolution line, and disable the spinner if there's
     * the evolution line contains only one pokemon. The method will also select by default either the evolution of
     * the scanned pokemon (if there is one) or the pokemon itself.
     * <p/>
     * This method only does anything if it detects that the spinner was not previously initialized.
     *
     * @param scannedPokemon the pokemon to use for selecting a good default, if init is performed
     */
    private Pokemon initPokemonSpinnerIfNeeded(Pokemon scannedPokemon) {
        ArrayList<Pokemon> evolutionLine = PokeInfoCalculator.getInstance().getEvolutionLine(scannedPokemon);
        extendedEvolutionSpinnerAdapter.updatePokemonList(evolutionLine);

        int spinnerSelectionIdx = binding.extendedEvolutionSpinner.getSelectedItemPosition();

        if (spinnerSelectionIdx == -1) {
            if (!scannedPokemon.getEvolutions().isEmpty()) {
                scannedPokemon = scannedPokemon.getEvolutions().get(0);
            }
            // This happens at the beginning or after changing the pokemon list.
            //if initialising list, act as if scanned pokemon is marked
            for (int i = 0; i < evolutionLine.size(); i++) {
                if (evolutionLine.get(i).toString() == scannedPokemon.toString()) {
                    spinnerSelectionIdx = i;
                    break;
                }
            }
            //Invariant: evolutionLine.get(spinnerSelectionIdx).number == scannedPokemon.number., hence
            //evolutionLine.get(spinnerSelectionIdx) == scannedPokemon.
            binding.extendedEvolutionSpinner.setSelection(spinnerSelectionIdx);
            binding.extendedEvolutionSpinner.setEnabled(evolutionLine.size() > 1);
        }
        return extendedEvolutionSpinnerAdapter.getItem(spinnerSelectionIdx);
    }

    /**
     * Sets the "expected cp textview" to (+x) or (-y) in the powerup and evolution estimate box depending on what's
     * appropriate.
     *
     * @param scanResult    the ivscanresult of the current pokemon
     * @param selectedLevel   The goal level the pokemon in ivScanresult pokemon should reach
     * @param selectedPokemon The goal pokemon evolution he ivScanresult pokemon should reach
     */
    private void setEstimateCpTextBox(ScanResult scanResult, double selectedLevel, Pokemon selectedPokemon) {
        CPRange expectedRange = PokeInfoCalculator.getInstance().getCpRangeAtLevel(selectedPokemon,
                scanResult.getCombinationLowIVs(), scanResult.getCombinationHighIVs(), selectedLevel);
        int realCP = scanResult.cp;
        int expectedAverage = expectedRange.getAvg();

        binding.exResultCP.setText(String.valueOf(expectedAverage));

        String exResultCPStrPlus = "";
        int diffCP = expectedAverage - realCP;
        if (diffCP >= 0) {
            exResultCPStrPlus += " (+" + diffCP + ")";
        } else {
            exResultCPStrPlus += " (" + diffCP + ")";
        }
        binding.exResultCPPlus.setText(exResultCPStrPlus);
    }

    /**
     * Sets the "expected HP  textview" to the estimat HP in the powerup and evolution estimate box.
     *
     * @param scanResult  the ivscanresult of the current pokemon
     * @param selectedLevel The goal level the pokemon in ivScanresult pokemon should reach
     */
    private void setEstimateHPTextBox(ScanResult scanResult, double selectedLevel, Pokemon selectedPokemon) {
        int newHP = PokeInfoCalculator.getInstance().getHPAtLevel(scanResult, selectedLevel, selectedPokemon);

        binding.exResultHP.setText(String.valueOf(newHP));

        int oldHP = PokeInfoCalculator.getInstance().getHPAtLevel(
                scanResult, Pokefly.scanResult.levelRange.min, scanResult.pokemon);
        int hpDiff = newHP - oldHP;
        String sign = (hpDiff >= 0) ? "+" : ""; //add plus in front if positive.
        String hpTextPlus = " (" + sign + hpDiff + ")";
        binding.exResultHPPlus.setText(hpTextPlus);
    }

    /**
     * Sets the pokemon perfection % text in the powerup and evolution results box.
     *
     * @param scanResult    The object containing the ivs to base current pokemon on.
     * @param selectedLevel   Which level the prediction should me made for.
     * @param selectedPokemon The pokemon to compare selected iv with max iv to.
     */
    private void setPokemonPerfectionPercentageText(ScanResult scanResult,
                                                    double selectedLevel, Pokemon selectedPokemon) {
        CPRange cpRange = PokeInfoCalculator.getInstance().getCpRangeAtLevel(selectedPokemon,
                scanResult.getCombinationLowIVs(), scanResult.getCombinationHighIVs(),
                selectedLevel);
        double maxCP = PokeInfoCalculator.getInstance().getCpRangeAtLevel(selectedPokemon,
                IVCombination.MAX, IVCombination.MAX, selectedLevel).high;
        double perfection = (100.0 * cpRange.getFloatingAvg()) / maxCP;
        int difference = (int) (cpRange.getFloatingAvg() - maxCP);
        DecimalFormat df = new DecimalFormat("#.#");
        String sign = "";
        if (difference >= 0) {
            sign = "+";
        }
        String differenceString = "(" + sign + difference + ")";
        String perfectionString = df.format(perfection) + "% " + differenceString;
        binding.exResultPercentPerfection.setText(perfectionString);
    }

    /**
     * Sets the candy cost and stardust cost textfields in the powerup and evolution estimate box. The textviews are
     * populated with the cost in dust and candy required to go from the pokemon in ivscanresult to the desired
     * selecterdLevel and selectedPokemon.
     *
     * @param scanResult    The pokemon to base the estimate on.
     * @param selectedLevel   The level the pokemon needs to reach.
     * @param selectedPokemon The target pokemon. (example, ivScan pokemon can be weedle, selected can be beedrill.)
     * @param isLucky         Whether the pokemon is lucky, and costs half the normal amount of dust.
     */
    private void setEstimateCostTextboxes(ScanResult scanResult, double selectedLevel, Pokemon selectedPokemon,
                                          boolean isLucky) {
        UpgradeCost cost = PokeInfoCalculator.getInstance()
                .getUpgradeCost(selectedLevel, Pokefly.scanResult.levelRange.min, isLucky);
        int evolutionCandyCost = PokeInfoCalculator.getInstance()
                .getCandyCostForEvolution(scanResult.pokemon, selectedPokemon);
        String candyCostText = cost.candy + evolutionCandyCost + "";
        binding.exResCandy.setText(candyCostText);
        String candyXlCostText = Integer.toString(cost.candyXl);
        binding.exResXlCandy.setText(candyXlCostText);
        DecimalFormat formater = new DecimalFormat();
        binding.exResStardust.setText(formater.format(cost.dust));
    }

    /**
     * Sets the text color of the level next to the slider in the estimate box to normal or orange depending on if
     * the user can level up the pokemon that high with his current trainer level. For example, if the user has
     * trainer level 20, then his pokemon can reach a max level of 22 - so any goalLevel above 22 would become
     * orange.
     *
     * @param selectedLevel The level to reach.
     */
    private void setEstimateLevelTextColor(double selectedLevel) {
        // If selectedLevel exceeds trainer capabilities then show text in orange
        if (selectedLevel > Data.trainerLevelToMaxPokeLevel(pokefly.getTrainerLevel())) {
            binding.exResLevel.setTextColor(ContextCompat.getColor(pokefly, R.color.orange));
        } else {
            binding.exResLevel.setTextColor(exResLevelDefaultColor);
        }
    }


    /**
     * setAndCalculatePokeSpamText sets pokespamtext and makes it visible.
     *
     * @param scanResult ScanResult object that contains the scan results, mainly needed to get candEvolutionCost
     *                     variable
     */
    private void setAndCalculatePokeSpamText(ScanResult scanResult) {
        if (GoIVSettings.getInstance(pokefly).isPokeSpamEnabled()
                && scanResult.pokemon != null) {
            if (scanResult.pokemon.candyEvolutionCost < 0) {
                binding.exResPokeSpam.setText(context.getString(R.string.pokespam_not_available));
                binding.llPokeSpam.setVisibility(View.VISIBLE);
                return;
            }

            PokeSpam pokeSpamCalculator = new PokeSpam(
                    Pokefly.scanData.getPokemonCandyAmount().or(0),
                    scanResult.pokemon.candyEvolutionCost);

            // number for total evolvable
            int totEvol = pokeSpamCalculator.getTotalEvolvable();
            // number for rows of evolvables
            int evolRow = pokeSpamCalculator.getEvolveRows();
            // number for evolvables in extra row (not complete row)
            int evolExtra = pokeSpamCalculator.getEvolveExtra();

            String text;

            if (totEvol < PokeSpam.HOW_MANY_POKEMON_WE_HAVE_PER_ROW) {
                text = String.valueOf(totEvol);
            } else if (evolExtra == 0) {
                text = context.getString(R.string.pokespam_formatted_message2, totEvol, evolRow);
            } else {
                text = context.getString(R.string.pokespam_formatted_message, totEvol, evolRow, evolExtra);
            }
            binding.exResPokeSpam.setText(text);
            binding.llPokeSpam.setVisibility(View.VISIBLE);
        } else {
            binding.exResPokeSpam.setText("");
            binding.llPokeSpam.setVisibility(View.GONE);
        }
    }

    /**
     * Creates and initializes the level seekbarr in the evolution and powerup prediction section in the results
     * screen.
     */
    private void createExtendedResultLevelSeekbar() {
        binding.expandedLevelSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    populateAdvancedInformation();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    /**
     * Creates and initializes the evolution spinner in the evolution and powerup prediction section in the results
     * screen.
     */
    private void createExtendedResultEvolutionSpinner() {
        //The evolution picker for seeing estimates of how much cp and cost a pokemon will have at a different evolution
        extendedEvolutionSpinnerAdapter = new PokemonSpinnerAdapter(pokefly, R.layout.spinner_pokemon,
                new ArrayList<>());
        binding.extendedEvolutionSpinner.setAdapter(extendedEvolutionSpinnerAdapter);

        binding.extendedEvolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                populateAdvancedInformation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                populateAdvancedInformation();
            }

        });

    }

    /**
     * Adjusts expandedLevelSeekbar and expandedLevelSeekbar thumbs.
     * expandedLevelSeekbar - Adjustable single thumb seekbar to allow users to check for more Pokemon stats at
     * different Pokemon level
     * expandedLevelSeekbarBackground - Static double thumb seekbar as background to identify area of Pokemon stats
     * above Pokemon level at current trainer level
     */
    private void adjustSeekbarsThumbs() {
        // Set Seekbar max value to max Pokemon level at trainer level 40
        binding.expandedLevelSeekbar.setMax(levelToSeekbarProgress(Data.MAXIMUM_POKEMON_LEVEL));

        // Set Thumb value to current Pokemon level
        binding.expandedLevelSeekbar.setProgress(
                levelToSeekbarProgress(Pokefly.scanResult.levelRange.min));

        // Set Seekbar Background max value to max Pokemon level at trainer level 40
        binding.expandedLevelSeekbarBackground.setMax(levelToSeekbarProgress(Data.MAXIMUM_POKEMON_LEVEL));

        // Set Thumb 1 drawable to an orange marker and value at the max possible Pokemon level at the current
        // trainer level
        binding.expandedLevelSeekbarBackground.getThumb(0).setThumb(
                ContextCompat.getDrawable(pokefly, R.drawable.orange_seekbar_thumb_marker));
        binding.expandedLevelSeekbarBackground.getThumb(0).setValue(
                levelToSeekbarProgress(Data.trainerLevelToMaxPokeLevel(pokefly.getTrainerLevel())));

        // Set Thumb 2 to invisible and value at max Pokemon level at trainer level 40
        binding.expandedLevelSeekbarBackground.getThumb(1).setInvisibleThumb(true);
        binding.expandedLevelSeekbarBackground.getThumb(1).setValue(levelToSeekbarProgress(Data.MAXIMUM_POKEMON_LEVEL));

        // Set empty on touch listener to prevent changing values of Thumb 1
        binding.expandedLevelSeekbarBackground.setOnTouchListener((v, event) -> true);
    }


    /**
     * Calculate the seekbar progress from a pokemon level.
     *
     * @param level a valid pokemon level (hence <= 40).
     * @return a seekbar progress index.
     */
    private int levelToSeekbarProgress(double level) {
        return (int) (2 * level - getSeekbarOffset());
    }

    public void explainCPPercentageComparedToMaxIV() {
        Toast.makeText(pokefly.getApplicationContext(), R.string.perfection_explainer, Toast.LENGTH_LONG).show();
    }

    public void incrementLevelExpanded() {
        binding.expandedLevelSeekbar.setProgress(binding.expandedLevelSeekbar.getProgress() + 1);
        populateAdvancedInformation();
    }

    public void decrementLevelExpanded() {
        binding.expandedLevelSeekbar.setProgress(binding.expandedLevelSeekbar.getProgress() - 1);
        populateAdvancedInformation();
    }

    private int getSeekbarOffset() {
        return (int) (2 * Pokefly.scanResult.levelRange.min);
    }

    private double seekbarProgressToLevel(int progress) {
        return (progress + getSeekbarOffset()) / 2.0;
        //seekbar only supports integers, so the seekbar works between 2 and 80.
    }

    @Override
    public void updateGuiColors() {
        int c = GUIColorFromPokeType.getInstance().getColor();
        binding.ivButton.setBackgroundColor(c);
        binding.movesetButton.setBackgroundColor(c);
        binding.powerupHeader.setBackgroundColor(c);
    }
}
