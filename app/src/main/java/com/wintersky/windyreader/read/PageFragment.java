package com.wintersky.windyreader.read;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wintersky.windyreader.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PageFragment extends Fragment {

    private static final String ARG_TIT = "title";
    private static final String ARG_CON = "content";
    private static final String ARG_IDX = "index";

    private String mTitle;
    private String mContent;
    private String mIndex;

    public PageFragment() {
        // Required empty public constructor
    }

    public static PageFragment newInstance(String title, String content, String index) {
        PageFragment fragment = new PageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TIT, title);
        args.putString(ARG_CON, content);
        args.putString(ARG_IDX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TIT);
            mContent = getArguments().getString(ARG_CON);
            mIndex = getArguments().getString(ARG_IDX);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_page, container, false);

        TextView title = view.findViewById(R.id.title);
        TextView content = view.findViewById(R.id.content);
        TextView index = view.findViewById(R.id.index);

        title.setText(mTitle);
        content.setText(mContent);
        index.setText(mIndex);

        return view;
    }
}
