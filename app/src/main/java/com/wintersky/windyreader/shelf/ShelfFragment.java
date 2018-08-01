package com.wintersky.windyreader.shelf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.wintersky.windyreader.R;
import com.wintersky.windyreader.data.Book;
import com.wintersky.windyreader.read.ReadActivity;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

import static com.wintersky.windyreader.shelf.ShelfActivity.BOOK_URL;

/**
 * A simple {@link Fragment} subclass.
 */
public class ShelfFragment extends DaggerFragment implements ShelfContract.View {

    @Inject
    ShelfContract.Presenter mPresenter;

    private EditText mLink;
    private ImageButton mAdd;
    private GridView mGridView;

    @Inject
    public ShelfFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.takeView(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.dropView();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_shelf, container, false);

        mLink = view.findViewById(R.id.link);
        mAdd = view.findViewById(R.id.add);
        mGridView = view.findViewById(R.id.book_grid);

        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mLink.toString();
                mPresenter.getBook(url);
            }
        });

        return view;
    }

    @Override
    public void setShelf(RealmResults<Book> list) {
        GridAdapter adapter = new GridAdapter(list);
        //adapter.bind(getActivity());
        mGridView.setAdapter(adapter);
    }

    @Override
    public void getBookFinish() {
        mLink.setError("Error");
        mAdd.setEnabled(false);
    }

    @Override
    public void getBookFinish(Book book) {
        mLink.getText().clear();
    }

    class GridAdapter extends RealmBaseAdapter<Book> {

        GridAdapter(@Nullable OrderedRealmCollection<Book> data) {
            super(data);
        }

        @Override
        public int getCount() {
            if (adapterData == null) {
                return 0;
            } else if (adapterData.size() % 3 == 0) {
                return adapterData.size();
            } else {
                return adapterData.size() / 3 * 3 + 3;
            }
        }

        @Nullable
        @Override
        public Book getItem(int position) {
            if (adapterData == null || position >= adapterData.size()) {
                return null;
            }
            return adapterData.get(position);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View getView(int i, View view, final ViewGroup viewGroup) {
            ViewHolder holder;
            final Book bk = getItem(i);

            if (view == null) {
                holder = new ViewHolder();
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_shelf, viewGroup, false);
                holder.img = view.findViewById(R.id.cover);
                holder.tv = view.findViewById(R.id.title);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            if (adapterData != null && i < adapterData.size() && bk != null) {
                holder.img.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            Drawable drawable = ((ImageView) v).getDrawable();
                            int color = ContextCompat.getColor(viewGroup.getContext(), R.color.shelfCoverPress);
                            drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                            Drawable drawable = ((ImageView) v).getDrawable();
                            drawable.clearColorFilter();
                        }
                        return false;
                    }
                });
                holder.img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(viewGroup.getContext(), ReadActivity.class);
                        intent.putExtra(BOOK_URL, bk.getUrl());
                        viewGroup.getContext().startActivity(intent);
                    }
                });
                holder.tv.setText(bk.getTitle());
            } else {
                holder.img.setImageDrawable(null);
            }

            return view;
        }

        class ViewHolder {
            ImageView img;
            TextView tv;
        }
    }
}
