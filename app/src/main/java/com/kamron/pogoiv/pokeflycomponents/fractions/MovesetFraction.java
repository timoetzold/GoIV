package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.Toast;

import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.FractionMovesetBinding;
import com.kamron.pogoiv.databinding.TableRowMovesetBinding;
import com.kamron.pogoiv.pokeflycomponents.MovesetsManager;
import com.kamron.pogoiv.scanlogic.MovesetData;
import com.kamron.pogoiv.scanlogic.PokemonShareHandler;
import com.kamron.pogoiv.utils.ExportPokemonQueue;
import com.kamron.pogoiv.utils.GUIColorFromPokeType;
import com.kamron.pogoiv.utils.ReactiveColorListener;
import com.kamron.pogoiv.utils.fractions.MovableFraction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static com.kamron.pogoiv.GoIVSettings.MOVESET_WINDOW_POSITION;


public class MovesetFraction extends MovableFraction implements ReactiveColorListener {

    private static final String URL_POKEBATTLER_IMPORT = "https://www.pokebattler.com/pokebox/import";


    private final Pokefly pokefly;
    private ArrayList<MovesetData> movesets;
    private final Comparator<MovesetData> atkComparator = new MovesetData.AtkComparator();
    private final Comparator<MovesetData> reverseAtkComparator = Collections.reverseOrder(new MovesetData.AtkComparator());
    private final Comparator<MovesetData> defComparator = new MovesetData.DefComparator();
    private final Comparator<MovesetData> reverseDefComparator = Collections.reverseOrder(new MovesetData.DefComparator());
    private Comparator<MovesetData> currentComparator;
    private final DecimalFormat scoreFormat = new DecimalFormat("###%");

    private FractionMovesetBinding binding;

    public MovesetFraction(@NonNull Pokefly pokefly, @NonNull SharedPreferences sharedPrefs) {
        super(sharedPrefs);
        this.pokefly = pokefly;
    }

    @Override
    protected @Nullable String getVerticalOffsetSharedPreferencesKey() {
        return MOVESET_WINDOW_POSITION;
    }

    @Override
    public void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        binding = FractionMovesetBinding.inflate(inflater, parent, attachToParent);

        // Load moveset data
        Collection<MovesetData> m = MovesetsManager.getMovesetsForDexNumber(Pokefly.scanResult.pokemon.number);
        if (m != null) {
            movesets = new ArrayList<>(m);
        } else {
            movesets = new ArrayList<>();
        }

        if (!movesets.isEmpty()) {
            // Initialize descent attack order by default; this will cause the table to rebuild.
            sortBy(atkComparator);
        }
        updateGuiColors();
        GUIColorFromPokeType.getInstance().setListenTo(this);

        binding.positionHandler.setOnTouchListener(this::positionHandlerTouchEvent);
        binding.movesetHeader.setOnTouchListener(this::positionHandlerTouchEvent);

        binding.powerUpButton.setOnClickListener(view -> onPowerUp());
        binding.ivButton.setOnClickListener(view -> onMoveset());
        binding.btnBack.setOnClickListener(view -> onBack());
        binding.btnClose.setOnClickListener(view -> onClose());
        binding.headerAttack.setOnClickListener(view -> sortAttack());
        binding.headerDefense.setOnClickListener(view -> sortDefense());

        binding.exportWebButton.setOnClickListener(view -> export());
        binding.clipboardClear.setOnClickListener(view -> clearClip());
        binding.exportWebButtonQueue.setOnClickListener(view -> addToQueue());
        binding.shareWithOtherApp.setOnClickListener(view -> shareScannedPokemonInformation());
    }



    @Override
    public void onDestroy() {
        GUIColorFromPokeType.getInstance().removeListener(this);
        binding = null;
    }

    @Override
    public Anchor getAnchor() {
        return Anchor.BOTTOM;
    }

    @Override
    public int getDefaultVerticalOffset(DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, displayMetrics);
    }

    boolean positionHandlerTouchEvent(View v, MotionEvent event) {
        return super.onTouch(v, event);
    }

    private void sortBy(Comparator<MovesetData> comparator) {
        currentComparator = comparator;
        movesets.sort(currentComparator);

        // Rebuild table
        buildTable();

        // Update header sort icons
        Drawable none = ContextCompat.getDrawable(pokefly, R.drawable.ic_sort_none);
        if (atkComparator.equals(currentComparator)) {
            Drawable desc = ContextCompat.getDrawable(pokefly, R.drawable.ic_sort_desc);
            binding.headerIconAttack.setImageDrawable(desc);
            binding.headerIconDefense.setImageDrawable(none);
        } else if (reverseAtkComparator.equals(currentComparator)) {
            Drawable asc = ContextCompat.getDrawable(pokefly, R.drawable.ic_sort_asc);
            binding.headerIconAttack.setImageDrawable(asc);
            binding.headerIconDefense.setImageDrawable(none);
        } else if (defComparator.equals(currentComparator)) {
            Drawable desc = ContextCompat.getDrawable(pokefly, R.drawable.ic_sort_desc);
            binding.headerIconAttack.setImageDrawable(none);
            binding.headerIconDefense.setImageDrawable(desc);
        } else if (reverseDefComparator.equals(currentComparator)) {
            Drawable asc = ContextCompat.getDrawable(pokefly, R.drawable.ic_sort_asc);
            binding.headerIconAttack.setImageDrawable(none);
            binding.headerIconDefense.setImageDrawable(asc);
        }
    }

    private void buildTable() {
        for (int i = 0; i < movesets.size(); i++) {
            MovesetData moveset = movesets.get(i);
            buildRow(moveset, (TableRow) binding.tableLayout.getChildAt(i + 1));
        }
    }

    private void buildRow(MovesetData move, TableRow recycle) {
        TableRow row;
        RowViewHolder holder;
        if (recycle != null) {
            row = recycle;
            holder = (RowViewHolder) row.getTag();
        } else {
            TableRowMovesetBinding rowBinding = TableRowMovesetBinding.inflate(LayoutInflater.from(pokefly),
                    binding.tableLayout, false);
            row = (TableRow) rowBinding.getRoot();
            holder = new RowViewHolder(rowBinding);

            row.setTag(holder);
        }

        holder.setMovesetData(move);

        if (row.getParent() == null) {
            binding.tableLayout.addView(row);
        }
    }

    private int getIsSelectedColor(boolean scanned) {
        if (scanned) {
            return Color.parseColor("#edfcef");
        } else {
            return Color.parseColor("#ffffff");
        }
    }

    private int getMoveColor(boolean legacy) {
        if (legacy) {
            return Color.parseColor("#a3a3a3");
        } else {
            return Color.parseColor("#282828");
        }
    }

    private int getPowerColor(double atkScore) {
        if (atkScore > 0.95) {
            return Color.parseColor("#4c8fdb");
        }
        if (atkScore > 0.85) {
            return Color.parseColor("#8eed94");
        }
        if (atkScore > 0.7) {
            return Color.parseColor("#f9a825");
        }
        return Color.parseColor("#d84315");
    }

    void onPowerUp() {
        pokefly.navigateToPowerUpFraction();
    }

    void onMoveset() {
        pokefly.navigateToIVResultFraction();
    }

    void onBack() {
        pokefly.navigateToPreferredStartFraction();
    }

    void onClose() {
        pokefly.closeInfoDialog();
    }

    void sortAttack() {
        if (atkComparator.equals(currentComparator)) {
            sortBy(reverseAtkComparator);
        } else {
            sortBy(atkComparator);
        }
    }

    void sortDefense() {
        if (defComparator.equals(currentComparator)) {
            sortBy(reverseDefComparator);
        } else {
            sortBy(defComparator);
        }
    }

    void export() {
        String exportString = ExportPokemonQueue.getExportString();
        ClipboardManager clipboard = (ClipboardManager) pokefly.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(exportString, exportString));

        Toast toast = Toast.makeText(pokefly, R.string.export_queue_copied, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(URL_POKEBATTLER_IMPORT));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pokefly.startActivity(i);
        pokefly.closeInfoDialog();
    }

    void clearClip() {
        ExportPokemonQueue.clear();

        Toast toast = Toast.makeText(pokefly, R.string.export_queue_cleared, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    void addToQueue() {
        ExportPokemonQueue.add(Pokefly.scanResult);

        String text = pokefly.getString(R.string.export_queue_added,
                Pokefly.scanResult.pokemon, ExportPokemonQueue.size());
        Toast toast = Toast.makeText(pokefly, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * Creates an intent to share the result of the pokemon scan, and closes the overlay.
     */
    void shareScannedPokemonInformation() {
        PokemonShareHandler communicator = new PokemonShareHandler();
        communicator.spreadResultIntent(pokefly);
        pokefly.closeInfoDialog();
    }

    @Override public void updateGuiColors() {
        int c = GUIColorFromPokeType.getInstance().getColor();
        binding.powerUpButton.setBackgroundColor(c);
        binding.ivButton.setBackgroundColor(c);
        binding.topNavigation.setBackgroundColor(c);
    }

    public class RowViewHolder {

        private final TableRowMovesetBinding binding;

        private MovesetData data;

        private RowViewHolder(@NonNull TableRowMovesetBinding binding) {
            this.binding = binding;
        }

        public void setMovesetData(MovesetData data) {
            this.data = data;

            // Fast move
            binding.textFast.setTextColor(getMoveColor(data.isFastIsLegacy()));
            binding.textFast.setText(data.getFast());
            if (data.equals(Pokefly.scanResult.selectedMoveset)) {
                binding.textFast.setTypeface(null, Typeface.BOLD);
            } else {
                binding.textFast.setTypeface(null, Typeface.NORMAL);
            }

            // Charge move
            binding.textCharge.setTextColor(getMoveColor(data.isChargeIsLegacy()));
            binding.textCharge.setText(data.getCharge());
            if (data.equals(Pokefly.scanResult.selectedMoveset)) {
                binding.textCharge.setTypeface(null, Typeface.BOLD);
            } else {
                binding.textCharge.setTypeface(null, Typeface.NORMAL);
            }

            // Attack score
            if (data.getAtkScore() != null) {
                binding.textAttack.setTextColor(getPowerColor(data.getAtkScore()));
                binding.textAttack.setText(scoreFormat.format(data.getAtkScore()));
            } else {
                binding.textAttack.setTextColor(Color.parseColor("#d84315"));
                binding.textAttack.setText("<");
            }


            // Defense score

            if (data.getDefScore() != null) {
                binding.textDefense.setTextColor(getPowerColor(data.getDefScore()));
                binding.textDefense.setText(scoreFormat.format(data.getDefScore()));
            } else {
                binding.textDefense.setTextColor(Color.parseColor("#d84315"));
                binding.textDefense.setText("<");
            }

            binding.textFast.setOnClickListener(view -> onRowClick());
            binding.textCharge.setOnClickListener(view -> onRowClick());
            binding.textAttack.setOnClickListener(view -> onRowClick());
            binding.textDefense.setOnClickListener(view -> onRowClick());
        }

        void onRowClick() {
            Pokefly.scanResult.selectedMoveset = data;
            buildTable();

            // Regenerate clipboard
            pokefly.addSpecificMovesetClipboard(data);
        }
    }
}
