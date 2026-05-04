package id.icapps.savera.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import id.icapps.savera.R;
import id.icapps.savera.model.LeaderboardItem;

public class LeaderboardAdapter extends ArrayAdapter<LeaderboardItem> {

    private final Context context;
    private List<LeaderboardItem> leaderboardItems;

    public LeaderboardAdapter(Context context, List<LeaderboardItem> items) {
        super(context, 0, items);

        this.context = context;
        this.leaderboardItems = items;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.my_leaderboard_item, parent, false);
        }

        LeaderboardItem item = leaderboardItems.get(position);

        // Set data to views
        TextView textRank = view.findViewById(R.id.textRank);
        ImageView imgProfile = view.findViewById(R.id.imgProfile);
        TextView textName = view.findViewById(R.id.textName);
        TextView textPosition = view.findViewById(R.id.textPosition);
        TextView textAverage = view.findViewById(R.id.textAverage);
        TextView textDays = view.findViewById(R.id.textDays);

        // Bind data to the views
        textRank.setText(String.format("%d", item.getRank()));
        imgProfile.setImageResource(item.getPhoto());
        textName.setText(item.getName());
        textPosition.setText(item.getPosition());
        textAverage.setText(item.getAverageSleep());
        textDays.setText(item.getDays());

        return view;
    }
}
