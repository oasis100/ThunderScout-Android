/*
 * MIT License
 *
 * Copyright (c) 2016 - 2017 Luke Myers (FRC Team 980 ThunderBots)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.team980.thunderscout.analytics.rankings;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.team980.thunderscout.R;
import com.team980.thunderscout.analytics.rankings.breakdown.AverageScoutData;
import com.team980.thunderscout.analytics.rankings.breakdown.TeamInfoActivity;
import com.team980.thunderscout.backend.AccountScope;
import com.team980.thunderscout.backend.StorageWrapper;
import com.team980.thunderscout.schema.ScoutData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RankingsAdapter extends RecyclerView.Adapter<RankingsAdapter.TeamViewHolder> implements StorageWrapper.StorageListener {

    private LayoutInflater mInflator;

    private RankingsFragment fragment;

    private List<TeamWrapper> teamList; //The list that represents all the loaded data

    private TeamComparator sortMode = TeamComparator.SORT_POINT_CONTRIBUTION;
    private String teamFilter = "";

    private NumberFormat formatter;

    public RankingsAdapter(RankingsFragment fragment) {
        mInflator = LayoutInflater.from(fragment.getContext());

        this.fragment = fragment;

        teamList = new ArrayList<>();

        formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
    }

    @Override
    public RankingsAdapter.TeamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View teamView = mInflator.inflate(R.layout.team_view, parent, false);
        return new TeamViewHolder(teamView);
    }

    @Override
    public void onBindViewHolder(RankingsAdapter.TeamViewHolder holder, int position) {
        TeamWrapper team = teamList.get(position);
        holder.bind(team);
    }

    @Override
    public int getItemCount() {
        return teamList.size();
    }

    public TeamComparator getCurrentSortMode() {
        return sortMode;
    }

    public void sort(TeamComparator mode) {
        sortMode = mode;

        Collections.sort(teamList, TeamComparator.getComparator(sortMode));

        notifyDataSetChanged();
    }

    public void filterByTeam(String query) {
        teamFilter = query;

        AccountScope.getStorageWrapper(AccountScope.LOCAL, fragment.getContext()).queryData(this);
        notifyDataSetChanged();
    }

    @Override
    public void onDataQuery(List<ScoutData> dataList) {
        int listSize = teamList.size();
        teamList.clear();
        //notifyItemRangeRemoved(0, listSize);

        data:
        for (ScoutData data : dataList) {
            for (int i = 0; i < teamList.size(); i++) { //I wish there was an easier way, but there isn't
                TeamWrapper wrapper = teamList.get(i);

                if (wrapper.getTeam().equals(data.getTeam())) {
                    //Pre-existing team

                    wrapper.getDataList().add(data);
                    //notifyItemChanged(i);
                    continue data; //continues the loop labeled 'DATA'
                }
            }

            if (data.getTeam().toLowerCase().startsWith(teamFilter.toLowerCase())) { //Inline filtering in query - TODO add a setting to use contains?
                //New team
                TeamWrapper wrapper = new TeamWrapper(data.getTeam());
                wrapper.getDataList().add(data);
                teamList.add(wrapper);
                //notifyItemInserted(teamList.size() - 1);
            }
        }

        Collections.sort(teamList, TeamComparator.getComparator(sortMode));
        notifyDataSetChanged();

        fragment.getSwipeRefreshLayout().setRefreshing(false);
    }

    @Override
    public void onDataWrite(@Nullable List<ScoutData> dataWritten) {
        //do nothing for now
    }

    @Override
    public void onDataRemove(@Nullable List<ScoutData> dataRemoved) {
        //do nothing for now
    }

    @Override
    public void onDataClear(boolean success) {
        int listSize = teamList.size();
        teamList.clear();
        notifyItemRangeRemoved(0, listSize);
    }

    public class TeamViewHolder extends RecyclerView.ViewHolder {

        private View itemView;

        private TextView teamNumber;
        private TextView descriptor;
        //private TextView rank;

        public TeamViewHolder(View itemView) {
            super(itemView);

            this.itemView = itemView;

            teamNumber = itemView.findViewById(R.id.team_number);
            descriptor = itemView.findViewById(R.id.team_descriptor);
            //rank = itemView.findViewById(R.id.team_rank);
        }

        public void bind(final TeamWrapper wrapper) {
            teamNumber.setText(wrapper.getTeam());
            descriptor.setText(wrapper.getDescriptor(sortMode));

            //rank.setText(formatter.format(wrapper.getExpectedPointContribution()) + " points");

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launchInfoActivity = new Intent(fragment.getContext(), TeamInfoActivity.class);
                    launchInfoActivity.putExtra("com.team980.thunderscout.INFO_AVERAGE_SCOUT", new AverageScoutData(wrapper.getDataList()));

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        ActivityOptions options = ActivityOptions
                                .makeSceneTransitionAnimation(fragment.getActivity(), itemView, "team");
                        itemView.setTransitionName("team");
                        fragment.getContext().startActivity(launchInfoActivity, options.toBundle());
                    } else {
                        fragment.getContext().startActivity(launchInfoActivity);
                    }
                }
            });
        }
    }
}