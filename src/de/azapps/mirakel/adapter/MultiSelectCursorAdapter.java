/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;

import java.util.ArrayList;

import de.azapps.mirakel.model.IGenericElementInterface;

/**
 * An Extension for the CursorAdapter to support selecting multiple items at once
 *
 * How to use:
 * 1. Extend this class.
 * 2. Extend the ViewHolder
 */
public abstract class
    MultiSelectCursorAdapter<VH extends MultiSelectCursorAdapter.MultiSelectViewHolder, T extends IGenericElementInterface>
    extends CursorAdapter<VH> {
    protected boolean selectMode = false;
    @NonNull
    protected final SparseBooleanArray selectedItems = new SparseBooleanArray();
    @NonNull
    private final MultiSelectCallbacks<T> multiSelectCallbacks;
    @Nullable
    private final OnItemClickedListener<T> itemClickListener;

    /**
     *
     * @param context @see Cursor
     * @param cursor @see Cursor
     * @param itemClickListener What should happen when the user click on an item in "normal" mode
     * @param multiSelectCallbacks Callbacks for the select
     */
    public MultiSelectCursorAdapter(@NonNull final Context context, @NonNull final Cursor cursor,
                                    @Nullable final OnItemClickedListener<T> itemClickListener,
                                    @NonNull final MultiSelectCallbacks<T> multiSelectCallbacks) {
        super(context, cursor);
        this.multiSelectCallbacks = multiSelectCallbacks;
        this.itemClickListener = itemClickListener;
    }

    /**
     * Translates a cursor to an item in the list
     * @param cursor
     * @return
     */
    @NonNull
    public abstract T fromCursor(@NonNull final Cursor cursor);

    /**
     * Clear the whole selection
     */
    public void clearSelections() {
        setSelectMode(false);
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    @NonNull
    public ArrayList<T> getSelectedItems() {
        final ArrayList<T> items =
            new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            final int pos = selectedItems.keyAt(i);
            items.add(getItemAt(pos));
        }
        return items;
    }

    public class MultiSelectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {

        /**
         * Don't forget to call super()!
         * @param view
         */
        public MultiSelectViewHolder(final View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(@NonNull final View v) {
            if (selectMode) {
                toggleSelection(getPosition());
            } else if (itemClickListener != null) {
                itemClickListener.onItemSelected(getItemAt(getPosition()));
            }
        }

        @Override
        public boolean onLongClick(@NonNull final View v) {
            if (!selectMode) {
                setSelectMode(true);
                toggleSelection(getPosition());
            }
            return true;
        }
    }

    public interface MultiSelectCallbacks<T> {
        public void onSelectModeChanged(boolean selectMode);

        /**
         * Is it possible to add this item to the selection?
         * @param item
         * @return
         */
        public boolean canAddItem(@NonNull final T item);

        public void onAddSelectedItem(@NonNull final T item);

        public void onRemoveSelectedItem(@NonNull final T item);
    }

    private void setSelectMode(final boolean selectMode) {
        this.selectMode = selectMode;
        multiSelectCallbacks.onSelectModeChanged(selectMode);
        notifyDataSetChanged();
    }

    private void toggleSelection(final int pos) {
        final T item = getItemAt(pos);

        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            multiSelectCallbacks.onRemoveSelectedItem(item);
        } else {
            // Check if it is allowed to select the item
            if (!multiSelectCallbacks.canAddItem(item)) {
                if (getSelectedItemCount() == 0) {
                    setSelectMode(false);
                }
                return;
            }
            selectedItems.put(pos, true);
            multiSelectCallbacks.onAddSelectedItem(item);
        }
        notifyItemChanged(pos);
    }

    @NonNull
    private T getItemAt(final int position) {
        final Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        return fromCursor(cursor);
    }
}
