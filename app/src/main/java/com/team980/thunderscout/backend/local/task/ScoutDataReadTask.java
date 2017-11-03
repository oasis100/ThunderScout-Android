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

package com.team980.thunderscout.backend.local.task;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.team980.thunderscout.ThunderScout;
import com.team980.thunderscout.backend.StorageWrapper;
import com.team980.thunderscout.backend.local.ScoutDataContract;
import com.team980.thunderscout.backend.local.ScoutDataDbHelper;
import com.team980.thunderscout.schema.ScoutData;
import com.team980.thunderscout.schema.enumeration.AllianceStation;
import com.team980.thunderscout.schema.enumeration.ClimbingStats;
import com.team980.thunderscout.schema.enumeration.FuelDumpAmount;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScoutDataReadTask extends AsyncTask<Void, Void, List<ScoutData>> {

    @Nullable //no point in passing null, but safer code if I allow it
    private StorageWrapper.StorageListener listener;
    private Context context;

    public ScoutDataReadTask(@Nullable StorageWrapper.StorageListener listener, Context context) {
        this.listener = listener;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    public List<ScoutData> doInBackground(Void... params) {

        SQLiteDatabase db = new ScoutDataDbHelper(context).getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ScoutDataContract.ScoutDataTable._ID,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TEAM_NUMBER,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_MATCH_NUMBER,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_ALLIANCE_STATION,

                ScoutDataContract.ScoutDataTable.COLUMN_NAME_DATE_ADDED,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_DATA_SOURCE,

                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_GEARS_DELIVERED,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_GEARS_DROPPED,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_LOW_GOAL_DUMP_AMOUNT,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_HIGH_GOALS,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_MISSED_HIGH_GOALS,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_CROSSED_BASELINE,

                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_GEARS_DELIVERED,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_GEARS_DROPPED,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_LOW_GOAL_DUMPS,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_HIGH_GOALS,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_MISSED_HIGH_GOALS,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_CLIMBING_STATS,

                ScoutDataContract.ScoutDataTable.COLUMN_NAME_TROUBLE_WITH,
                ScoutDataContract.ScoutDataTable.COLUMN_NAME_COMMENTS
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                ScoutDataContract.ScoutDataTable._ID + " DESC";

        Cursor cursor;

        try {
            cursor = db.query(
                    ScoutDataContract.ScoutDataTable.TABLE_NAME,  // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        } catch (SQLiteException e) {
            Crashlytics.logException(e);
            return null;
        }

        List<ScoutData> dataList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            dataList.add(initScoutData(cursor));
        }

        while (cursor.moveToNext()) {
            dataList.add(initScoutData(cursor));
        }

        cursor.close();
        db.close();

        return dataList;
    }

    private ScoutData initScoutData(Cursor cursor) {
        ScoutData data = new ScoutData(cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable._ID)));

        // Init
        String teamNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TEAM_NUMBER));

        data.setTeam(teamNumber);

        int matchNumber = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_MATCH_NUMBER));

        data.setMatchNumber(matchNumber);

        String allianceStation = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_ALLIANCE_STATION));

        data.setAllianceStation(AllianceStation.valueOf(allianceStation));

        long dateAdded = cursor.getLong(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_DATE_ADDED));

        data.setDate(new Date(dateAdded));

        String dataSource = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_DATA_SOURCE));

        data.setSource(dataSource);

        // Auto
        int autoGearsDelivered = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_GEARS_DELIVERED));

        data.getAutonomous().setGearsDelivered(autoGearsDelivered);

        int autoGearsDropped = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_GEARS_DROPPED));

        data.getAutonomous().setGearsDropped(autoGearsDropped);

        String autoLowGoalDumpAmount = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_LOW_GOAL_DUMP_AMOUNT));

        data.getAutonomous().setLowGoalDumpAmount(FuelDumpAmount.valueOf(autoLowGoalDumpAmount));

        int autoHighGoals = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_HIGH_GOALS));

        data.getAutonomous().setHighGoals(autoHighGoals);

        int autoMissedHighGoals = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_MISSED_HIGH_GOALS));

        data.getAutonomous().setMissedHighGoals(autoMissedHighGoals);

        int crossedBaseline = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_AUTO_CROSSED_BASELINE));

        data.getAutonomous().setCrossedBaseline(crossedBaseline != 0); //I2B conversion

        // Teleop
        int teleopGearsDelivered = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_GEARS_DELIVERED));

        data.getTeleop().setGearsDelivered(teleopGearsDelivered);

        int teleopGearsDropped = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_GEARS_DROPPED));

        data.getTeleop().setGearsDropped(teleopGearsDropped);

        byte[] teleopLowGoalDumps = cursor.getBlob(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_LOW_GOAL_DUMPS));

        data.getTeleop().getLowGoalDumps().addAll((ArrayList<FuelDumpAmount>) ThunderScout.deserializeObject(teleopLowGoalDumps));

        int teleopHighGoals = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_HIGH_GOALS));

        data.getTeleop().setHighGoals(teleopHighGoals);

        int teleopMissedHighGoals = cursor.getInt(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TELEOP_MISSED_HIGH_GOALS));

        data.getTeleop().setMissedHighGoals(teleopMissedHighGoals);

        String climbingStats = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_CLIMBING_STATS));

        data.getTeleop().setClimbingStats(ClimbingStats.valueOf(climbingStats));

        // Summary
        String troubleWith = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_TROUBLE_WITH));

        data.setTroubleWith(troubleWith);

        String comments = cursor.getString(
                cursor.getColumnIndexOrThrow(ScoutDataContract.ScoutDataTable.COLUMN_NAME_COMMENTS));

        data.setComments(comments);

        return data;
    }

    @Override
    protected void onProgressUpdate(Void[] values) {
        //Runs on UI thread when publishProgress() is called

        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<ScoutData> dataList) {
        //Runs on UI thread after execution

        if (listener != null) {
            listener.onDataQuery(dataList); //return the data to the listener
        }

        super.onPostExecute(dataList);
    }

}